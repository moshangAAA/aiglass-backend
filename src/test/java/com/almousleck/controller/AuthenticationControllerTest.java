package com.almousleck.controller;

import com.almousleck.dto.AuthResponse;
import com.almousleck.dto.LoginRequest;
import com.almousleck.dto.RegisterRequest;
import com.almousleck.enums.UserRole;
import com.almousleck.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.almousleck.exceptions.ResourceAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false) // 禁用 Spring Security 过滤器
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AuthenticationService authenticationService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_ShouldReturnCreated_WhenRequestIsValid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPhoneNumber("123456789");
        request.setPassword("password");

        // Send POST-Request
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("用户注册成功"));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        // 测试空用户名
        RegisterRequest request = new RegisterRequest();
        request.setUsername(""); // 空用户名
        request.setPhoneNumber("123456789");
        request.setPassword("password");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // 验证返回状态码为 400 Bad Request
    }

    @Test
    void register_ShouldReturnConflict_WhenUserExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existingUser");
        request.setPhoneNumber("123456789");
        request.setPassword("password");

        // 测试用户名已存在
        doThrow(new ResourceAlreadyExistsException("用户名已被占用"))
                .when(authenticationService).register(any());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("用户名已被占用"));
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreValid() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("newuser");
        request.setPassword("password");

        // Mock the response
        AuthResponse response = new AuthResponse(
                "fake-jwt-token",
                "newuser",
                UserRole.USER
        );

        when(authenticationService.login(any()))
                .thenReturn(response);

        // Perform and verify
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenCredentialsAreInvalid() throws Exception {
        com.almousleck.dto.LoginRequest request = new com.almousleck.dto.LoginRequest();
        request.setIdentifier("wrongUser");
        request.setPassword("wrongPass");
        // Mock Exception
        doThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"))
                .when(authenticationService).login(any());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()) // HTTP 401 Unauthorized
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }












}