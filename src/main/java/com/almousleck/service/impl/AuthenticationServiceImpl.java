package com.almousleck.service.impl;

import com.almousleck.config.ApplicationUserDetails;
import com.almousleck.dto.*;
import com.almousleck.enums.UserRole;
import com.almousleck.exceptions.*;
import com.almousleck.jwt.JwtUtils;
import com.almousleck.model.RefreshToken;
import com.almousleck.model.User;
import com.almousleck.repository.RefreshTokenRepository;
import com.almousleck.repository.UserRepository;
import com.almousleck.service.AuthenticationService;
import com.almousleck.service.NotificationService;
import com.almousleck.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final ModelMapper modelMapper;
    private final NotificationService notificationService;


    private static final SecureRandom random = new SecureRandom();
    @Value("${app.security.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Override
    public OtpResponse register(RegisterRequest request) {
        // check if the username is taken
        if (userRepository.existsByUsername(request.getUsername()))
            throw new ResourceAlreadyExistsException("用户名已被占用");

        // check if the phone is taken
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber()))
                throw new ResourceAlreadyExistsException("手机号已被占用");

        // map user to model mapper
        User user = modelMapper.map(request, User.class);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        if (user.getRole() == null)
            user.setRole(UserRole.USER);

        // Generate OTP phone verification
        String otpCode = String.format("%06d", random.nextInt(1000000));
        user.setOtpCode(otpCode);
        user.setOtpGeneratedAt(LocalDateTime.now());
        user.setOtpVerified(false);
        user.setPhoneVerified(false);

        userRepository.save(user);

        // Send OTP notification
        notificationService.sendOtp(request.getPhoneNumber(), otpCode, otpExpiryMinutes);

        log.info("✅ User registered: {}", request.getUsername());

        return new OtpResponse("注册成功，验证码已发送", otpExpiryMinutes * 60, otpCode);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication  authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword())
        );

        ApplicationUserDetails userDetails = (ApplicationUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Strategy: Rotation - Clear old token
        refreshTokenRepository.deleteByUser(user);

        // Issuing tokens
        String jwt = jwtUtils.generateTokenForUser(authentication);
        RefreshToken refreshToken = createRefreshToken(user);
        
        return AuthResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    @Override
    public void verifyOtp(String phoneNumber, String otpCode) {
        User user = userRepository.findUserByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("手机号未注册"));
        // Check if OTP exists
        if (user.getOtpCode() == null || user.getOtpGeneratedAt() == null)
            throw new InvalidOtpException("请先获取验证码");

        // Check if OTP expired
        long minutesSinceGeneration = ChronoUnit.MINUTES.between(
                user.getOtpGeneratedAt(), LocalDateTime.now());

        if (minutesSinceGeneration > otpExpiryMinutes)
            throw new OtpExpiredException("验证码已过期，请重新获取");

        // Verify OTP code
        if (!user.getOtpCode().equals(otpCode))
            throw new InvalidOtpException("验证码错误");

        // Mark as verified
        user.setOtpVerified(true);
        user.setPhoneVerified(true);
        user.setOtpCode(null);
        userRepository.save(user);

        // Send a success message
        notificationService.sendPhoneVerifiedNotification(phoneNumber);

        log.info("✅ Phone verified: {}", phoneNumber);
    }

    @Override
    public OtpResponse resendOtp(String phoneNumber) {
        User user = userRepository.findUserByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("手机号未注册"));

        // Rate limiting - prevent spam
        if (user.getOtpGeneratedAt() != null) {
            long minutesSinceLastOtp = ChronoUnit.MINUTES.between(
                    user.getOtpGeneratedAt(), LocalDateTime.now());

            if (minutesSinceLastOtp < 1) {
                int retryAfter = (int) (1 - minutesSinceLastOtp) * 60;
                throw new OtpRateLimitException("请求过于频繁", retryAfter);
            }
        }

        // Generate new OTP
        String otpCode = String.format("%06d", random.nextInt(1000000));
        user.setOtpCode(otpCode);
        user.setOtpGeneratedAt(LocalDateTime.now());
        userRepository.save(user);

        // Send OTP
        notificationService.sendOtp(phoneNumber, otpCode, otpExpiryMinutes);

        log.info("🔄 OTP resent to: {}", phoneNumber);

        return new OtpResponse("验证码已重新发送", otpExpiryMinutes * 60, otpCode);
    }

    @Override
    @Transactional
    public OtpResponse forgotPassword(String phoneNumber) {
        User user = userRepository.findUserByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("手机号未注册"));

        // Check rate limiting
        if (user.getOtpGeneratedAt() != null) {
            long minutesSinceLastOtp = ChronoUnit.MINUTES.between(
                    user.getOtpGeneratedAt(),
                    LocalDateTime.now());

            if (minutesSinceLastOtp < 1) {
                int retryAfter = (int) (1 - minutesSinceLastOtp) * 60;
                throw new OtpRateLimitException("请求过于频繁", retryAfter);
            }
        }

        // Generate OTP for password reset
        String otpCode = String.format("%06d", random.nextInt(1000000));
        user.setOtpCode(otpCode);
        user.setOtpGeneratedAt(LocalDateTime.now());
        userRepository.save(user);

        // Send OTP
        notificationService.sendPasswordResetOtp(phoneNumber, otpCode, otpExpiryMinutes);

        log.info("🔑 Password reset OTP sent to: {}", phoneNumber);

        return new OtpResponse("密码重置验证码已发送", otpExpiryMinutes * 60, otpCode);
    }

    @Override
    @Transactional
    public void resetPassword(String phoneNumber, String otpCode, String newPassword) {
        User user = userRepository.findUserByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("手机号未注册"));

        // check if OTP exist
        if(user.getOtpCode() == null || user.getOtpGeneratedAt() == null)
            throw new InvalidOtpException("请先获取验证码");

        // Check if OTP expired
        long minutesSinceGeneration = ChronoUnit.MINUTES
                .between(
                        user.getOtpGeneratedAt(), LocalDateTime.now()
                );

        if (minutesSinceGeneration > otpExpiryMinutes)
            throw new OtpExpiredException("验证码已过期，请重新获取");

        // Verify OTP code
        if (!user.getOtpCode().equals(otpCode))
            throw new InvalidOtpException("验证码错误");

        // reset
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setOtpCode(null);
        user.setOtpVerified(false);

        // Reset lockout if the account was locked
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockoutTime(null);

        userRepository.save(user);

        // Send confirmation notification
        notificationService.sendPasswordResetConfirmation(phoneNumber);

        log.info("Correct! Your password reset successful for: {}", phoneNumber);
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        return refreshTokenRepository.findByToken(request.getRefreshToken())
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    // TOKEN ROTATION: Delete used token, issue brand-new ones
                    refreshTokenRepository.deleteByUser(user);

                    String newAccessToken = jwtUtils.generateTokenFromUsername(user.getUsername());
                    RefreshToken newRefreshToken = createRefreshToken(user);

                    return TokenRefreshResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .build();
                })
                .orElseThrow(() -> new TokenRefreshException(request.getRefreshToken(),
                        "Refresh token is not in database!"));
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // 1. Blacklist the Access Token
        try {
            Date expiration = jwtUtils.getExpirationDateFromToken(accessToken);
            long ttlInSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;

            if (ttlInSeconds > 0)
                tokenBlacklistService.blacklistToken(accessToken, ttlInSeconds);
        } catch (Exception ex) {
            log.warn("Could not blacklist access token: {}", ex.getMessage());
        }

        // Revoke the refresh token
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> refreshTokenRepository
                        .deleteByUser(token.getUser()));
        log.info("User logged out successfully. Tokens revoked.");
    }

    // helpers methods
    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .expiryDate(Instant.now().plusMillis(jwtUtils.getRefreshExpirationTime()))
                .token(UUID.randomUUID().toString())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token has expired. Please log in again.");
        }
        return token;
    }
}
