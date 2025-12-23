package com.almousleck.config.security;

import com.almousleck.config.ApplicationUserDetailsService;
import com.almousleck.jwt.AuthenticationTokenFilter;
import com.almousleck.jwt.JwtUtils;
import com.almousleck.service.TokenBlacklistService;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ApplicationSecurityConfiguration {

    @Bean
    public AuthenticationTokenFilter authenticationTokenFilter(JwtUtils jwtUtils,
                                                               ApplicationUserDetailsService detailsService,
                                                               TokenBlacklistService tokenBlacklistService) {
        return new AuthenticationTokenFilter(jwtUtils, detailsService, tokenBlacklistService);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(ApplicationUserDetailsService applicationUserDetailsService) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(applicationUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public PasswordEncoder  passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

}
