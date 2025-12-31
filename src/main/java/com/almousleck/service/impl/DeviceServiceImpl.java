package com.almousleck.service.impl;

import com.almousleck.dto.device.DevicePairRequest;
import com.almousleck.dto.device.DeviceResponse;
import com.almousleck.enums.DeviceStatus;
import com.almousleck.exceptions.DuplicationException;
import com.almousleck.exceptions.ResourceNotFoundException;
import com.almousleck.exceptions.UnauthorizedDeviceAccessException;
import com.almousleck.exceptions.UserNotFoundException;
import com.almousleck.model.Device;
import com.almousleck.model.User;
import com.almousleck.repository.UserRepository;
import com.almousleck.repository.device.DeviceRepository;
import com.almousleck.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String DEVICE_ONLINE_KEY_PREFIX = "device:online:";
    private static final long HEARTBEAT_TTL_SECONDS = 60;// Device considered offline if no pulse for the next 60s



    @Override
    public DeviceResponse pairDevice(DevicePairRequest request, Long userId) {
        User user = getUserByIdOrThrow(userId);
        Device device = deviceRepository.findBySerialNumber(request.getSerialNumber())
                .map(existingDevice -> {
                    if (existingDevice.getOwner() != null) {
                        throw new DuplicationException("Device is already owned by another user");
                    }
                    return existingDevice;
                })
                .orElseGet(() -> createNewDevice(request));
        // Pairing logic: Bind to user, but the device is OFFLINE until it actually connects via WebSocket
        device.setOwner(user);
        device.setName(request.getDeviceName());
        device.setStatus(DeviceStatus.OFFLINE); // Production Best Practice: Pairing != Online

        return mapToResponseWithRealTimeStatus(deviceRepository.save(device));
    }

    @Override
    public Page<DeviceResponse> getMyDevices(Long userId, Pageable pageable) {
        User user = getUserByIdOrThrow(userId);

        return deviceRepository.findByOwner(user, pageable)
                .map(this::mapToResponseWithRealTimeStatus);
    }

    @Override
    public void updateHeartbeat(String serialNumber, Integer batteryLevel, String ipAddress) {
        // 1. Redis: Extend "ONLINE" presence (The Pulse)
        String redisKey = DEVICE_ONLINE_KEY_PREFIX + serialNumber;
        stringRedisTemplate.opsForValue().set(redisKey, "1", HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);

        // 2. Database: Update 'Hard State' (Audit trails and battery)
        // Optimization: In high-scale, I might only write to DB if battery changes > 5%
        // For now, update strictly necessary fields
        Device device = getDeviceBySerial(serialNumber);

        if (batteryLevel != null)
            device.setBatteryLevel(batteryLevel);

        if (ipAddress != null)
            device.setIpAddress(ipAddress);

        // Always update last_heartbeat for audit
        device.setLastHeartbeat(Instant.now());

        deviceRepository.save(device);
    }

    @Override
    @Transactional
    public void updateFirmwareVersion(String serialNumber, String firmwareVersion) {
        Device device = getDeviceBySerial(serialNumber);
        device.setFirmwareVersion(firmwareVersion);
        deviceRepository.save(device);
        log.info("Firmware updated for device {}: {}", serialNumber, firmwareVersion);
    }

    @Override
    @Transactional
    public void markDeviceOnline(String serialNumber) {
        // Called when WebSocket connects
        String redisKey = DEVICE_ONLINE_KEY_PREFIX + serialNumber;
        stringRedisTemplate.opsForValue().set(redisKey, "1", HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);

        // Update DB Connection History
        Device device = getDeviceBySerial(serialNumber);
        device.setStatus(DeviceStatus.ONLINE);
        device.setConnectTime(Instant.now());
        deviceRepository.save(device);
        log.info("Device connected (Online): {}", serialNumber);
    }

    @Override
    @Transactional
    public void markDeviceOffline(String serialNumber) {
        // Called when WebSocket disconnects
        String redisKey = DEVICE_ONLINE_KEY_PREFIX + serialNumber;
        stringRedisTemplate.delete(redisKey);

        Device device = getDeviceBySerial(serialNumber);
        device.setStatus(DeviceStatus.OFFLINE);
        deviceRepository.save(device);
        log.info("Device disconnected (Offline): {}", serialNumber);
    }

    @Override
    @Transactional
    public void unpairDevice(String serialNumber, Long userId) {
        Device device = getDeviceBySerial(serialNumber);

        if (device.getOwner() == null || !device.getOwner().getId().equals(userId))
            throw new UnauthorizedDeviceAccessException("Not authorized to unpair this device");

        // Clean up
        String redisKey = DEVICE_ONLINE_KEY_PREFIX + serialNumber;
        stringRedisTemplate.delete(redisKey);

        device.setOwner(null);
        device.setStatus(DeviceStatus.OFFLINE);
        device.setConnectTime(null);
        deviceRepository.save(device);
        log.info("Device unpaired: {}", serialNumber);
    }

    // Helper methods
    private User getUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private Device createNewDevice(DevicePairRequest request) {
        return Device.builder()
                .serialNumber(request.getSerialNumber())
                .name(request.getDeviceName())
                .type("AI-GLASS-V1")
                .status(DeviceStatus.OFFLINE)
                .firmwareVersion("1.0.0")
                .batteryLevel(20)
                .build();
    }

    private Device getDeviceBySerial(String serialNumber) {
        return deviceRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with serial number: [%s] ".formatted(serialNumber)));
    }

    private DeviceResponse mapToResponseWithRealTimeStatus(Device device) {
        String redisKey = DEVICE_ONLINE_KEY_PREFIX + device.getSerialNumber();
        boolean isOnlineInRedis = stringRedisTemplate.hasKey(redisKey);

        return DeviceResponse.builder()
                .id(device.getId())
                .name(device.getName())
                .serialNumber(device.getSerialNumber())
                .status(isOnlineInRedis ? DeviceStatus.ONLINE : DeviceStatus.OFFLINE)
                .batteryLevel(device.getBatteryLevel())
                .firmwareVersion(device.getFirmwareVersion())
                .ipAddress(device.getIpAddress())
                .lastHeartbeat(device.getLastHeartbeat())
                .connectTime(device.getConnectTime())
                .build();
    }
}
