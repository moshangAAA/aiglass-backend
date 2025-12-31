package com.almousleck.service;

import com.almousleck.exceptions.UserLockedException;
import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    private User user;

    @BeforeEach
    void setUp() {
        // Set the @Value fields using ReflectionTestUtils
        ReflectionTestUtils.setField(loginAttemptService, "maxFailedLoginAttempts", 5);
        ReflectionTestUtils.setField(loginAttemptService, "lockDurationMinutes", 30);
        
        user = new User();
        user.setId(1L); // Set ID for proper logging
        user.setUsername("testuser");
        user.setPhoneNumber("+1234567890");
        user.setFailedLoginAttempts(0);
        user.setLocked(false);
    }

    @Test
    void loginSucceeded_ShouldResetAttempts() {
        user.setFailedLoginAttempts(3);
        when(userRepository.findByUsernameOrPhoneNumber("testuser", "testuser")).thenReturn(Optional.of(user));

        loginAttemptService.loginSucceeded("testuser");

        assertEquals(0, user.getFailedLoginAttempts());
        assertFalse(user.getLocked());
        verify(userRepository).save(user);
    }

    @Test
    void loginFailed_ShouldIncrementAttempts() {
        when(userRepository.findByUsernameOrPhoneNumber("testuser", "testuser")).thenReturn(Optional.of(user));

        loginAttemptService.loginFailed("testuser");

        assertEquals(1, user.getFailedLoginAttempts());
        verify(userRepository).save(user);
    }

    @Test
    void loginFailed_ShouldTriggerWarning_On4thAttempt() {
        // Warning on 4th attempt (current was 3, +1 = 4)
        user.setFailedLoginAttempts(3);
        when(userRepository.findByUsernameOrPhoneNumber("testuser", "testuser")).thenReturn(Optional.of(user));

        loginAttemptService.loginFailed("testuser");

        assertEquals(4, user.getFailedLoginAttempts());
        verify(notificationService).sendLoginWarningNotification(eq("+1234567890"), eq(1));
    }

    @Test
    void loginFailed_ShouldLockAccount_OnMaxAttempts() {
        user.setFailedLoginAttempts(4);
        when(userRepository.findByUsernameOrPhoneNumber("testuser", "testuser")).thenReturn(Optional.of(user));

        loginAttemptService.loginFailed("testuser");

        assertEquals(5, user.getFailedLoginAttempts());
        assertTrue(user.getLocked());
        assertNotNull(user.getLockoutTime());
        verify(notificationService).sendAccountLockedNotification(eq("+1234567890"), any());
        verify(userRepository).save(user);
    }

    @Test
    void checkAccountLock_ShouldThrowException_IfLocked() {
        user.setLocked(true);
        // Set lockout time to now, so unlock time (now + 30 min) is in the future
        user.setLockoutTime(LocalDateTime.now());
        when(userRepository.findByUsernameOrPhoneNumber("testuser", "testuser")).thenReturn(Optional.of(user));

        assertThrows(UserLockedException.class, () -> loginAttemptService.checkAccountLock("testuser"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void checkAccountLock_ShouldAutoUnlock_IfTimeExpired() {
        user.setLocked(true);
        // Set lockout time to 31 minutes ago (expired)
        user.setLockoutTime(LocalDateTime.now().minusMinutes(31));
        when(userRepository.findByUsernameOrPhoneNumber("testuser", "testuser")).thenReturn(Optional.of(user));

        loginAttemptService.checkAccountLock("testuser");

        assertFalse(user.getLocked());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockoutTime());
        verify(userRepository).save(user);
    }

    @Test
    void unlockAccount_ShouldManuallyUnlock() {
        user.setLocked(true);
        user.setFailedLoginAttempts(5);
        when(userRepository.findByUsernameOrPhoneNumber("testuser", "testuser")).thenReturn(Optional.of(user));

        loginAttemptService.unlockAccount("testuser");

        assertFalse(user.getLocked());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockoutTime());
        verify(userRepository).save(user);
    }
}
