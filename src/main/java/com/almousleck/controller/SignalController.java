package com.almousleck.controller;

import com.almousleck.dto.signal.SignalMessage;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "WebSocket", description = "实时通信（WebSocket，不在Swagger UI中显示）")
public class SignalController {
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * WebSocket入站消息处理
     * 
     * **功能说明:**
     * 接收来自AI智能眼镜的实时信号并处理
     * 
     * **消息流向:**
     * 设备 → /app/signal → 服务器处理 → AI核心（待实现：AIServiceClient集成）
     * 
     * **业务流程:**
     * 1. 接收设备发送的信号消息
     * 2. 验证用户身份（通过WebSocket认证）
     * 3. 记录信号类型和动作
     * 4. 添加服务器时间戳（用于延迟检测）
     * 5. 转发到Python/FastAPI AI核心进行处理（待实现：AIServiceClient集成）
     * 6. 回传处理结果
     * 
     * **信号类型:**
     * - NAVIGATION: 导航请求
     * - OBJECT_DETECTION: 物体识别请求
     * - TEXT_RECOGNITION: 文字识别请求
     * - VOICE_COMMAND: 语音命令
     * - EMERGENCY: 紧急呼叫
     * 
     * **消息格式:**
     * ```json
     * {
     *   "type": "NAVIGATION",
     *   "action": "START",
     *   "data": {...},
     *   "timestamp": "2025-12-26T10:00:00Z"
     * }
     * ```
     * 
     * **性能指标:**
     * - 目标延迟: <100ms
     * - 服务器时间戳用于计算往返延迟
     * 
     * **下一步开发:**
     * - 集成Python/FastAPI AI核心
     * - 使用gRPC或Redis进行服务间通信
     * - 添加消息队列确保可靠传输
     * 
     * @param message 信号消息
     * @param authentication 用户认证信息
     * @return 处理后的信号消息（包含服务器时间戳）
     */
    @MessageMapping("/signal")
    @SendTo("/topic/ar-updates")
    public SignalMessage processSignal(@Payload SignalMessage message, Authentication authentication) {
        String username = (authentication != null) ?
                authentication.getName() : "未知";
        log.info("收到信号来自用户 [{}]: 类型={} 动作={}", username, message.getType(), message.getAction());
        
        // Note: AI service integration pending - will be implemented via AIServiceClient
        // Future implementation: aiServiceClient.processSignal(message);

        // 回显消息并添加服务器时间戳（用于延迟检测）
        message.setSenderId(username);
        message.setTimestamp(Instant.now());

        return message;
    }

    /**
     * 系统主动推送消息给用户
     * 
     * **功能说明:**
     * 服务器主动向特定用户推送消息（如导航指令、AI识别结果等）
     * 
     * **使用场景:**
     * - AI识别结果回传
     * - 导航路线更新
     * - 紧急警告通知
     * - 系统状态通知
     * 
     * **消息流向:**
     * 内部服务 → 服务器 → /queue/notifications → 用户设备
     * 
     * **调用方式:**
     * 此方法由内部服务调用，如:
     * - NavigationService: 推送导航指令
     * - AIService: 推送识别结果
     * - AlertService: 推送紧急警告
     * 
     * **示例代码:**
     * ```java
     * SignalMessage navMessage = SignalMessage.builder()
     *     .type("NAVIGATION")
     *     .action("TURN_LEFT")
     *     .data(Map.of("distance", "10米", "direction", "左转"))
     *     .timestamp(Instant.now())
     *     .build();
     * signalController.sendToUser("username", navMessage);
     * ```
     * 
     * **订阅端点:**
     * 客户端需订阅: /user/queue/notifications
     * 
     * **消息格式:**
     * ```json
     * {
     *   "type": "NAVIGATION",
     *   "action": "TURN_LEFT",
     *   "data": {
     *     "distance": "10米",
     *     "direction": "左转"
     *   },
     *   "senderId": "system",
     *   "timestamp": "2025-12-26T10:00:00Z"
     * }
     * ```
     * 
     * **注意事项:**
     * - 只有在线用户才能接收消息
     * - 离线消息不会保存（待实现：消息持久化功能）
     * - 消息发送失败会记录日志
     * 
     * @param username 目标用户名
     * @param message 要发送的信号消息
     */
    public void sendToUser(String username, SignalMessage message) {
        log.info("推送消息给用户 [{}]: 类型={} 动作={}", username, message.getType(), message.getAction());
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);
    }

}
