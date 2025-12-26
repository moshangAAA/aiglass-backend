package com.almousleck.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // Rate limiting is now handled by RateLimitFilter
    // which runs BEFORE Spring MVC interceptors and works with Actuator endpoints
}
