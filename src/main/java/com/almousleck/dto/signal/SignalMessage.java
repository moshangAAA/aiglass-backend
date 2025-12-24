package com.almousleck.dto.signal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalMessage {
    private String type; // ex "VOICE_COMMAND", "GESTURE", "SYSTEM_ALERT"
    private String action; // "NAVIGATE", "DISPLAY_TEXT", "WARN"
    private Map<String, Object> payload; // Flexible data: { "text": "Turn Left", "lat": 30.5 }
    private String senderId;    // Device Serial or User ID
    private Instant timestamp;
}
