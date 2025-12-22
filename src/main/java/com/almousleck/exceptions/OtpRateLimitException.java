package com.almousleck.exceptions;

public class OtpRateLimitException extends RuntimeException {
    private final int retryAfterSeconds;

    public OtpRateLimitException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
