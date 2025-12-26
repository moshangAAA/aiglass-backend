package com.almousleck.service;

import com.almousleck.dto.device.DevicePairRequest;
import com.almousleck.dto.device.DeviceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DeviceService {
    DeviceResponse pairDevice(DevicePairRequest request, Long userId);
    Page<DeviceResponse> getMyDevices(Long userId, Pageable pageable);
    void updateHeartbeat(String serialNumber, Integer batteryLevel, String ipAddress);
    void updateFirmwareVersion(String serialNumber, String firmwareVersion);
    void markDeviceOnline(String serialNumber);
    void markDeviceOffline(String serialNumber);
    void unpairDevice(String serialNumber, Long userId);
}
