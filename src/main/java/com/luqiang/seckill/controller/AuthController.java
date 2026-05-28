package com.luqiang.seckill.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luqiang.seckill.common.ApiResponse;
import com.luqiang.seckill.common.JwtUtil;
import com.luqiang.seckill.service.WebAuthnService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final WebAuthnService webAuthnService;
    private final ObjectMapper objectMapper;

    public AuthController(WebAuthnService webAuthnService, ObjectMapper objectMapper) {
        this.webAuthnService = webAuthnService;
        this.objectMapper = objectMapper;
    }

    // ==================== Registration ====================

    @PostMapping("/register/options")
    public ApiResponse<?> registerOptions(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return ApiResponse.fail(400, "username required");
        }
        try {
            return ApiResponse.success("ok", webAuthnService.startRegistration(username));
        } catch (WebAuthnService.UsernameAlreadyExistsException e) {
            return ApiResponse.fail(409, e.getMessage());
        }
    }

    @PostMapping("/register/verify")
    public ApiResponse<?> registerVerify(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        if (username == null || username.isBlank()) {
            return ApiResponse.fail(400, "username required");
        }
        try {
            String credentialJson = objectMapper.writeValueAsString(body.get("registrationResponse"));
            String token = webAuthnService.finishRegistration(username, credentialJson);
            return ApiResponse.success("注册成功", Map.of("token", token, "username", username));
        } catch (WebAuthnService.UsernameAlreadyExistsException e) {
            return ApiResponse.fail(409, e.getMessage());
        } catch (WebAuthnService.ChallengeExpiredException e) {
            return ApiResponse.fail(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    // ==================== Authentication ====================

    @PostMapping("/login/options")
    public ApiResponse<?> loginOptions(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return ApiResponse.fail(400, "username required");
        }
        try {
            return ApiResponse.success("ok",
                    webAuthnService.startAssertion(username).getPublicKeyCredentialRequestOptions());
        } catch (WebAuthnService.NoPasskeyException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PostMapping("/login/verify")
    public ApiResponse<?> loginVerify(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        if (username == null || username.isBlank()) {
            return ApiResponse.fail(400, "username required");
        }
        try {
            String credentialJson = objectMapper.writeValueAsString(body.get("authenticationResponse"));
            String token = webAuthnService.finishAssertion(username, credentialJson);
            return ApiResponse.success("登录成功", Map.of("token", token, "username", username));
        } catch (WebAuthnService.ChallengeExpiredException e) {
            return ApiResponse.fail(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    // ==================== Discoverable Authentication ====================

    @PostMapping("/discover")
    public ApiResponse<?> discoverOptions() {
        return ApiResponse.success("ok",
                webAuthnService.startDiscoverAssertion().getPublicKeyCredentialRequestOptions());
    }

    @PostMapping("/discover/verify")
    public ApiResponse<?> discoverVerify(@RequestBody Map<String, Object> body) {
        try {
            String credentialJson = objectMapper.writeValueAsString(body.get("authenticationResponse"));
            String token = webAuthnService.finishDiscoverAssertion(credentialJson);
            // Extract username from token
            String userId = JwtUtil.parseUserId(token);
            return ApiResponse.success("登录成功", Map.of("token", token, "userId", userId != null ? userId : ""));
        } catch (WebAuthnService.ChallengeExpiredException e) {
            return ApiResponse.fail(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    // ==================== Session ====================

    @GetMapping("/me")
    public ApiResponse<?> currentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponse.fail(401, "Not authenticated");
        }
        String userId = JwtUtil.parseUserId(authHeader.substring(7));
        if (userId == null) {
            return ApiResponse.fail(401, "Invalid token");
        }
        return ApiResponse.success("ok", Map.of("userId", userId));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout() {
        return ApiResponse.success("已登出", null);
    }
}
