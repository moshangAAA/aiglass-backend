package com.almousleck.service;

import com.almousleck.dto.user.ChangePasswordRequest;
import com.almousleck.dto.user.UpdateProfileRequest;
import com.almousleck.dto.user.UserResponse;
import com.almousleck.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    //ADMIN
    Page<UserResponse> getAllUsers(UserStatus status, Pageable pageable);
    UserResponse getUserById(Long userId);
    UserResponse updateUserStatus(Long userId, UserStatus status, String reason);
    void deleteUser(Long userId);

    UserResponse getMyProfile(Long userId);
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}
