package com.almousleck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "需要用户名")
    private String username;

    @NotBlank(message = "需要手机号")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "手机号格式错误")
    private String phoneNumber;

    @NotBlank(message = "需要密码")
    @Size(min = 8, message = "密码至少8个字符")
    private String password;
}


