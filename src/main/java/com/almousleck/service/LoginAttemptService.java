package com.almousleck.service;

import com.almousleck.exceptions.UserLockedException;
import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;
    private static final int ATTEMPTS_TIME = 1;

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public void loginSucceeded(String identifier) {
        User user = findUserByIdentifier(identifier);
        if (user != null && (user.getFailedLoginAttempts() > 0 || user.getLocked())) {
            user.setFailedLoginAttempts(0);
            user.setLocked(false);
            user.setLockoutTime(null);
            userRepository.save(user);
            log.info("Login successful, reset attempts for: {}", identifier);
        }
    }

    @Transactional
    public void loginFailed(String identifier) {
        log.info("🔍 Login failed event received for: {}", identifier);

        User user = findUserByIdentifier(identifier);
        if (user != null) {
            int attempts = user.getFailedLoginAttempts() + ATTEMPTS_TIME;
            user.setFailedLoginAttempts(attempts);

            log.info("📊 Current failed attempts for {}: {}/{}",
                    identifier, attempts, MAX_FAILED_LOGIN_ATTEMPTS);

            // Warning on 4th attempt (MAX=5, so checking if attempts == 4)
            if (attempts == MAX_FAILED_LOGIN_ATTEMPTS - 1) {
                notificationService.sendLoginWarningNotification(
                        user.getPhoneNumber(),
                        MAX_FAILED_LOGIN_ATTEMPTS - attempts
                );
            }

            if (attempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                user.setLocked(true);
                user.setLockoutTime(LocalDateTime.now());
                userRepository.save(user);

                log.warn("Account locked: {} after {} failed attempts", identifier, attempts);

                // Send SMS notification
                LocalDateTime unlockTime = user.getLockoutTime().plusMinutes(LOCK_DURATION_MINUTES);
                notificationService.sendAccountLockedNotification(user.getPhoneNumber(), unlockTime);
            } else {
                userRepository.save(user);
                log.info("⚠️ Failed login attempt {}/{} for: {}",
                        attempts, MAX_FAILED_LOGIN_ATTEMPTS, identifier);
            }
        } else {
            log.warn("❌ Login failed for non-existent user: {}", identifier);
        }
    }

    @Transactional
    public void unlockAccount(String identifier) {
        User user = findUserByIdentifier(identifier);
        if (user != null) {
            user.setLocked(false);
            user.setFailedLoginAttempts(0);
            user.setLockoutTime(null);
            userRepository.save(user);
            log.info("🔓 Admin manually unlocked account: {}", identifier);
        }
    }

    public void checkAccountLock(String identifier) {
        User user = findUserByIdentifier(identifier);
        if (user != null && user.getLocked()) {
            LocalDateTime unlockTime = user.getLockoutTime()
                    .plusMinutes(LOCK_DURATION_MINUTES);

            if (unlockTime.isAfter(LocalDateTime.now()))
                // Still locked
                throw new UserLockedException("账户已被锁定", unlockTime);
            else {
                // Auto-unlock
                user.setLocked(false);
                user.setFailedLoginAttempts(0);
                user.setLockoutTime(null);
                userRepository.save(user);
                log.info("🔓 Account auto-unlocked: {}", identifier);
            }
        }
    }



    // Helper method
    private User findUserByIdentifier(String identifier) {
        return userRepository.findByUsernameOrPhoneNumber(identifier, identifier)
                .orElse(null);
    }
}
