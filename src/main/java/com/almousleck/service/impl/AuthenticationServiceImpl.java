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
import com.almousleck.service.*;
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
    private final LoginAttemptService loginAttemptService;
    private final MessageService  messageService;


    private static final SecureRandom random = new SecureRandom();
    @Value("${app.security.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;
    @Value("${app.security.otp.include-in-response:false}")
    private boolean includeOtpInResponse;

    @Override
    public OtpResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new ResourceAlreadyExistsException(messageService.getMessage("error.username.taken"));

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber()))
                throw new ResourceAlreadyExistsException(messageService.getMessage("error.phone.taken"));

        User user = modelMapper.map(request, User.class);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        if (user.getRole() == null)
            user.setRole(UserRole.USER);

        // Generate OTP phone verification
       String otpCode = generateAndSetOtp(user);
       user.setOtpVerified(false);
       user.setPhoneVerified(false);

        userRepository.save(user);

        // Send OTP notification
        notificationService.sendOtp(request.getPhoneNumber(), otpCode, otpExpiryMinutes);

        log.info("User registered: {}", request.getUsername());

        return new OtpResponse(
                messageService.getMessage("otp.register.success")
                ,
                otpExpiryMinutes * 60,
                includeOtpInResponse ? otpCode : null);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        loginAttemptService.checkAccountLock(request.getIdentifier());

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
        validateOtpCode(user, otpCode);

        // Mark as verified
        user.setOtpVerified(true);
        user.setPhoneVerified(true);
        user.setOtpCode(null);
        user.setOtpGeneratedAt(null);

        userRepository.save(user);
        // Send a success message
        notificationService.sendPhoneVerifiedNotification(phoneNumber);
        log.info("Phone verified: {}", phoneNumber);
    }

    @Override
    public OtpResponse resendOtp(String phoneNumber) {
        User user = userRepository.findUserByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("手机号未注册"));

        checkOtpRateLimit(user);

        String otpCode = generateAndSetOtp(user);
        userRepository.save(user);

        // Send OTP
        notificationService.sendOtp(phoneNumber, otpCode, otpExpiryMinutes);

        log.info("OTP resent to: {}", phoneNumber);
        return new OtpResponse(
                "验证码已重新发送",
                otpExpiryMinutes * 60,
                includeOtpInResponse ? otpCode : null
        );
    }

    @Override
    @Transactional
    public OtpResponse forgotPassword(String phoneNumber) {
        User user = userRepository.findUserByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("手机号未注册"));

       checkOtpRateLimit(user);

       String otpCode = generateAndSetOtp(user);
       userRepository.save(user);

        // Send password forgot OTP
        notificationService.sendPasswordResetOtp(phoneNumber, otpCode, otpExpiryMinutes);

        log.info("Password reset OTP sent to: {}", phoneNumber);
        return new OtpResponse(
                "密码重置验证码已发送",
                otpExpiryMinutes * 60,
                includeOtpInResponse ? otpCode : null
        );
    }

    @Override
    @Transactional
    public void resetPassword(String phoneNumber, String otpCode, String newPassword) {
        User user = userRepository.findUserByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("手机号未注册"));

       validateOtpCode(user, otpCode);

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

    public String generateAndSetOtp(User user) {
        String otpCode = String.format("%06d", random.nextInt(1000000));
        user.setOtpCode(otpCode);
        user.setOtpGeneratedAt(LocalDateTime.now());
        return otpCode;
    }

    // Validate OTP code and expiration
    private void validateOtpCode(User user, String otpCode) {
        // is otp valid
        if (user.getOtpCode() == null || user.getOtpGeneratedAt() == null)
            throw new InvalidOtpException("请先获取验证码");

        // check if otp expired
        long minutesSinceGeneration = ChronoUnit.MINUTES.between(
                user.getOtpGeneratedAt(), LocalDateTime.now()
        );

        if (minutesSinceGeneration > otpExpiryMinutes)
            throw new OtpExpiredException("验证码已过期，请重新获取");

        //verify
        if (!user.getOtpCode().equals(otpCode))
            throw new InvalidOtpException("验证码错误");
    }

    // check the rate limiting to prevent spam
    private void checkOtpRateLimit(User user) {
        if (user.getOtpGeneratedAt() != null) {
            long minutesSinceLastOtp = ChronoUnit.MINUTES.between(
                    user.getOtpGeneratedAt(), LocalDateTime.now()
            );

            if (minutesSinceLastOtp < 1) {
                int retryAfterSeconds = (int) ((1 - minutesSinceLastOtp) * 60);
                throw new OtpRateLimitException("请求过于频繁", retryAfterSeconds);
            }
        }
    }
}
