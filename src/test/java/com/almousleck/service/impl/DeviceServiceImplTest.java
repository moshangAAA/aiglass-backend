package com.almousleck.service.impl;

import com.almousleck.dto.device.DevicePairRequest;
import com.almousleck.dto.device.DeviceResponse;
import com.almousleck.enums.DeviceStatus;
import com.almousleck.enums.UserRole;
import com.almousleck.exceptions.DuplicationException;
import com.almousleck.exceptions.UnauthorizedDeviceAccessException;
import com.almousleck.model.Device;
import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
import com.almousleck.repository.device.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    private User user;
    private Device device;
    private DevicePairRequest pairRequest;

    @BeforeEach
    void setUp() {
        // 1. Setup User
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(UserRole.USER);

        // 2. Setup Device
        device = new Device();
        device.setId(1L);
        device.setSerialNumber("GLASS-001");
        device.setStatus(DeviceStatus.OFFLINE);
        device.setFirmwareVersion("1.0.0");
        device.setBatteryLevel(20);
        device.setName("My Smart Glass");
        device.setOwner(user);

        // 3. Setup Request
        pairRequest = new DevicePairRequest();
        pairRequest.setSerialNumber("GLASS-001");
        pairRequest.setDeviceName("My Smart Glass");

        // 4. Mock Redis
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void pairDevice_ShouldSuccess_WhenDeviceIsNew() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(deviceRepository.findBySerialNumber("GLASS-001")).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(device);

        // Act
        DeviceResponse response = deviceService.pairDevice(pairRequest, 1L);

        // Assert
        assertNotNull(response);
        assertEquals("GLASS-001", response.getSerialNumber());
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void pairDevice_ShouldThrow_WhenDeviceAlreadyOwned() {
        // Arrange
        Device existingDevice = new Device();
        existingDevice.setSerialNumber("GLASS-001");
        existingDevice.setOwner(new User()); // Owned by someone else

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(deviceRepository.findBySerialNumber("GLASS-001")).thenReturn(Optional.of(existingDevice));

        // Act & Assert
        assertThrows(DuplicationException.class, 
            () -> deviceService.pairDevice(pairRequest, 1L));
        
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void updateHeartbeat_ShouldUpdateRedis() {
        // Arrange
        Device mockDevice = new Device();
        mockDevice.setSerialNumber("GLASS-001");
        mockDevice.setBatteryLevel(80);
        
        when(deviceRepository.findBySerialNumber("GLASS-001")).thenReturn(Optional.of(mockDevice));

        // Act
        deviceService.updateHeartbeat("GLASS-001", 95, "127.0.0.1");

        // Assert
        // Should set key "device:online:GLASS-001"
        verify(valueOperations).set(eq("device:online:GLASS-001"), eq("1"), anyLong(), any());
        verify(deviceRepository).save(any(Device.class)); // Should save because battery changed 80->95
    }

    @Test
    void unpairDevice_ShouldThrow_WhenUserIsNotOwner() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(99L);
        device.setOwner(anotherUser);

        when(deviceRepository.findBySerialNumber("GLASS-001"))
                .thenReturn(Optional.of(device));

        // Act and Assert
        assertThrows(UnauthorizedDeviceAccessException.class, () -> deviceService.unpairDevice("GLASS-001", 1L));
    }

    @Test
    void updateHeartbeat_ShouldNotSaveToDb_WhenNotFieldsChanged() {
        // Arrange
        device.setBatteryLevel(80);
        device.setIpAddress("192.168.1.1");
        when(deviceRepository.findBySerialNumber("GLASS-001"))
                .thenReturn(Optional.of(device));

        // Act
        deviceService.updateHeartbeat("GLASS-001", 80, "192.168.1.1");

        // Assert
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any());
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void markDeviceOnline_ShouldUpdateStatusAndRedis() {
        // Arrange
        when(deviceRepository.findBySerialNumber("GLASS-001"))
                .thenReturn(Optional.of(device));

        // Act
        deviceService.markDeviceOnline("GLASS-001");

        // Assert
        assertEquals(DeviceStatus.ONLINE, device.getStatus());
        assertNotNull(device.getConnectTime());
        verify(valueOperations).set(eq("device:online:GLASS-001"), eq("1"), anyLong(), eq(TimeUnit.SECONDS));
        verify(deviceRepository).save(device);
    }

    @Test
    void  unpairDevice_ShouldClearOwnerAndRedis() {
        // Arrange
        when(deviceRepository.findBySerialNumber("GLASS-001")).thenReturn(Optional.of(device));

        // Act
        deviceService.unpairDevice("GLASS-001", 1L);

        // Assert
        assertNull(device.getOwner());
        assertEquals(DeviceStatus.OFFLINE, device.getStatus());
        verify(redisTemplate).delete("device:online:GLASS-001");
        verify(deviceRepository).save(device);
    }

    @Test
    void updateHeartbeat_ShouldAlwaysUpdateLastHeartbeat() {
        // Arrange
        Device mockDevice = new Device();
        mockDevice.setSerialNumber("GLASS-001");
        mockDevice.setBatteryLevel(80);
        when(deviceRepository.findBySerialNumber("GLASS-001"))
                .thenReturn(Optional.of(mockDevice));

        // Act
        deviceService.updateHeartbeat("GLASS-001", 80, "127.0.0.1");

        // Assert
        verify(deviceRepository).save(argThat(device ->
                device.getLastHeartbeat() != null && device.getBatteryLevel() == 80
        ));
    }

}
