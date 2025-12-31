package com.almousleck.controller;

import com.almousleck.config.ApplicationUserDetails;
import com.almousleck.config.ratelimit.RateLimitFilter;
import com.almousleck.dto.device.DevicePairRequest;
import com.almousleck.dto.device.DeviceResponse;
import com.almousleck.enums.DeviceStatus;
import com.almousleck.enums.UserRole;
import com.almousleck.model.User;
import com.almousleck.service.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DeviceController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.almousleck.config.WebConfig.class),
        excludeAutoConfiguration = RedisAutoConfiguration.class
)
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceService deviceService;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private ApplicationUserDetails principal;
    private DeviceResponse deviceResponse;

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

        // 1. Mock parameters: 1L
        when(deviceService.pairDevice(any(DevicePairRequest.class), eq(1L)))
                .thenReturn(mockResponse);

        // Prep User and Auth
        User mockUser = User.builder()
                .username("testuser")
                .role(UserRole.USER)
                .phoneNumber("+1234567890")
                .phoneVerified(true)
                .locked(false)
                .build();
        mockUser.setId(1L);

  ApplicationUserDetails principal = ApplicationUserDetails.buildApplicationDetails(mockUser);

        // 2. Mock Security Context properly
        mockMvc.perform(post("/api/v1/devices/pair")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.user(principal))
                        .with(csrf()))
                .andDo(print()) // This will print the response
                .andExpect(status().isOk());
               // .andExpect(jsonPath("$.serialNumber").value("GLASS-001"));
    }

    @Test
    void getMyDevices_ShouldReturnList() throws Exception {
        // Prep User
        User mockUser = User.builder()
                .username("testuser")
                .role(com.almousleck.enums.UserRole.USER)
                .phoneNumber("+1234567890")
                .phoneVerified(true)
                .locked(false)
                .build();
        mockUser.setId(1L);

        var principal = ApplicationUserDetails.buildApplicationDetails(mockUser);

        // Mock the service to return empty page
        when(deviceService.getMyDevices(eq(1L), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/v1/devices/my")
                        .with(SecurityMockMvcRequestPostProcessors.user(principal)))
                .andExpect(status().isOk());
    }
}