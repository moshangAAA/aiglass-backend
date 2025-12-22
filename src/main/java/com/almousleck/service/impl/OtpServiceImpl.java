package com.almousleck.service.impl;

import com.almousleck.dto.OtpResponse;
import com.almousleck.exceptions.InvalidOtpException;
import com.almousleck.exceptions.OtpExpiredException;
import com.almousleck.exceptions.OtpRateLimitException;
import com.almousleck.exceptions.UserNotFoundException;
import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
import com.almousleck.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private final UserRepository userRepository;
    private static final SecureRandom random = new SecureRandom();

    @Value("${app.security.otp.expiryMinutes}")
    private int otpExpiryMinutes;
    @Value("${app.security.otp.mode}")
    private String otpMode;
    @Value("${app.security.otp.rateLimitMinutes}")
    private int rateLimitMinutes;

    @Override
    public OtpResponse generateOtp(String phoneNumber) {
        User user = userRepository.findUserByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("手机号未注册 " + phoneNumber));

        //check rate limiting
        if (user.getOtpGeneratedAt() != null) {
            long minutesSinceLastOpt = ChronoUnit.MINUTES.between(
                    user.getOtpGeneratedAt(),
                    LocalDateTime.now()
            );

            if (minutesSinceLastOpt < rateLimitMinutes) {
                int retryAfter = (int) (rateLimitMinutes - minutesSinceLastOpt) * 60;
                throw new OtpRateLimitException("OTP 发送频率过快，请稍后重试", retryAfter);
            }
        }
        // generate 6 digit opt
        String otpCode = String.format("%06d", random.nextInt(1000000));

        //save otp to user
        user.setOtpCode(otpCode);
        user.setOtpGeneratedAt(LocalDateTime.now());
        user.setOtpVerified(false);
        userRepository.save(user);

        // Log to console in development mode
        if ("console".equalsIgnoreCase(otpMode)) {
            log.info("=".repeat(50));
            log.info("📱 OTP for {}: {}", phoneNumber, otpCode);
            log.info("⏰ Expires in {} minutes", otpExpiryMinutes);
            log.info("=".repeat(50));
        }

        OtpResponse response = new OtpResponse();
        response.setMessage("OTP sent successfully");
        response.setExpiresInSeconds(otpExpiryMinutes * 60);

        // Include OTP in response for development mode
        if ("console".equalsIgnoreCase(otpMode)) {
            response.setOtpCode(otpCode);
        }

        return response;
    }

    @Override
    @Transactional
    public void verifyOtp(String phoneNumber, String optCode) {
        User user = userRepository.findUserByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("手机号未注册 " + phoneNumber));

        // Check if otp exist
        if (user.getOtpCode() == null || user.getOtpGeneratedAt() == null)
                throw new InvalidOtpException("OTP 未发送，请先发送OTP");

        // check if opt expired
        long minutesSinceOtpGenerated = ChronoUnit.MINUTES.between(
                user.getOtpGeneratedAt(), LocalDateTime.now()
        );

        if (minutesSinceOtpGenerated > otpExpiryMinutes)
                throw new OtpExpiredException("验证码已过期，请重新获取");

        // verify otp code
        if (!user.getOtpCode().equals(optCode))
                throw new InvalidOtpException("验证码错误，请重新输入");

        // mark otp as verified
        user.setOtpVerified(true);
        user.setPhoneVerified(true);
        user.setOtpCode(null); // Clear OTP after verification
        userRepository.save(user);
        log.info("✅ Phone verified successfully for: {}", phoneNumber);
    }

    @Override
    @Transactional
    public OtpResponse resendOpt(String phoneNumber) {
        return generateOtp(phoneNumber);
    }
}
