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
        name = "2. 设备管理模块",
        description = """
                AI智能眼镜设备管理相关接口
                
                **功能说明:**
                - 设备配对与绑定
                - 设备列表查询（分页）
                - 设备心跳检测（在线状态）
                - 设备解绑
                
                **认证要求:**
                - 除心跳接口外，其他接口均需JWT认证
                - 用户只能操作自己的设备
                
                **在线状态机制:**
                - 使用Redis存储设备在线状态（高性能）
                - 心跳超时时间: 60秒
                - 设备离线自动标记
                """
)
public class DeviceController {
    private final DeviceService deviceService;

    @Operation(
            summary = "设备配对",
            description = """
                    将AI智能眼镜设备绑定到当前用户账户
                    
                    **业务流程:**
                    1. 验证设备序列号格式
                    2. 检查设备是否已被其他用户绑定
                    3. 创建设备记录并关联到当前用户
                    4. 初始化设备在线状态（Redis）
                    
                    **设备信息:**
                    - serialNumber: 设备唯一序列号
                    - model: 设备型号（可选）
                    - deviceName: 设备昵称（可选，默认为"我的AI眼镜"）
                    
                    **注意事项:**
                    - 一个设备只能绑定到一个用户
                    - 设备序列号必须唯一
                    - 配对成功后设备立即可用
                    
                    **权限要求:**
                    需要JWT认证，用户角色: USER/ADMIN
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "设备配对成功"),
            @ApiResponse(responseCode = "409", description = "设备已被其他用户绑定"),
            @ApiResponse(responseCode = "400", description = "设备序列号格式错误"),
            @ApiResponse(responseCode = "401", description = "未认证，请先登录")
    })
    @PostMapping("/pair")
    public ResponseEntity<DeviceResponse> pairDevice(
            @Valid @RequestBody DevicePairRequest request,
            @AuthenticationPrincipal ApplicationUserDetails details) {
        DeviceResponse response = deviceService.pairDevice(request, details.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "查询我的设备列表",
            description = """
                    查询当前用户绑定的所有AI智能眼镜设备（支持分页）
                    
                    **返回信息:**
                    - 设备序列号
                    - 设备型号
                    - 设备昵称
                    - 在线状态（online/offline）
                    - 电池电量
                    - 最后活跃时间
                    - 绑定时间
                    
                    **分页参数:**
                    - page: 页码（从0开始）
                    - size: 每页数量（默认20，最大100）
                    - sort: 排序字段（默认按创建时间倒序）
                    
                    **在线状态判断:**
                    - 60秒内有心跳 → online
                    - 超过60秒无心跳 → offline
                    
                    **使用示例:**
                    ```
                    GET /api/v1/devices/my?page=0&size=10&sort=createdAt,desc
                    ```
                    
                    **权限要求:**
                    需要JWT认证，用户角色: USER/ADMIN
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功，返回设备列表"),
            @ApiResponse(responseCode = "401", description = "未认证，请先登录")
    })
    @GetMapping("/my")
    public ResponseEntity<Page<DeviceResponse>> getMyDevices(
            @AuthenticationPrincipal ApplicationUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(deviceService.getMyDevices(userDetails.getId(), pageable));
    }

    @Operation(
            summary = "设备心跳",
            description = """
                    设备定期发送心跳信号，用于维持在线状态和更新设备信息
                    
                    **业务流程:**
                    1. 接收设备心跳请求
                    2. 更新Redis中的设备在线状态（TTL=60秒）
                    3. 如果电量有变化，更新数据库中的电量信息
                    4. 记录设备IP地址（用于地理位置分析）
                    
                    **心跳策略:**
                    - 建议频率: 30秒/次
                    - 超时时间: 60秒
                    - 使用Redis存储（高性能，自动过期）
                    
                    **参数说明:**
                    - serialNumber: 设备序列号（必填）
                    - batteryLevel: 电池电量 0-100（可选）
                    - clientIp: 自动从请求中获取
                    
                    **性能优化:**
                    - 使用Redis缓存，避免频繁写入数据库
                    - 仅在电量变化时更新数据库
                    - 异步处理，不阻塞设备请求
                    
                    **注意事项:**
                    - 此接口为公开接口，无需JWT认证
                    - 超过60秒无心跳，设备自动标记为离线
                    - 设备必须先完成配对才能发送心跳
                    
                    **使用示例:**
                    ```
                    POST /api/v1/devices/heartbeat?serialNumber=ABC123456&batteryLevel=85
                    ```
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "心跳接收成功"),
            @ApiResponse(responseCode = "404", description = "设备未找到或未配对"),
            @ApiResponse(responseCode = "400", description = "参数格式错误")
    })
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

    @Operation(
            summary = "解绑设备",
            description = """
                    解除AI智能眼镜设备与当前用户的绑定关系
                    
                    **业务流程:**
                    1. 验证设备所有权
                    2. 从数据库删除设备记录
                    3. 清除Redis中的在线状态
                    4. 清除设备相关缓存
                    
                    **安全验证:**
                    - 只有设备所有者才能解绑设备
                    - 管理员可以强制解绑任何设备
                    
                    **注意事项:**
                    - 解绑后设备可被其他用户重新配对
                    - 解绑操作不可恢复
                    - 设备上的数据不会被清除（需设备端主动清除）
                    
                    **建议操作:**
                    解绑前建议先在设备端执行恢复出厂设置
                    
                    **权限要求:**
                    需要JWT认证，用户角色: USER/ADMIN
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "设备解绑成功"),
            @ApiResponse(responseCode = "404", description = "设备未找到"),
            @ApiResponse(responseCode = "403", description = "无权操作此设备"),
            @ApiResponse(responseCode = "401", description = "未认证，请先登录")
    })
    @DeleteMapping("/{serialNumber}")
    public ResponseEntity<Void> unpairDevice(
            @PathVariable String serialNumber,
            @AuthenticationPrincipal ApplicationUserDetails userDetails) {
        deviceService.unpairDevice(serialNumber, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
