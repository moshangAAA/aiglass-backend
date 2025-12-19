package com.almousleck.service.impl;

import com.almousleck.config.ApplicationUserDetails;
import com.almousleck.dto.AuthResponse;
import com.almousleck.dto.LoginRequest;
import com.almousleck.dto.RegisterRequest;
import com.almousleck.enums.UserRole;
import com.almousleck.exceptions.ResourceAlreadyExistsException;
import com.almousleck.jwt.JwtUtils;
import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
import com.almousleck.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final ModelMapper modelMapper;

    @Override
    public void register(RegisterRequest request) {
        // check if the username is taken
        if (userRepository.existsByUsername(request.getUsername()))
            throw new ResourceAlreadyExistsException("用户名已被占用");

        // check if the phone is taken
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber()))
                throw new ResourceAlreadyExistsException("手机号已被占用");

        // map user to model mapper
        User user = modelMapper.map(request, User.class);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        if (user.getRole() == null)
            user.setRole(UserRole.USER);
        userRepository.save(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication  authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword())
        );
        String jwt = jwtUtils.generateTokenForUser(authentication);
        ApplicationUserDetails userDetails = (ApplicationUserDetails) authentication.getPrincipal();
        
        return new AuthResponse(jwt, userDetails.getUsername(), userDetails.getRole());
    }
}
