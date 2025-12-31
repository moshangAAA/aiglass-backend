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

import java.time.Instant;
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
    private final MessageService messageService;
    private final OtpService otpService;

    @Value("${app.security.otp-expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.security.otp-include-in-response:false}")
    private boolean includeOtpInResponse;

    @Override
    @Transactional
    public OtpResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new ResourceAlreadyExistsException(messageService.getMessage("error.username.taken"));

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber()))
            throw new ResourceAlreadyExistsException(messageService.getMessage("error.phone.taken"));

        User user = modelMapper.map(request, User.class);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(user.getRole() == null ? UserRole.USER : user.getRole());
        user.setPhoneVerified(false);
        userRepository.save(user);

        String otp = handleOtpFlow(user.getPhoneNumber(), "REGISTER");
        log.info("New user registered, verification OTP sent to: {}", user.getPhoneNumber());

        return createOtpResponse(otp, "otp.register.success");
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        loginAttemptService.checkAccountLock(request.getIdentifier());

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
        );

        ApplicationUserDetails userDetails = (ApplicationUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        // Token Rotation: Clear old tokens and generate new pair
        refreshTokenRepository.deleteByUser(user);
        String jwt = jwtUtils.generateTokenForUser(auth);
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("User logged in: {}", user.getUsername());
        return AuthResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public void verifyOtp(String phoneNumber, String otpCode) {
        if (!otpService.validateOtp(phoneNumber, otpCode)) {
            throw new InvalidOtpException(messageService.getMessage("error.otp.invalid"));
        }

        User user = findUserByPhoneOrThrow(phoneNumber);
        user.setPhoneVerified(true);
        userRepository.save(user);

        // Security best practice: One-time use cleanup
        otpService.clearOtp(phoneNumber);
        notificationService.sendPhoneVerifiedNotification(phoneNumber);
        log.info("Phone verified successfully for: {}", phoneNumber);
    }

    @Override
    public OtpResponse resendOtp(String phoneNumber) {
        findUserByPhoneOrThrow(phoneNumber);
        String otp = handleOtpFlow(phoneNumber, "RESEND");
        return createOtpResponse(otp, "otp.resend.success");
    }

    @Override
    @Transactional
    public OtpResponse forgotPassword(String phoneNumber) {
        findUserByPhoneOrThrow(phoneNumber);
        String otp = handleOtpFlow(phoneNumber, "FORGOT_PWD");
        return createOtpResponse(otp, "otp.forgot.success");
    }

    @Override
    @Transactional
    public void resetPassword(String phoneNumber, String otpCode, String newPassword) {
        if (!otpService.validateOtp(phoneNumber, otpCode)) {
            throw new InvalidOtpException(messageService.getMessage("error.otp.invalid"));
        }

        User user = findUserByPhoneOrThrow(phoneNumber);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Reset security state
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        otpService.clearOtp(phoneNumber);
        notificationService.sendPasswordResetConfirmation(phoneNumber);
        log.info("Password reset successful for user: {}", phoneNumber);
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        return refreshTokenRepository.findByToken(request.getRefreshToken())
                .map(this::verifyTokenExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    refreshTokenRepository.deleteByUser(user);
                    String newAccessToken = jwtUtils.generateTokenFromUsername(user.getUsername());
                    RefreshToken newRefreshToken = createRefreshToken(user);
                    return TokenRefreshResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .build();
                })
                .orElseThrow(() -> new TokenRefreshException(request.getRefreshToken(), "Invalid refresh token"));
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        try {
            Date expiration = jwtUtils.getExpirationDateFromToken(accessToken);
            long ttl = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            if (ttl > 0) tokenBlacklistService.blacklistToken(accessToken, ttl);
        } catch (Exception e) {
            log.warn("Logout warning: Could not blacklist access token: {}", e.getMessage());
        }

        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> refreshTokenRepository.deleteByUser(token.getUser()));
        log.info("Logout successful, tokens revoked.");
    }

    // --- Private Infrastructure Helpers ---

    private String handleOtpFlow(String phone, String type) {
        if (otpService.isRateLimited(phone)) {
            throw new OtpRateLimitException(messageService.getMessage("error.otp.rate-limit"), 60);
        }

        String code = otpService.generateOtp(phone);

        if ("FORGOT_PWD".equals(type)) {
            notificationService.sendPasswordResetOtp(phone, code, otpExpiryMinutes);
        } else {
            notificationService.sendOtp(phone, code, otpExpiryMinutes);
        }
        return code;
    }

    private OtpResponse createOtpResponse(String code, String msgKey) {
        return new OtpResponse(
                messageService.getMessage(msgKey),
                otpExpiryMinutes * 60,
                includeOtpInResponse ? code : null
        );
    }

    private User findUserByPhoneOrThrow(String phone) {
        return userRepository.findUserByPhoneNumber(phone)
                .orElseThrow(() -> new UserNotFoundException(messageService.getMessage("error.user.not-found")));
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .expiryDate(Instant.now().plusMillis(jwtUtils.getRefreshExpirationTime()))
                .token(UUID.randomUUID().toString())
                .build();
        return refreshTokenRepository.save(token);
    }

    private RefreshToken verifyTokenExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token expired. Please login again.");
        }
        return token;
    }
}
