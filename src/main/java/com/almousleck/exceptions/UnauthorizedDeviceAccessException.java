package com.almousleck.exceptions;

public class UnauthorizedDeviceAccessException extends RuntimeException {
    public UnauthorizedDeviceAccessException(String message) {
        super(message);
    }
}
