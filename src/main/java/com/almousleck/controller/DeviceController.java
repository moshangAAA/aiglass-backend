package com.almousleck.controller;

import com.almousleck.config.ApplicationUserDetails;
import com.almousleck.dto.device.DevicePairRequest;
import com.almousleck.dto.device.DeviceResponse;
import com.almousleck.service.DeviceService;
import com.almousleck.util.HttpRequestUtil;
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

@RestController
@RequiredArgsConstructor
@Tag(name = "设备管理", description = "设备配对、查询、心跳、解绑")
@RequestMapping("/api/v1/devices")
public class DeviceController {
    private final DeviceService deviceService;

    @Operation(summary = "设备配对")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "配对成功"),
            @ApiResponse(responseCode = "409", description = "设备已被绑定"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @PostMapping("/pair")
    public ResponseEntity<DeviceResponse> pairDevice(
            @Valid @RequestBody DevicePairRequest request,
            @AuthenticationPrincipal ApplicationUserDetails details) {
        DeviceResponse response = deviceService.pairDevice(request, details.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "查询我的设备列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping("/my")
    public ResponseEntity<Page<DeviceResponse>> getMyDevices(
            @AuthenticationPrincipal ApplicationUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(deviceService.getMyDevices(userDetails.getId(), pageable));
    }

    @Operation(summary = "设备心跳")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "心跳成功"),
            @ApiResponse(responseCode = "404", description = "设备未找到"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @RequestParam String serialNumber,
            @RequestParam(required = false) Integer batteryLevel,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            HttpServletRequest request) {
        //
        if (deviceToken == null || deviceToken.isBlank()) {
            return ResponseEntity.status(403).build();
        }
        // Auto-capture IP from the request header/remote address
        String clientIp = HttpRequestUtil.getClientIp(request);
        deviceService.updateHeartbeat(serialNumber, batteryLevel, clientIp);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "解绑设备")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "解绑成功"),
            @ApiResponse(responseCode = "404", description = "设备未找到"),
            @ApiResponse(responseCode = "403", description = "无权操作"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @DeleteMapping("/{serialNumber}")
    public ResponseEntity<Void> unpairDevice(
            @PathVariable String serialNumber,
            @AuthenticationPrincipal ApplicationUserDetails userDetails) {
        deviceService.unpairDevice(serialNumber, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
