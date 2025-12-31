package com.almousleck.controller;

import com.almousleck.dto.user.UpdateUserStatusRequest;
import com.almousleck.dto.user.UserResponse;
import com.almousleck.enums.UserStatus;
import com.almousleck.model.SystemLog;
import com.almousleck.service.LoginAttemptService;
import com.almousleck.service.SystemLogService;
import com.almousleck.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "系统管理", description = "管理员功能（需ADMIN权限）")
public class AdminController {

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;
    private final SystemLogService systemLogService;

    @Operation(summary = "解锁用户账户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "解锁成功"),
            @ApiResponse(responseCode = "404", description = "用户未找到"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @PostMapping("/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> unlockUserAccount(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        loginAttemptService.unlockAccount(identifier);
        return ResponseEntity.ok("账户解锁成功");
    }

    @Operation(summary = "获取用户列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20, sort = "created") Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getAllUsers(status, pageable));
    }

    @Operation(summary = "获取用户详情")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "用户未找到"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "更新用户状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "用户未找到"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserStatus(id, request.getStatus(), request.getReason()));
    }

    @Operation(summary = "删除用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "用户未找到"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("用户删除成功");
    }

    @Operation(summary = "查看系统日志")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SystemLog>> getSystemLogs(
            @PageableDefault(size = 20, sort = "created") Pageable pageable
    ) {
        return ResponseEntity.ok(systemLogService.getLogs(pageable));
    }

    @Operation(summary = "按用户查询日志")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping("/logs/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SystemLog>> getLogsByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "created") Pageable pageable) {
        return ResponseEntity.ok(systemLogService.getLogsByUser(userId, pageable));
    }

    @Operation(summary = "按操作类型查询日志")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping("/logs/action/{action}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SystemLog>> getLogsByAction(
            @PathVariable String action,
            @PageableDefault(size = 20, sort = "created") Pageable pageable) {
        return ResponseEntity.ok(systemLogService.getLogsByAction(action, pageable));
    }

    @Operation(summary = "按资源查询日志")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping("/logs/resource")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SystemLog>> getLogsByResource(
            @RequestParam String resourceType,
            @RequestParam Long resourceId,
            @PageableDefault(size = 20, sort = "created") Pageable pageable
    ) {
        return ResponseEntity.ok(systemLogService.getLogsByResource(resourceType, resourceId, pageable));
    }

    @Operation(summary = "按日期范围查询日志")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping("/logs/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SystemLog>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @PageableDefault(size = 20, sort = "created") Pageable pageable
    ) {
        return ResponseEntity.ok(systemLogService.getLogsByDateRange(start, end, pageable));
    }



}
