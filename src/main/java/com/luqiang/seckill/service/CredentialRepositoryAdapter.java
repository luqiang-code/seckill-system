package com.luqiang.seckill.service;

import com.luqiang.seckill.entity.Credential;
import com.luqiang.seckill.repository.CredentialRepository;
import com.luqiang.seckill.repository.UserRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;

import com.yubico.webauthn.data.exception.Base64UrlException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bridges Yubico's CredentialRepository interface to Spring Data JPA repositories.
 */
public class CredentialRepositoryAdapter implements com.yubico.webauthn.CredentialRepository {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    public CredentialRepositoryAdapter(UserRepository userRepository, CredentialRepository credentialRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> credentialRepository.findByUserId(user.getId()))
                .map(creds -> creds.stream()
                        .map(c -> {
                            try {
                                return PublicKeyCredentialDescriptor.builder()
                                        .id(ByteArray.fromBase64Url(c.getCredentialId()))
                                        .transports(parseTransports(c.getTransports()))
                                        .build();
                            } catch (Base64UrlException e) {
                                throw new RuntimeException("Invalid credential ID: " + c.getCredentialId(), e);
                            }
                        })
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return Optional.of(new ByteArray(username.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return Optional.of(new String(userHandle.getBytes(), StandardCharsets.UTF_8));
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return credentialRepository.findByCredentialId(credentialId.getBase64Url())
                .map(c -> toRegisteredCredential(c));
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return credentialRepository.findByCredentialId(credentialId.getBase64Url())
                .map(c -> Set.of(toRegisteredCredential(c)))
                .orElse(Set.of());
    }

    private RegisteredCredential toRegisteredCredential(Credential c) {
        try {
            String username = userRepository.findById(c.getUserId())
                    .map(u -> u.getUsername())
                    .orElse(c.getUserId().toString());
            return RegisteredCredential.builder()
                    .credentialId(ByteArray.fromBase64Url(c.getCredentialId()))
                    .userHandle(new ByteArray(username.getBytes(StandardCharsets.UTF_8)))
                    .publicKeyCose(new ByteArray(c.getPublicKey()))
                    .signatureCount(c.getCounter())
                    .build();
        } catch (Base64UrlException e) {
            throw new RuntimeException("Invalid credential ID: " + c.getCredentialId(), e);
        }
    }

    private Set<com.yubico.webauthn.data.AuthenticatorTransport> parseTransports(String transports) {
        if (transports == null || transports.isEmpty()) return Collections.emptySet();
        return Arrays.stream(transports.split(","))
                .map(String::trim)
                .map(t -> {
                    try {
                        return com.yubico.webauthn.data.AuthenticatorTransport.of(t);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
