package com.almousleck.service.impl;

import com.almousleck.config.ApplicationUserDetails;
import com.almousleck.dto.AuthResponse;
import com.almousleck.dto.LoginRequest;
import com.almousleck.dto.OtpResponse;
import com.almousleck.dto.RegisterRequest;
import com.almousleck.enums.UserRole;
import com.almousleck.exceptions.*;
import com.almousleck.jwt.JwtUtils;
import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
import com.almousleck.service.AuthenticationService;
import com.almousleck.service.NotificationService;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
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
    public AuthResponse login(LoginRequest request) {
        Authentication  authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword())
        );

        String jwt = jwtUtils.generateTokenForUser(authentication);
        ApplicationUserDetails userDetails = (ApplicationUserDetails) authentication.getPrincipal();
        
        return new AuthResponse(jwt, userDetails.getUsername(), userDetails.getRole());
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
}
