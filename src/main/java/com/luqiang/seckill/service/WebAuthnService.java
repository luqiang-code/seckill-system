package com.luqiang.seckill.service;

import com.luqiang.seckill.common.JwtUtil;
import com.luqiang.seckill.entity.Credential;
import com.luqiang.seckill.entity.User;
import com.luqiang.seckill.repository.CredentialRepository;
import com.luqiang.seckill.repository.UserRepository;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebAuthnService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private RelyingParty relyingParty;

    @Value("${webauthn.rp-id:localhost}")
    private String rpId;

    @Value("${webauthn.rp-name:秒杀系统}")
    private String rpName;

    @Value("${webauthn.origin:http://localhost:5173}")
    private String origin;

    private final ConcurrentHashMap<String, ChallengeEntry<?>> challengeMap = new ConcurrentHashMap<>();

    public WebAuthnService(UserRepository userRepository, CredentialRepository credentialRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
    }

    @PostConstruct
    void init() {
        this.relyingParty = RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder()
                        .id(rpId)
                        .name(rpName)
                        .build())
                .credentialRepository(new CredentialRepositoryAdapter(userRepository, credentialRepository))
                .origins(Set.of(origin))
                .build();
    }

    // ==================== Registration ====================

    public PublicKeyCredentialCreationOptions startRegistration(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            List<Credential> creds = credentialRepository.findByUserId(user.getId());
            if (!creds.isEmpty()) {
                throw new UsernameAlreadyExistsException("用户名已被注册");
            }
        });

        UserIdentity userIdentity = UserIdentity.builder()
                .name(username)
                .displayName(username)
                .id(new ByteArray(username.getBytes(StandardCharsets.UTF_8)))
                .build();

        StartRegistrationOptions options = StartRegistrationOptions.builder()
                .user(userIdentity)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .residentKey(ResidentKeyRequirement.REQUIRED)
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .build())
                .build();

        PublicKeyCredentialCreationOptions result = relyingParty.startRegistration(options);
        challengeMap.put(username, new ChallengeEntry<>(result));
        return result;
    }

    public String finishRegistration(String username, String credentialJson) {
        @SuppressWarnings("unchecked")
        ChallengeEntry<PublicKeyCredentialCreationOptions> entry =
                (ChallengeEntry<PublicKeyCredentialCreationOptions>) challengeMap.remove(username);
        if (entry == null || entry.isExpired()) {
            throw new ChallengeExpiredException("注册超时，请重试");
        }

        try {
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
                    PublicKeyCredential.parseRegistrationResponseJson(credentialJson);

            FinishRegistrationOptions options = FinishRegistrationOptions.builder()
                    .request(entry.request)
                    .response(pkc)
                    .build();

            RegistrationResult result = relyingParty.finishRegistration(options);

            User user = userRepository.findByUsername(username)
                    .orElseGet(() -> {
                        User newUser = new User(username);
                        return userRepository.save(newUser);
                    });

            Credential credential = new Credential();
            credential.setUserId(user.getId());
            credential.setCredentialId(result.getKeyId().getId().getBase64Url());
            credential.setPublicKey(result.getPublicKeyCose().getBytes());
            credential.setCounter(result.getSignatureCount());
            credentialRepository.save(credential);

            return JwtUtil.generate(user.getId().toString());
        } catch (UsernameAlreadyExistsException | ChallengeExpiredException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("注册验证失败: " + e.getMessage(), e);
        }
    }

    // ==================== Authentication ====================

    public AssertionRequest startAssertion(String username) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new NoPasskeyException("该用户未注册 Passkey，请先注册"));

        StartAssertionOptions options = StartAssertionOptions.builder()
                .username(username)
                .userVerification(UserVerificationRequirement.REQUIRED)
                .build();

        AssertionRequest result = relyingParty.startAssertion(options);
        challengeMap.put(username, new ChallengeEntry<>(result));
        return result;
    }

    public String finishAssertion(String username, String credentialJson) {
        @SuppressWarnings("unchecked")
        ChallengeEntry<AssertionRequest> entry =
                (ChallengeEntry<AssertionRequest>) challengeMap.remove(username);
        if (entry == null || entry.isExpired()) {
            throw new ChallengeExpiredException("登录超时，请重试");
        }

        try {
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                    PublicKeyCredential.parseAssertionResponseJson(credentialJson);

            FinishAssertionOptions options = FinishAssertionOptions.builder()
                    .request(entry.request)
                    .response(pkc)
                    .build();

            AssertionResult result = relyingParty.finishAssertion(options);

            if (result.isSuccess()) {
                credentialRepository.findByCredentialId(
                        result.getCredential().getCredentialId().getBase64Url()
                ).ifPresent(cred -> {
                    cred.setCounter(result.getSignatureCount());
                    credentialRepository.save(cred);
                });

                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("用户不存在"));
                return JwtUtil.generate(user.getId().toString());
            }
            throw new RuntimeException("登录验证失败");
        } catch (ChallengeExpiredException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("登录验证失败: " + e.getMessage(), e);
        }
    }

    // ==================== Discoverable Authentication ====================

    private static final String KEY_DISCOVER = "__discover__";

    public AssertionRequest startDiscoverAssertion() {
        StartAssertionOptions options = StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.REQUIRED)
                .build();

        AssertionRequest result = relyingParty.startAssertion(options);
        challengeMap.put(KEY_DISCOVER, new ChallengeEntry<>(result));
        return result;
    }

    public String finishDiscoverAssertion(String credentialJson) {
        @SuppressWarnings("unchecked")
        ChallengeEntry<AssertionRequest> entry =
                (ChallengeEntry<AssertionRequest>) challengeMap.remove(KEY_DISCOVER);
        if (entry == null || entry.isExpired()) {
            throw new ChallengeExpiredException("登录超时，请重试");
        }

        try {
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                    PublicKeyCredential.parseAssertionResponseJson(credentialJson);

            FinishAssertionOptions options = FinishAssertionOptions.builder()
                    .request(entry.request)
                    .response(pkc)
                    .build();

            AssertionResult result = relyingParty.finishAssertion(options);

            if (result.isSuccess()) {
                String username = result.getUsername();

                credentialRepository.findByCredentialId(
                        result.getCredential().getCredentialId().getBase64Url()
                ).ifPresent(cred -> {
                    cred.setCounter(result.getSignatureCount());
                    credentialRepository.save(cred);
                });

                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("用户不存在"));
                return JwtUtil.generate(user.getId().toString());
            }
            throw new RuntimeException("登录验证失败");
        } catch (ChallengeExpiredException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("登录验证失败: " + e.getMessage(), e);
        }
    }

    // ==================== Challenge Management ====================

    @Scheduled(fixedRate = 300_000)
    public void pruneChallenges() {
        challengeMap.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private static class ChallengeEntry<T> {
        final T request;
        final long createdAt;

        ChallengeEntry(T request) {
            this.request = request;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > 300_000;
        }
    }

    // ==================== Exception Classes ====================

    public static class UsernameAlreadyExistsException extends RuntimeException {
        public UsernameAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class ChallengeExpiredException extends RuntimeException {
        public ChallengeExpiredException(String message) {
            super(message);
        }
    }

    public static class NoPasskeyException extends RuntimeException {
        public NoPasskeyException(String message) {
            super(message);
        }
    }
}
