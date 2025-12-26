package com.almousleck.controller;

import com.almousleck.config.ApplicationUserDetails;
import com.almousleck.dto.device.DevicePairRequest;
import com.almousleck.dto.device.DeviceResponse;
import com.almousleck.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(
        name = "Devices Controller",
        description = "Device pairing, retrieval, and management endpoints"
)
public class DeviceController {
    private final DeviceService deviceService;

    @PostMapping("/pair")
    public ResponseEntity<DeviceResponse> pairDevice(
            @Valid @RequestBody DevicePairRequest request,
            @AuthenticationPrincipal ApplicationUserDetails details) {
        DeviceResponse response = deviceService.pairDevice(request, details.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get my devices", description = "Retrieve all devices paired to the current user")
    @GetMapping("/my")
    public ResponseEntity<Page<DeviceResponse>> getMyDevices(
            @AuthenticationPrincipal ApplicationUserDetails userDetails,
            @PageableDefault(size = 20, sort = "created") Pageable pageable
    ) {
        return ResponseEntity.ok(deviceService.getMyDevices(userDetails.getId(), pageable));
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
