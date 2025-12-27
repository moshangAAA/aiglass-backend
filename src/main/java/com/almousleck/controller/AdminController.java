package com.almousleck.controller;

import com.almousleck.service.LoginAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(
        name = "3. 系统管理模块",
        description = """
                系统管理相关接口（仅限管理员）
                
                **功能说明:**
                - 用户账户管理
                - 账户解锁
                - 系统配置管理
                
                **权限要求:**
                - 所有接口需要管理员权限(ROLE_ADMIN)
                - 需要JWT认证
                
                **安全说明:**
                - 操作日志会被记录
                - 敏感操作需要二次验证
                """
)
public class AdminController {
    private final LoginAttemptService loginAttemptService;

    @Operation(
            summary = "解锁用户账户",
            description = """
                    管理员手动解锁被系统锁定的用户账户
                    
                    **使用场景:**
                    - 用户登录失败5次后被锁定
                    - 用户要求紧急解锁
                    - 误锁定需要立即恢复
                    
                    **业务流程:**
                    1. 验证管理员权限
                    2. 查找对应用户（通过用户名或手机号）
                    3. 重置登录失败次数为0
                    4. 移除账户锁定状态
                    5. 清除锁定时间记录
                    6. 记录管理员操作日志（TODO）
                    
                    **请求参数:**
                    - identifier: 用户名或手机号
                    
                    **安全机制:**
                    - 仅ADMIN角色可执行
                    - 操作会被审计日志记录
                    - 建议记录解锁原因
                    
                    **注意事项:**
                    - 解锁后用户可以立即登录
                    - 建议通知用户账户已解锁
                    - 如果用户继续输入错误密码，会再次被锁定
                    
                    **使用示例:**
                    ```json
                    {
                      "identifier": "13027207507"
                    }
                    ```
                    
                    **权限要求:**
                    需要JWT认证，用户角色: ADMIN
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "账户解锁成功"),
            @ApiResponse(responseCode = "404", description = "用户未找到"),
            @ApiResponse(responseCode = "403", description = "权限不足，仅限管理员"),
            @ApiResponse(responseCode = "401", description = "未认证，请先登录")
    })
    @PostMapping("/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> unlockUserAccount(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        loginAttemptService.unlockAccount(identifier);
        return ResponseEntity.ok("账户解锁成功");
    }
}
