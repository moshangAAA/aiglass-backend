package com.almousleck.service.impl;

import com.almousleck.config.ApplicationUserDetails;
import com.almousleck.dto.user.ChangePasswordRequest;
import com.almousleck.dto.user.UpdateProfileRequest;
import com.almousleck.dto.user.UserResponse;
import com.almousleck.enums.UserRole;
import com.almousleck.enums.UserStatus;
import com.almousleck.exceptions.InsufficientPermissionsException;
import com.almousleck.exceptions.ResourceAlreadyExistsException;
import com.almousleck.exceptions.UserNotFoundException;
import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
import com.almousleck.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SystemLogService systemLogService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private HttpServletRequest request;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserServiceImpl userService;

    private User currentUser;
    private User targetUser;
    private ApplicationUserDetails userDetails;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).username("admin").role(UserRole.ADMIN).status(UserStatus.ACTIVE).build();
        targetUser = User.builder().id(2L).username("user").role(UserRole.USER).status(UserStatus.ACTIVE).passwordHash("oldHash").build();
        userDetails = ApplicationUserDetails.buildApplicationDetails(currentUser);;
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockSecurityContext(User user) {
        ApplicationUserDetails details = ApplicationUserDetails.buildApplicationDetails(user);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(details);
        //SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getMyProfile_ShouldSuccess_WhenAccessingOwnProfile() {
        mockSecurityContext(currentUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        UserResponse mockResponse = UserResponse.builder()
                .id(1L)
                .username("admin")
                .build();
        when(modelMapper.map(any(), eq(UserResponse.class))).thenReturn(mockResponse);

        UserResponse result = userService.getMyProfile(1L);

        assertNotNull(result);
        verify(userRepository).findById(1L);
    }

    @Test
    void getMyProfile_ShouldThrow_WhenAccessingOtherProfile() {
        mockSecurityContext(currentUser); // Logged in as ID 1

        assertThrows(InsufficientPermissionsException.class, () -> userService.getMyProfile(2L));
    }

    @Test
    void updateStatus_ShouldLogAction_WhenAdminUpdatesUser() {
        mockSecurityContext(currentUser);

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        userService.updateUserStatus(2L, UserStatus.BANNED, "Rules violation");

        verify(userRepository).save(targetUser);
        verify(systemLogService).logAction(eq("USER_STATUS_UPDATED"), eq(currentUser), eq("USER"), eq(2L), any(), any(), eq(request));
        assertEquals(UserStatus.BANNED, targetUser.getStatus());
    }

    @Test
    void changePassword_ShouldSuccess_WhenCurrentPasswordMatches() {
        mockSecurityContext(targetUser); // User 2 is updating self

        ChangePasswordRequest pwReq = new ChangePasswordRequest();
        pwReq.setCurrentPassword("oldPassword");
        pwReq.setNewPassword("newPassword");

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(passwordEncoder.matches("oldPassword", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedHash");

        userService.changePassword(2L, pwReq);

        verify(userRepository).save(targetUser);
        assertEquals("newEncodedHash", targetUser.getPasswordHash());
    }



    @Test
    void updateProfile_ShouldThrow_WhenUsernameExists() {
        mockSecurityContext(targetUser); // User 2 is updating self

        UpdateProfileRequest updateReq = new UpdateProfileRequest();
        updateReq.setUsername("existingUser");

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> userService.updateProfile(2L, updateReq));
    }


    @Test
    void deleteUser_ShouldSoftDeleteAndLog() {
        mockSecurityContext(currentUser);

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        userService.deleteUser(2L);

        assertEquals(UserStatus.BANNED, targetUser.getStatus());
        verify(systemLogService).logAction(eq("USER_DELETED"), any(), any(), any(), any(), any(), any());
    }


    @Test
    void getUserById_ShouldThrow_WhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.getUserById(99L));
    }

}