package com.almousleck.controller;

import com.almousleck.service.LoginAttemptService;
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
public class AdminController {
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> unlockUserAccount(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        loginAttemptService.unlockAccount(identifier);
        return ResponseEntity.ok("Account unlocked successfully");
    }
}
