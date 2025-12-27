package com.almousleck.service;

import com.almousleck.exceptions.UserLockedException;
import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    @Value("${app.security.login.max-attempts:5}")
    private int maxFailedLoginAttempts;
    @Value("${app.security.login.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void loginSucceeded(String identifier) {
        findUserByIdentifier(identifier).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.getLocked()) {
                user.setFailedLoginAttempts(0);
                user.setLocked(false);
                user.setLockoutTime(null);
                userRepository.save(user);
                log.info("Login successful, reset attempts for user ID: {}", user.getId());
            }
        });
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void loginFailed(String identifier) {
        log.info("Login failed event received for: {}", identifier);

        Optional<User> userOpt = findUserByIdentifier(identifier);

        if (userOpt.isEmpty()) {
            // SECURITY: Don't reveal if user exists or not
            log.warn("Login failed for invalid identifier");
            return;
        }

        User user = userOpt.get();
        int currentAttempts = user.getFailedLoginAttempts();
        int newAttempts = currentAttempts + 1;
        user.setFailedLoginAttempts(newAttempts);

        log.info("Failed attempts for user ID {}: {}/{}", user.getId(), newAttempts, maxFailedLoginAttempts);

        // Warning notification before lockout (at the 4th attempt if max is 5)
        if (newAttempts == maxFailedLoginAttempts - 1) {
            notificationService.sendLoginWarningNotification(
                    user.getPhoneNumber(),
                    maxFailedLoginAttempts - newAttempts
            );
            log.warn("Warning sent - User {} is 1 attempt away from lockout", user.getId());
        }

        // Lock account if max attempts reached
        if (newAttempts >= maxFailedLoginAttempts) {
            user.setLocked(true);
            user.setLockoutTime(LocalDateTime.now());
            userRepository.save(user);

            LocalDateTime unlockTime = user.getLockoutTime().plusMinutes(lockDurationMinutes);
            log.warn("Account locked: User ID {} after {} failed attempts. Unlock at: {}",
                    user.getId(), newAttempts, unlockTime);

            // Notify user via SMS
            notificationService.sendAccountLockedNotification(user.getPhoneNumber(), unlockTime);
        } else {
            userRepository.save(user);
            log.info("⚠Failed login attempt {}/{} for user ID: {}", newAttempts, maxFailedLoginAttempts, user.getId());
        }

    }

    @Transactional
    public void unlockAccount(String identifier) {
        findUserByIdentifier(identifier).ifPresent(user -> {
            user.setLocked(false);
            user.setFailedLoginAttempts(0);
            user.setLockoutTime(null);
            userRepository.save(user);
            log.info("Admin manually unlocked account: User ID {}", user.getId());
            // TODO: Add audit trail logging here
        });
    }

    public void checkAccountLock(String identifier) {
        findUserByIdentifier(identifier).ifPresent(user -> {
            if (user.getLocked()) {
                LocalDateTime unlockTime = user.getLockoutTime()
                        .plusMinutes(lockDurationMinutes);

                if (unlockTime.isAfter(LocalDateTime.now())) {
                    // still locked and reject login
                    log.warn("Locked account login attempt: User ID {}", user.getId());
                    throw new UserLockedException("账户已被锁定", unlockTime);
                } else {
                    // Auto-unlock - lock duration expired
                    user.setLocked(false);
                    user.setFailedLoginAttempts(0);
                    user.setLockoutTime(null);
                    userRepository.save(user);
                    log.info("Account auto-unlocked: User ID {}", user.getId());
                }
            }
        });
    }



    // Helper method
    private Optional<User> findUserByIdentifier(String identifier) {
        return userRepository.findByUsernameOrPhoneNumber(identifier, identifier);
    }
}
