package com.almousleck.controller;

import com.almousleck.dto.*;
import com.almousleck.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "User authentication and account management APIS"
)
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(
            summary = "Register new user", description = "Register a new user and send OTP to phone number"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "409", description = "Identifier already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<OtpResponse> register(@Valid @RequestBody RegisterRequest request) {
        OtpResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "User login", description = "Authenticate user with username/phone and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "423", description = "Account locked")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        authenticationService.verifyOtp(request.getPhoneNumber(), request.getOtpCode());
        return ResponseEntity.ok(Map.of("message", "手机号验证成功"));
    }
    @PostMapping("/resend-otp")
    public ResponseEntity<OtpResponse> resendOtp(@Valid @RequestBody OtpRequest request) {
        OtpResponse response = authenticationService.resendOtp(request.getPhoneNumber());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<OtpResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        OtpResponse response = authenticationService.forgotPassword(request.getPhoneNumber());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(
                request.getPhoneNumber(),
                request.getOtpCode(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(Map.of("message", "密码重置成功，请使用新密码登录"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, @RequestBody Map<String, String> body) {
        // Get access token from Header
        String authHeader = request.getHeader("Authorization");
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer "))
            accessToken = authHeader.substring(7);

        // Get refresh token from Body
        String refreshToken = body.get("refreshToken");

        if (accessToken != null && refreshToken != null) {
            authenticationService.logout(accessToken, refreshToken);
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        }

        return ResponseEntity.badRequest()
                .body(Map.of("message", "Tokens required for logout"));
    }

}
