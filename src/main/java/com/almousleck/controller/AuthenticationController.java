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
@Tag(name = "认证", description = "用户认证与账户管理")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(summary = "用户注册")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "注册成功"),
            @ApiResponse(responseCode = "409", description = "用户名或手机号已被注册"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/register")
    public ResponseEntity<OtpResponse> register(@Valid @RequestBody RegisterRequest request) {
        OtpResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "用户登录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "423", description = "账户已锁定"),
            @ApiResponse(responseCode = "403", description = "手机号未验证")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @Operation(summary = "验证OTP验证码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "验证成功"),
            @ApiResponse(responseCode = "400", description = "验证码错误或已过期"),
            @ApiResponse(responseCode = "404", description = "手机号未注册")
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        authenticationService.verifyOtp(request.getPhoneNumber(), request.getOtpCode());
        return ResponseEntity.ok(Map.of("message", "手机号验证成功"));
    }

    @Operation(summary = "重新发送OTP验证码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "发送成功"),
            @ApiResponse(responseCode = "429", description = "请求过于频繁"),
            @ApiResponse(responseCode = "404", description = "手机号未注册")
    })
    @PostMapping("/resend-otp")
    public ResponseEntity<OtpResponse> resendOtp(@Valid @RequestBody OtpRequest request) {
        OtpResponse response = authenticationService.resendOtp(request.getPhoneNumber());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "忘记密码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "验证码已发送"),
            @ApiResponse(responseCode = "429", description = "请求过于频繁"),
            @ApiResponse(responseCode = "404", description = "手机号未注册")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<OtpResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        OtpResponse response = authenticationService.forgotPassword(request.getPhoneNumber());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "重置密码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "重置成功"),
            @ApiResponse(responseCode = "400", description = "验证码错误或已过期"),
            @ApiResponse(responseCode = "404", description = "手机号未注册")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(
                request.getPhoneNumber(),
                request.getOtpCode(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(Map.of("message", "密码重置成功，请使用新密码登录"));
    }

    @Operation(summary = "刷新访问令牌")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "刷新成功"),
            @ApiResponse(responseCode = "403", description = "刷新令牌无效或已过期")
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "用户登出")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登出成功"),
            @ApiResponse(responseCode = "400", description = "缺少令牌")
    })
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
            return ResponseEntity.ok(Map.of("message", "登出成功"));
        }

        return ResponseEntity.badRequest()
                .body(Map.of("message", "缺少访问令牌或刷新令牌"));
    }

}
