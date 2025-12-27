package com.almousleck.controller;

import com.almousleck.dto.*;
import com.almousleck.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
        name = "1. 用户认证模块",
        description = """
                用户认证与账户管理相关接口
                
                **功能说明:**
                - 用户注册与手机号验证
                - 用户登录与JWT令牌颁发  
                - OTP验证码管理
                - 密码重置流程
                - 令牌刷新与登出
                
                **注意事项:**
                - 本模块所有接口均为公开接口，无需认证
                - 登录失败5次后账户将被锁定30分钟
                - OTP验证码有效期为5分钟
                - OTP请求限流：1次/分钟/用户
                """
)
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(
            summary = "用户注册",
            description = """
                    注册新用户并发送OTP验证码到手机
                    
                    **业务流程:**
                    1. 验证用户名和手机号是否已被注册
                    2. 创建新用户账户（密码加密存储）
                    3. 生成6位数字OTP验证码
                    4. 发送OTP到用户手机号（开发模式下会在响应中返回OTP）
                    
                    **注意事项:**
                    - 用户名必须唯一
                    - 手机号必须唯一且格式正确
                    - 密码长度至少8位
                    - OTP验证码有效期5分钟
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "注册成功，OTP验证码已发送"),
            @ApiResponse(responseCode = "409", description = "用户名或手机号已被注册"),
            @ApiResponse(responseCode = "400", description = "请求参数格式错误")
    })
    @PostMapping("/register")
    public ResponseEntity<OtpResponse> register(@Valid @RequestBody RegisterRequest request) {
        OtpResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "用户登录",
            description = """
                    使用用户名/手机号和密码进行身份认证
                    
                    **业务流程:**
                    1. 验证账户是否被锁定
                    2. 验证用户名/手机号和密码
                    3. 检查手机号是否已验证
                    4. 清除旧的刷新令牌（令牌轮换策略）
                    5. 生成新的访问令牌(JWT)和刷新令牌
                    
                    **安全机制:**
                    - 登录失败5次后账户锁定30分钟
                    - 第4次失败时发送警告短信
                    - 第5次失败时发送锁定通知短信
                    - JWT令牌有效期: 24小时
                    - 刷新令牌有效期: 7天
                    
                    **返回数据:**
                    - token: JWT访问令牌（用于API请求认证）
                    - refreshToken: 刷新令牌（用于获取新的访问令牌）
                    - username: 用户名
                    - role: 用户角色(USER/ADMIN)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功，返回JWT令牌"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "423", description = "账户已被锁定，请稍后再试"),
            @ApiResponse(responseCode = "403", description = "手机号未验证，请先验证手机号")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @Operation(
            summary = "验证OTP验证码",
            description = """
                    验证手机号对应的OTP验证码
                    
                    **使用场景:**
                    - 注册后验证手机号
                    - 激活新注册的账户
                    
                    **业务流程:**
                    1. 查找对应手机号的用户
                    2. 验证OTP码是否正确
                    3. 检查OTP是否过期（5分钟有效期）
                    4. 标记手机号为已验证状态
                    5. 清除已使用的OTP码
                    6. 发送验证成功通知
                    
                    **注意事项:**
                    - OTP验证码区分大小写
                    - 验证码有效期为5分钟
                    - 验证成功后OTP自动失效
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "验证成功，手机号已激活"),
            @ApiResponse(responseCode = "400", description = "验证码错误或已过期"),
            @ApiResponse(responseCode = "404", description = "手机号未注册")
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        authenticationService.verifyOtp(request.getPhoneNumber(), request.getOtpCode());
        return ResponseEntity.ok(Map.of("message", "手机号验证成功"));
    }

    @Operation(
            summary = "重新发送OTP验证码",
            description = """
                    重新发送OTP验证码到指定手机号
                    
                    **使用场景:**
                    - 用户未收到验证码
                    - 验证码已过期
                    
                    **业务流程:**
                    1. 查找对应手机号的用户
                    2. 检查OTP请求频率（限流：1次/分钟）
                    3. 生成新的6位数字OTP验证码
                    4. 发送OTP到用户手机号
                    
                    **限流说明:**
                    - 同一用户1分钟内只能请求1次
                    - 超出限制返回429状态码
                    - 响应头包含Retry-After字段
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "验证码已重新发送"),
            @ApiResponse(responseCode = "429", description = "请求过于频繁，请稍后再试"),
            @ApiResponse(responseCode = "404", description = "手机号未注册")
    })
    @PostMapping("/resend-otp")
    public ResponseEntity<OtpResponse> resendOtp(@Valid @RequestBody OtpRequest request) {
        OtpResponse response = authenticationService.resendOtp(request.getPhoneNumber());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "忘记密码",
            description = """
                    发起密码重置流程，发送OTP验证码到注册手机号
                    
                    **业务流程:**
                    1. 验证手机号是否已注册
                    2. 检查OTP请求频率（限流：1次/分钟）
                    3. 生成新的6位数字OTP验证码
                    4. 发送密码重置OTP到用户手机号
                    
                    **安全说明:**
                    - OTP验证码有效期5分钟
                    - 限流：1次/分钟/用户
                    - 验证码用于下一步重置密码
                    
                    **下一步操作:**
                    收到验证码后，调用 `/reset-password` 接口完成密码重置
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "密码重置验证码已发送"),
            @ApiResponse(responseCode = "429", description = "请求过于频繁，请稍后再试"),
            @ApiResponse(responseCode = "404", description = "手机号未注册")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<OtpResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        OtpResponse response = authenticationService.forgotPassword(request.getPhoneNumber());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "重置密码",
            description = """
                    使用OTP验证码重置账户密码
                    
                    **业务流程:**
                    1. 验证手机号是否已注册
                    2. 验证OTP验证码是否正确
                    3. 检查OTP是否过期（5分钟有效期）
                    4. 更新用户密码（加密存储）
                    5. 清除已使用的OTP码
                    6. 发送密码重置成功通知
                    
                    **前置条件:**
                    - 必须先调用 `/forgot-password` 获取OTP验证码
                    
                    **密码要求:**
                    - 最少8个字符
                    - 建议包含大小写字母、数字和特殊字符
                    
                    **注意事项:**
                    - 密码重置成功后需要使用新密码重新登录
                    - OTP验证码一次性使用，验证后自动失效
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "密码重置成功，请使用新密码登录"),
            @ApiResponse(responseCode = "400", description = "验证码错误或已过期"),
            @ApiResponse(responseCode = "404", description = "手机号未注册")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(
                request.getPhoneNumber(),
                request.getOtpCode(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(Map.of("message", "密码重置成功，请使用新密码登录"));
    }

    @Operation(
            summary = "刷新访问令牌",
            description = """
                    使用刷新令牌获取新的访问令牌
                    
                    **使用场景:**
                    - 访问令牌(JWT)即将过期
                    - 访问令牌已过期但刷新令牌仍有效
                    
                    **业务流程:**
                    1. 验证刷新令牌是否有效
                    2. 检查刷新令牌是否过期（7天有效期）
                    3. 生成新的访问令牌
                    4. 返回新的访问令牌
                    
                    **令牌说明:**
                    - 访问令牌有效期: 24小时
                    - 刷新令牌有效期: 7天
                    - 刷新令牌过期后需要重新登录
                    
                    **安全机制:**
                    - 令牌轮换：每次登录清除旧的刷新令牌
                    - 刷新令牌一次性使用
                    - 过期的刷新令牌自动删除
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "令牌刷新成功，返回新的访问令牌"),
            @ApiResponse(responseCode = "403", description = "刷新令牌无效或已过期，请重新登录")
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "用户登出",
            description = """
                    登出当前用户，使访问令牌和刷新令牌失效
                    
                    **业务流程:**
                    1. 从请求头获取访问令牌(JWT)
                    2. 从请求体获取刷新令牌
                    3. 将访问令牌加入黑名单（Redis，TTL=剩余有效期）
                    4. 删除数据库中的刷新令牌
                    
                    **请求格式:**
                    - Header: `Authorization: Bearer <access_token>`
                    - Body: `{ "refreshToken": "<refresh_token>" }`
                    
                    **安全说明:**
                    - 登出后访问令牌立即失效
                    - 黑名单令牌无法用于任何API请求
                    - 刷新令牌永久删除，无法恢复
                    
                    **注意事项:**
                    - 登出后需要重新登录获取新令牌
                    - 建议前端清除本地存储的所有令牌
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登出成功，令牌已失效"),
            @ApiResponse(responseCode = "400", description = "缺少访问令牌或刷新令牌")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, @RequestBody Map<String, String> body) {
        // Get access token from Header
        String authHeader = request.getHeader("Authorization");
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer "))
            accessToken = authHeader.substring(7);

        // Get refresh token from Body
        String refreshToken = body.get("refreshToken");

        if (accessToken != null && refreshToken != null) {
            authenticationService.logout(accessToken, refreshToken);
            return ResponseEntity.ok(Map.of("message", "登出成功"));
        }

        return ResponseEntity.badRequest()
                .body(Map.of("message", "缺少访问令牌或刷新令牌"));
    }

}
