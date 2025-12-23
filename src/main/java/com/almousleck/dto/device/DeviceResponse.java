package com.almousleck.dto.device;

import com.almousleck.enums.DeviceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DeviceResponse {
    private Long id;
    private String name;
    private String serialNumber;
    private DeviceStatus status;
    private Integer batteryLevel;
    private String firmwareVersion;
    private Instant lastHeartbeat;
    private String ipAddress;
    private Instant connectTime;
}
