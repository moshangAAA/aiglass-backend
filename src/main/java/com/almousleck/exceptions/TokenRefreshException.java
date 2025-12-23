package com.almousleck.exceptions;

public class TokenRefreshException extends RuntimeException{

    public TokenRefreshException(String token, String message) {
        super(String.format("fao;ed for [%s]", token, message));
    }
}
