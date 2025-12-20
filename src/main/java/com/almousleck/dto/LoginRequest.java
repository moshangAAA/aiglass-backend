package com.almousleck.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "用户名或手机号不能为空")
    private String identifier;

    @NotBlank(message = "需要密码")
    private String password;
}


