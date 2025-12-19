package com.almousleck.service;

import com.almousleck.dto.AuthResponse;
import com.almousleck.dto.LoginRequest;
import com.almousleck.dto.RegisterRequest;

public interface AuthenticationService {
    void register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
