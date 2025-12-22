package com.almousleck.exceptions;

public class InsufficientPermissionsException extends RuntimeException{
    public InsufficientPermissionsException(String message) {
        super(message);
    }
}
