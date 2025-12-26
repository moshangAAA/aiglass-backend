package com.almousleck.service.impl;

import com.almousleck.exceptions.InvalidOtpException;
import com.almousleck.exceptions.OtpExpiredException;
import com.almousleck.exceptions.OtpRateLimitException;
import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
import com.almousleck.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setPhoneNumber("+1234567890");
        user.setOtpCode("123456");
        user.setOtpGeneratedAt(LocalDateTime.now());
        
        ReflectionTestUtils.setField(authenticationService, "otpExpiryMinutes", 5);
    }

    @Test
    void verifyOtp_ShouldSucceed_WhenValid() {
        when(userRepository.findUserByPhoneNumber("+1234567890")).thenReturn(Optional.of(user));

        authenticationService.verifyOtp("+1234567890", "123456");

        assertTrue(user.getOtpVerified());
        assertTrue(user.getPhoneVerified());
        assertNull(user.getOtpCode());
        verify(userRepository).save(user);
        verify(notificationService).sendPhoneVerifiedNotification("+1234567890");
    }

    @Test
    void verifyOtp_ShouldThrow_WhenCodeMismatch() {
        when(userRepository.findUserByPhoneNumber("+1234567890")).thenReturn(Optional.of(user));

        assertThrows(InvalidOtpException.class, 
            () -> authenticationService.verifyOtp("+1234567890", "654321"));
    }

    @Test
    void verifyOtp_ShouldThrow_WhenExpired() {
        user.setOtpGeneratedAt(LocalDateTime.now().minusMinutes(6)); // Expired
        when(userRepository.findUserByPhoneNumber("+1234567890")).thenReturn(Optional.of(user));

        assertThrows(OtpExpiredException.class, 
            () -> authenticationService.verifyOtp("+1234567890", "123456"));
    }

    @Test
    void resendOtp_ShouldThrow_WhenRateLimited() {
        // Just generated OTP 10 seconds ago
        user.setOtpGeneratedAt(LocalDateTime.now().minusSeconds(10));
        when(userRepository.findUserByPhoneNumber("+1234567890")).thenReturn(Optional.of(user));

        assertThrows(OtpRateLimitException.class, 
            () -> authenticationService.resendOtp("+1234567890"));
    }

    @Test
    void resendOtp_ShouldSucceed_AfterCoolDown() {
        // Generated 2 minutes ago (limit is 1 minute)
        user.setOtpGeneratedAt(LocalDateTime.now().minusMinutes(2));
        when(userRepository.findUserByPhoneNumber("+1234567890")).thenReturn(Optional.of(user));

        authenticationService.resendOtp("+1234567890");

        verify(notificationService).sendOtp(eq("+1234567890"), anyString(), eq(5));
        verify(userRepository).save(user);
    }

    @Test
    void testGenerateAndSetOtp_ShouldSetOtpAndTimestamp() {
        User user = new User();

        String otp = authenticationService.generateAndSetOtp(user);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertNotNull(user.getOtpGeneratedAt());
    }
}
