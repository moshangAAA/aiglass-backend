package com.almousleck.service.impl;

import com.almousleck.config.ApplicationUserDetails;
import com.almousleck.dto.AuthResponse;
import com.almousleck.dto.LoginRequest;
import com.almousleck.dto.RegisterRequest;
import com.almousleck.enums.UserRole;
import com.almousleck.exceptions.ResourceAlreadyExistsException;
import com.almousleck.jwt.JwtUtils;
import com.almousleck.model.RefreshToken;
import com.almousleck.model.User;
import com.almousleck.repository.RefreshTokenRepository;
import com.almousleck.repository.UserRepository;
import com.almousleck.service.NotificationService;
import com.almousleck.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository  userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager  authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private com.almousleck.service.LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationServiceImpl;

    //Test data containers
    private RegisterRequest  registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {

        // create data
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("almousleck");
        registerRequest.setPhoneNumber("123456789");
        registerRequest.setPassword("password");

        // Login
        loginRequest = new LoginRequest();
        loginRequest.setIdentifier("almousleck");
        loginRequest.setPassword("password");

        user = new User();
        user.setId(1L);
        user.setUsername("almousleck");
        user.setPhoneNumber("123456789");
        user.setPasswordHash("password");
        user.setRole(UserRole.USER);
    }

    @Test
    void register_ShouldRegisterUser_WhenRequestIsValid() {
        // Arrange
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())).thenReturn(false);
        when(modelMapper.map(registerRequest, User.class)).thenReturn(user);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        
        // Act
        authenticationServiceImpl.register(registerRequest);
        
        // Assert
        verify(userRepository).save(user);
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(notificationService).sendOtp(anyString(), anyString(), anyInt());
    }

    @Test
    void register_ShouldThrowException_WhenUsernameExists() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class, () ->
                authenticationServiceImpl.register(registerRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShouldThrowException_WhenPhoneNumberExists() {
        when(userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())).thenReturn(true);
        assertThrows(ResourceAlreadyExistsException.class, () ->
                authenticationServiceImpl.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        Authentication authentication = mock(Authentication.class);
        ApplicationUserDetails userDetails = ApplicationUserDetails
                .buildApplicationDetails(user);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateTokenForUser(authentication)).thenReturn("jwt-token");
        
        // Mock Refresh Token Logic
        when(jwtUtils.getRefreshExpirationTime()).thenReturn(3600000L); // 1 hour
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        AuthResponse response = authenticationServiceImpl.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("almousleck", response.getUsername());
        assertEquals(UserRole.USER, response.getRole());
        verify(refreshTokenRepository).deleteByUser(user);
    }

}