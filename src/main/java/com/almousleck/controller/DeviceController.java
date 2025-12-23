package com.almousleck.controller;

import com.almousleck.config.ApplicationUserDetails;
import com.almousleck.dto.device.DevicePairRequest;
import com.almousleck.dto.device.DeviceResponse;
import com.almousleck.service.DeviceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    @PostMapping("/pair")
    public ResponseEntity<DeviceResponse> pairDevice(
            @Valid @RequestBody DevicePairRequest request,
            @AuthenticationPrincipal ApplicationUserDetails details) {
        DeviceResponse response = deviceService.pairDevice(request, details.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<DeviceResponse>> getMyDevices(
            @AuthenticationPrincipal ApplicationUserDetails userDetails) {
        return ResponseEntity.ok(deviceService.getMyDevices(userDetails.getId()));
    }

    /**
     * Heartbeat Endpoint (The Pulse 💓)
     * - Uses Redis for "Online" presence (fast).
     * - Updates DB only when battery changes or for audit.
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @RequestParam String serialNumber,
            @RequestParam(required = false) Integer batteryLevel,
            HttpServletRequest request) {

        // Auto-capture IP from the request header/remote address
        String clientIp = request.getRemoteAddr();

        deviceService.updateHeartbeat(serialNumber, batteryLevel, clientIp);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{serialNumber}")
    public ResponseEntity<Void> unpairDevice(
            @PathVariable String serialNumber,
            @AuthenticationPrincipal ApplicationUserDetails userDetails) {
        deviceService.unpairDevice(serialNumber, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
