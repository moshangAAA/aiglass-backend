# AI Glass Hardware Integration Guide

## 📋 Current Status

### ✅ Completed
- WebSocket server configuration (STOMP + SockJS)
- JWT authentication interceptor
- Signal message DTO definition
- Test page (`/websocket-test.html`)
- Device heartbeat mechanism (Redis cache)
- Device pairing and management API

### ⚠️ Todo
- WebSocket message handler binding (DONE - just added)
- FastAPI AI service integration needed
- Audio/video stream processing needed
- Real-time AI response push needed

---

## 🔌 Hardware Integration Checklist

### 1. **WebSocket Connection Configuration**

**Connection Details:**
```
WebSocket URL: ws://<server-ip>:8080/ws
Protocol: STOMP over SockJS
Authentication: JWT Bearer Token
```

**Connection Steps:**
```javascript
// 1. First login via REST API to get JWT
POST /api/v1/auth/login
{
  "identifier": "13027207507",
  "password": "password"
}
// Response: { "token": "eyJhbGc..." }

// 2. Connect to WebSocket with JWT
CONNECT /ws
Headers: { "Authorization": "Bearer eyJhbGc..." }
```

**Subscribe Channels:**
```
/user/queue/notifications  // Receive server push (AI results, navigation commands, etc.)
/topic/ar-updates          // Receive broadcast messages
```

**Send Signals:**
```
SEND /app/signal
Body: {
  "type": "VOICE_COMMAND",       // Signal type
  "action": "NAVIGATE_HOME",     // Action
  "payload": {                   // Data
    "confidence": 0.98,
    "timestamp": "2025-12-26T..."
  }
}
```

---

### 2. **Signal Type Definition**

| Type | Description | Hardware Action |
|------|-------------|-----------------|
| `VOICE_COMMAND` | Voice command | Microphone record → Send audio data |
| `OBJECT_DETECTION` | Object recognition | Camera capture → Send image |
| `TEXT_RECOGNITION` | Text recognition | Camera capture → Send image |
| `NAVIGATION` | Navigation request | Send current location + destination |
| `EMERGENCY` | Emergency call | Send location + SOS signal |
| `GESTURE` | Gesture recognition | IMU data → Send motion trajectory |

---

### 3. **Device Heartbeat Mechanism**

**Purpose:** Keep device online status, monitor battery level

**Heartbeat Configuration:**
```
Frequency: Every 30 seconds
Timeout: No heartbeat for 60s → Mark offline
```

**Heartbeat Request (REST):**
```http
POST /api/v1/devices/heartbeat?serialNumber=ABC123&batteryLevel=85
```

**Notes:** 
- This endpoint does NOT require JWT authentication (public)
- Device must be paired first before sending heartbeat

---

## 🚀 Next Development Tasks

### Phase 1: Complete WebSocket Message Handling (1-2 days)

**Tasks:**
1. ✅ Add `@MessageMapping("/signal")` annotation to `SignalController` (DONE)
2. ✅ Implement signal routing by type field
3. ✅ Add message validation and error handling
4. ✅ Implement async message processing (avoid blocking)

**Files to modify:**
```
src/main/java/com/almousleck/controller/SignalController.java
```

---

### Phase 2: FastAPI AI Service Integration (3-5 days)

**Architecture:**
```
Smart Glasses → WebSocket → Spring Boot → gRPC/HTTP → FastAPI (AI Processing)
                                     ↓
                                  Redis Queue
                                     ↓
                              FastAPI Processing Done
                                     ↓
                           Spring Boot ← Result
                                     ↓
                              WebSocket Push
                                     ↓
                             Smart Glasses Display
```

**Tasks:**
1. Create `AIServiceClient` to call FastAPI
2. Implement async task queue (Redis Pub/Sub or message queue)
3. Handle AI service timeout and retry
4. Implement result caching (avoid duplicate AI computation)

**New Files:**
```
src/main/java/com/almousleck/client/AIServiceClient.java
src/main/java/com/almousleck/service/AIIntegrationService.java
src/main/java/com/almousleck/config/AIServiceConfig.java
```

---

### Phase 3: Audio/Video Stream Processing (3-5 days)

**Current Issue:**
- WebSocket only supports text messages
- Audio/video requires binary stream transmission

**Solution A: Base64 Encoding (Simple but inefficient)**
```javascript
// Hardware side
const audioBase64 = btoa(audioBytes);
send({
  type: "VOICE_COMMAND",
  payload: { audio: audioBase64 }
});
```

**Solution B: Separate HTTP Upload (Recommended)**
```javascript
// 1. Upload audio to dedicated endpoint
POST /api/v1/media/upload-audio
Content-Type: multipart/form-data
Body: <audio file>
Response: { "mediaId": "abc123" }

// 2. WebSocket sends reference
send({
  type: "VOICE_COMMAND",
  payload: { "mediaId": "abc123" }
});
```

**Tasks:**
1. Create audio/video upload endpoints
2. Implement temporary file storage (Redis or local)
3. Add file size and format validation
4. Implement automatic cleanup of expired files

**New Endpoints:**
```
POST /api/v1/media/upload-audio
POST /api/v1/media/upload-image
POST /api/v1/media/upload-video
```

---

### Phase 4: Real-time Push Optimization (2-3 days)

**Current Push Method:**
```java
messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);
```

**Optimization Tasks:**
1. Add message priority queue (urgent messages first)
2. Implement offline message persistence (store when user offline)
3. Add message delivery confirmation mechanism
4. Implement message retransmission (for unstable network)

---

## 📝 Hardware Engineer Integration Checklist

### Test Environment Setup
- [ ] Get test server address: `http://<ip>:8080`
- [ ] Get test account: username + password
- [ ] Test REST API login to get JWT
- [ ] Test WebSocket connection (use `/websocket-test.html`)
- [ ] Test device pairing flow

### Development Environment Configuration
- [ ] Confirm device serial number generation rules
- [ ] Confirm audio format (recommended: WAV/16kHz/16bit)
- [ ] Confirm image format (recommended: JPEG/PNG)
- [ ] Confirm data transmission size limit (suggested <1MB per message)

### Protocol Alignment
- [ ] Confirm WebSocket library (recommended: STOMP.js or native WebSocket)
- [ ] Confirm JSON serialization library
- [ ] Confirm error handling mechanism
- [ ] Confirm reconnection strategy (auto-reconnect on disconnect)

### Performance Metrics
- [ ] WebSocket latency target: <100ms
- [ ] Heartbeat frequency: Every 30 seconds
- [ ] Message loss rate: <0.1%
- [ ] Battery life: >8 hours

---

## 🔧 Quick Test Commands

### 1. Test REST API
```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"13027207507","password":"your_password"}'

# Device heartbeat
curl -X POST "http://localhost:8080/api/v1/devices/heartbeat?serialNumber=TEST001&batteryLevel=85"
```

### 2. Test WebSocket
Open browser and visit:
```
http://localhost:8080/websocket-test.html
```

---

## 📞 Technical Support

**Backend Team: Lento Team**
- For issues check: `/swagger-ui.html`
- WebSocket logs: Search for "WebSocket authenticated"
- Heartbeat logs: Search for "updateHeartbeat"

**Next Meeting Agenda:**
1. Confirm device hardware specifications
2. Test WebSocket connection stability
3. Determine audio/video transmission solution
4. Create integration test plan

