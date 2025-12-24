package com.almousleck.controller;

import com.almousleck.dto.signal.SignalMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SignalController {
    private final SimpMessagingTemplate messagingTemplate;

    /**
     *INBOUND: Glasses send a signal here
     * Destination: /app/signal
     */
    public SignalMessage processSignal(@Payload SignalMessage message, Authentication authentication) {
        String username = (authentication != null) ?
                authentication.getName() : "未知";
        log.info("Received Signal from [{}]: {} -> {}", username, message.getType(), message.getAction());
        // TODO: In real life, we would send this to the Python/FastAPI AI Core here via gRPC/Redis
        //TODO: 在实际应用中，我们会通过 gRPC/Redis 将此信息发送到 Python/FastAPI AI Core。

        // Echo back with server timestamp (Latency Check)
        message.setSenderId(username);
        message.setTimestamp(Instant.now());

        return message;
    }

    /**
     *SYSTEM BROADCAST: Send data to specific user (e.g., Navigation Instruction)
     * This is meant to be called by internal services (e.g. NavigationService)
     */
    public void sendToUser(String username, SignalMessage message) {
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);
    }

}
