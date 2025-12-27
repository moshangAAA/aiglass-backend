package com.almousleck.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer认证";

        return new OpenAPI()
                .info(new Info()
                        .title("AI Glass 智能眼镜后端API")
                        .version("v1.0.0")
                        .description("""
                                # AI Glass 智能眼镜系统后端接口文档
                                
                                ## 项目简介
                                为视障人士设计的AI智能眼镜系统后端服务，提供用户认证、设备管理、实时通信等核心功能。
                                
                                ## 主要功能模块
                                - **用户认证模块**: 注册、登录、OTP验证、密码重置
                                - **设备管理模块**: 设备配对、设备列表、心跳检测、设备解绑
                                - **系统管理模块**: 账户解锁等管理功能
                                - **实时通信模块**: WebSocket信号传输
                                
                                ## 技术栈
                                - Spring Boot 3.5.8
                                - Spring Security + JWT
                                - Redis (缓存 + 限流)
                                - MySQL 8.0
                                - WebSocket (实时通信)
                                
                                ## 认证说明
                                除公开接口外，所有接口需要在请求头中携带JWT令牌：
                                ```
                                Authorization: Bearer <your_jwt_token>
                                ```
                                
                                ## 限流说明
                                API限流: 100请求/分钟/IP
                                OTP限流: 1次/分钟/用户
                                登录失败: 5次后锁定30分钟
                                """)
                        .contact(new Contact()
                                .name("Lento Team")
                                .email("support@lento.com")
                                .url("https://github.com/lento-team"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请在此处输入JWT令牌，格式: Bearer <token>")));
    }
}
