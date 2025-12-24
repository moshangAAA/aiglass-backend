package com.almousleck.controller;

import com.almousleck.dto.device.DevicePairRequest;
import com.almousleck.dto.device.DeviceResponse;
import com.almousleck.enums.DeviceStatus;
import com.almousleck.service.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceService deviceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pairDevice_ShouldReturnOk_WhenValid() throws Exception {
        DevicePairRequest request = new DevicePairRequest();
        request.setSerialNumber("GLASS-001");
        request.setDeviceName("My RayBan");

        DeviceResponse mockResponse = DeviceResponse.builder()
                .id(1L)
                .name("My RayBan")
                .serialNumber("GLASS-001")
                .status(DeviceStatus.ONLINE)
                .build();

        // 1. Mock parameters: 1L (userId) must match what is in ApplicationUserDetails
        when(deviceService.pairDevice(any(DevicePairRequest.class), eq(1L)))
                .thenReturn(mockResponse);

        // Prep User and Auth
        com.almousleck.model.User mockUser = com.almousleck.model.User.builder()
                .username("testuser")
                .role(com.almousleck.enums.UserRole.USER)
                .build();
        mockUser.setId(1L);
        
        var principal = com.almousleck.config.ApplicationUserDetails.buildApplicationDetails(mockUser);
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        // 2. Mock Security Context properly
        mockMvc.perform(post("/api/v1/devices/pair")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serialNumber").value("GLASS-001"));
    }

    @Test
    void getMyDevices_ShouldReturnList() throws Exception {
        // Prep User
        com.almousleck.model.User mockUser = com.almousleck.model.User.builder()
                .username("testuser")
                .role(com.almousleck.enums.UserRole.USER)
                .build();
        mockUser.setId(1L);
        
        var principal = com.almousleck.config.ApplicationUserDetails.buildApplicationDetails(mockUser);
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        mockMvc.perform(get("/api/v1/devices/my")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk());
    }
}