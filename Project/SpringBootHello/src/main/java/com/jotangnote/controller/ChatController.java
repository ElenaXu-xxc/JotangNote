
package com.jotangnote.controller;

import com.jotangnote.ai.AiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID; // 生成一个基本上不会重复的会话标识。
import java.util.concurrent.ConcurrentHashMap; // 保存多个会话，适合处理并发访问的基本场景。

@RestController
public class ChatController {

    private static final Logger log =
            LoggerFactory.getLogger(ChatController.class);

    private final AiChatService aiChatService;

    private final Map<String, List<Map<String, String>>> conversations =
            new ConcurrentHashMap<>();

    public ChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(
            @RequestBody Map<String, String> request) {

        String message = request.get("message");
        String conversationId = request.get("conversationId");

        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "message 不能为空"
            );
        }

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        List<Map<String, String>> history =
                conversations.computeIfAbsent(
                        conversationId,
                        id -> new ArrayList<>()
                );

        synchronized (history) {
            history.add(Map.of(
                    "role", "user",
                    "content", message
            ));

            try {
                String reply = aiChatService.chatWithNoteTool(history); // 调用新增的 chatWithNoteTool()，让 AI 可以决定是否查询笔记。

                history.add(Map.of(
                        "role", "assistant",
                        "content", reply
                ));

                return Map.of(
                        "conversationId", conversationId,
                        "reply", reply
                );
            } catch (Exception e) {
                log.error("调用 MiMo API 失败，具体原因如下：", e);

                history.remove(history.size() - 1);

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "调用 AI 服务失败，请检查后端终端日志"
                );
            }
        }
    }
}
