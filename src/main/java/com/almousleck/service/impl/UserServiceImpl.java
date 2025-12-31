package com.almousleck.service.impl;

import com.almousleck.config.ApplicationUserDetails;
import com.almousleck.dto.user.ChangePasswordRequest;
import com.almousleck.dto.user.UpdateProfileRequest;
import com.almousleck.dto.user.UserResponse;
import com.almousleck.enums.UserStatus;
import com.almousleck.exceptions.InsufficientPermissionsException;
import com.almousleck.exceptions.ResourceAlreadyExistsException;
import com.almousleck.exceptions.UserNotFoundException;
import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
import com.almousleck.service.SystemLogService;
import com.almousleck.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final PasswordEncoder  passwordEncoder;
    private final HttpServletRequest request;
    private final ModelMapper modelMapper;

    @Override
    public Page<UserResponse> getAllUsers(UserStatus status, Pageable pageable) {
        Page<User> users = (status != null)
                ? userRepository.findByStatus(status, pageable)
                : userRepository.findAll(pageable);
        return users.map(this::mapToUserResponse);
    }

    @Override
    public UserResponse getUserById(Long userId) {
        return mapToUserResponse(getUserByIdOrThrow(userId));
    }

    @Override
    public UserResponse updateUserStatus(Long userId, UserStatus status, String reason) {
        User user = getUserByIdOrThrow(userId);
        UserStatus oldStatus = user.getStatus();
        user.setStatus(status);
        userRepository.save(user);

        User currentAdmin = getCurrentUser();
        systemLogService.logAction("USER_STATUS_UPDATED", currentAdmin, "USER", userId,
                oldStatus.toString(), status.toString(), request);

        log.info("User status updated: {} -> {} by admin {}", userId, status, currentAdmin.getId());
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserByIdOrThrow(userId);
        UserStatus oldStatus = user.getStatus();
        user.setStatus(UserStatus.BANNED);
        userRepository.save(user);

        User currentAdmin = getCurrentUser();
        systemLogService.logAction("USER_DELETED", currentAdmin, "USER", userId,
                oldStatus.toString(), UserStatus.BANNED.toString(), request);

        log.info("User soft-deleted (banned): {} by admin {}", userId, currentAdmin.getId());
    }

    @Override
    public UserResponse getMyProfile(Long userId) {
        User authenticatedUser = getCurrentUser();
        if (!authenticatedUser.getId().equals(userId))
            throw new InsufficientPermissionsException("You can only access your own profile");
        return mapToUserResponse(getUserByIdOrThrow(userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        // Security: Verify user can only update their own profile
        User authenticatedUser = getCurrentUser();
        if (!authenticatedUser.getId().equals(userId)) {
            throw new InsufficientPermissionsException("You can only update your own profile");
        }
        
        User user = getUserByIdOrThrow(userId);

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ResourceAlreadyExistsException("Username already taken");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getPreference() != null) {
            user.setPreference(request.getPreference());
        }

        userRepository.save(user);
        log.info("Profile updated for user: {}", userId);
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // Security: Verify user can only change their own password
        User authenticatedUser = getCurrentUser();
        if (!authenticatedUser.getId().equals(userId)) {
            throw new InsufficientPermissionsException("You can only change your own password");
        }
        
        User user = getUserByIdOrThrow(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", userId);
    }

    // Helper methods
    private UserResponse mapToUserResponse(User user) {
        return modelMapper.map(user, UserResponse.class);
    }

    private User getUserByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof ApplicationUserDetails)) {
            throw new UserNotFoundException("No authenticated user found");
        }
        ApplicationUserDetails userDetails = (ApplicationUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }
}
