package com.almousleck.service.impl;

import com.almousleck.config.ApplicationUserDetails;
import com.almousleck.dto.AuthResponse;
import com.almousleck.dto.LoginRequest;
import com.almousleck.dto.OtpResponse;
import com.almousleck.dto.RegisterRequest;
import com.almousleck.enums.UserRole;
import com.almousleck.exceptions.InvalidOtpException;
import com.almousleck.exceptions.OtpRateLimitException;
import com.almousleck.exceptions.ResourceAlreadyExistsException;
import com.almousleck.jwt.JwtUtils;
import com.almousleck.model.RefreshToken;
import com.almousleck.model.User;
import com.almousleck.repository.RefreshTokenRepository;
import com.almousleck.repository.UserRepository;
import com.almousleck.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtils jwtUtils;
    @Mock private ModelMapper modelMapper;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private NotificationService notificationService;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private MessageService messageService;
    @Mock private OtpService otpService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private RegisterRequest registerRequest;
    private User user;
    private final String phone = "+1234567890";

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPhoneNumber(phone);
        registerRequest.setPassword("password123");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPhoneNumber(phone);
        user.setRole(UserRole.USER);
        user.setPhoneVerified(false);
        user.setLocked(false);
    }

    @Test
    @DisplayName("Registration: Should save user and dispatch OTP when request is valid")
    void register_Success() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(modelMapper.map(any(), eq(User.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pwd");

        // Stubs for handleOtpFlow
        when(otpService.isRateLimited(phone)).thenReturn(false);
        when(otpService.generateOtp(phone)).thenReturn("123456");
        when(messageService.getMessage(anyString())).thenReturn("Success");

        // Act
        OtpResponse response = authenticationService.register(registerRequest);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(user);
        verify(notificationService).sendOtp(eq(phone), eq("123456"), anyInt());
    }

    @Test
    @DisplayName("Registration: Should throw OtpRateLimitException if user is on cooldown")
    void register_Fail_RateLimited() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(modelMapper.map(any(), eq(User.class))).thenReturn(user);
        when(otpService.isRateLimited(phone)).thenReturn(true);
        when(messageService.getMessage("error.otp.rate-limit")).thenReturn("Too fast");

        // Act & Assert
        assertThrows(OtpRateLimitException.class, () -> authenticationService.register(registerRequest));
        verify(notificationService, never()).sendOtp(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Verification: Should mark phone as verified and clear Redis OTP")
    void verifyOtp_Success() {
        // Arrange
        when(otpService.validateOtp(phone, "123456")).thenReturn(true);
        when(userRepository.findUserByPhoneNumber(phone)).thenReturn(Optional.of(user));

        // Act
        authenticationService.verifyOtp(phone, "123456");

        // Assert
        assertTrue(user.getPhoneVerified());
        verify(userRepository).save(user);
        verify(otpService).clearOtp(phone);
        verify(notificationService).sendPhoneVerifiedNotification(phone);
    }

    @Test
    @DisplayName("Verification: Should throw InvalidOtpException when code is wrong")
    void verifyOtp_Fail_InvalidCode() {
        // Arrange
        when(otpService.validateOtp(phone, "wrong")).thenReturn(false);
        when(messageService.getMessage("error.otp.invalid")).thenReturn("Invalid");

        // Act & Assert
        assertThrows(InvalidOtpException.class, () -> authenticationService.verifyOtp(phone, "wrong"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login: Should return tokens when user is verified and credentials match")
    void login_Success() {
        // Arrange
        user.setPhoneVerified(true);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("testuser");
        loginRequest.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        ApplicationUserDetails details = ApplicationUserDetails.buildApplicationDetails(user);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(details);
        when(jwtUtils.generateTokenForUser(auth)).thenReturn("jwt_token");
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());

        // Act
        AuthResponse response = authenticationService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt_token", response.getToken());
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    @DisplayName("Reset Password: Should update password and clear OTP security state")
    void resetPassword_Success() {
        // Arrange
        when(otpService.validateOtp(phone, "123456")).thenReturn(true);
        when(userRepository.findUserByPhoneNumber(phone)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new_pwd")).thenReturn("new_hash");

        // Act
        authenticationService.resetPassword(phone, "123456", "new_pwd");

        // Assert
        assertEquals("new_hash", user.getPasswordHash());
        assertFalse(user.getLocked());
        verify(otpService).clearOtp(phone);
        verify(notificationService).sendPasswordResetConfirmation(phone);
    }
}