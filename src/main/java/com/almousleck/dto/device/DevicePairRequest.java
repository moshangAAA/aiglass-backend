package com.almousleck.dto.device;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DevicePairRequest {
    @NotBlank(message = "Serial number is required")
    private String serialNumber;

    @NotBlank(message = "Please give your device a name")
    private String deviceName;
}
