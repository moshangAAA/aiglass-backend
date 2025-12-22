package com.almousleck.exceptions;

import java.time.LocalDateTime;

public class UserLockedException extends RuntimeException{
    private final LocalDateTime unlockTime;

    public UserLockedException(String message, LocalDateTime unlockTime) {
        super(message);
        this.unlockTime = unlockTime;
    }

    public LocalDateTime getUnlockTime() {
        return unlockTime;
    }
}
