package com.almousleck.service;

import com.almousleck.dto.device.DevicePairRequest;
import com.almousleck.dto.device.DeviceResponse;

import java.util.List;

public interface DeviceService {
    DeviceResponse pairDevice(DevicePairRequest request, Long userId);
    List<DeviceResponse> getMyDevices(Long userId);
    void updateHeartbeat(String serialNumber, Integer batteryLevel, String ipAddress);
    void updateFirmwareVersion(String serialNumber, String firmwareVersion);
    void markDeviceOnline(String serialNumber);
    void markDeviceOffline(String serialNumber);
    void unpairDevice(String serialNumber, Long userId);
}
