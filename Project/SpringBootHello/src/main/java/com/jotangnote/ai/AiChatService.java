
package com.jotangnote.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import com.jotangnote.entity.Note;
import com.jotangnote.service.NoteService;

import java.util.ArrayList;

import org.slf4j.Logger; // 添加logger导入
import org.slf4j.LoggerFactory;

@Service
public class AiChatService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final NoteService noteService;
    private static final Logger log =
        LoggerFactory.getLogger(AiChatService.class);

    public AiChatService(
            @Value("${mimo.api.url}") String apiUrl,
            @Value("${mimo.api.key}") String apiKey,
            @Value("${mimo.api.model}") String model,
            NoteService noteService) {

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .build();

        this.apiKey = apiKey;
        this.model = model;
        this.noteService = noteService;
    }

    public String chat(String message) {
        Map<?, ?> response = restClient.post()
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "input", message,
                        "stream", false,
                        "max_output_tokens", 1024
                ))
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("MiMo API 返回了空响应");
        }

        Object outputObject = response.get("output");
        
        if (!(outputObject instanceof List output)) {
            throw new IllegalStateException("MiMo API 响应中没有有效的 output");
        }

        StringBuilder reply = new StringBuilder();

        for (Object itemObject : output) {
            if (!(itemObject instanceof Map<?, ?> item)) {
                continue;
            }

            if (!"message".equals(item.get("type"))) {
                continue;
            }

            Object contentObject = item.get("content");

            if (!(contentObject instanceof List<?> contents)) {
                continue;
            }

            for (Object contentItem : contents) {
                if (!(contentItem instanceof Map<?, ?> content)) {
                    continue;
                }

                if ("output_text".equals(content.get("type"))) {
                    Object textObject = content.get("text");

                    if (textObject instanceof String text) {
                        reply.append(text);
                    }
                }
            }
        }

        if (reply.isEmpty()) {
            throw new IllegalStateException(
                    "MiMo API 响应中没有找到文本内容"
            );
        }

        return reply.toString();
    }
    
    public String chatHistory(List<Map<String, String>> history) {
        Map<?, ?> response = restClient.post()
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(

                        "model", model,
                        "input", history,
                        "stream", false,
                        "max_output_tokens", 1024
                ))
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("MiMo API 返回了空响应");
        }

        Object outputObject = response.get("output");

        if (!(outputObject instanceof List<?> output)) {
            throw new IllegalStateException("MiMo API 响应中没有有效的 output");
        }

        StringBuilder reply = new StringBuilder();

        for (Object itemObject : output) {
            if (!(itemObject instanceof Map<?, ?> item)) {
                continue;
            }

            if (!"message".equals(item.get("type"))) {
                continue;
            }

            Object contentObject = item.get("content");

            if (!(contentObject instanceof List<?> contents)) {
                continue;
            }

            for (Object contentItem : contents) {
                if (!(contentItem instanceof Map<?, ?> content)) {
                    continue;
                }

                if ("output_text".equals(content.get("type"))) {
                    Object textObject = content.get("text");

                    if (textObject instanceof String text) {
                        reply.append(text);
                    }
                }
            }
        }

        if (reply.isEmpty()) {
            throw new IllegalStateException(
                    "MiMo API 响应中没有找到文本内容"
            );
        }

        return reply.toString();
    }
    public String chatWithNoteTool(List<Map<String, String>> history) {

        // 1. 声明 AI 可以调用的工具
        Map<String, Object> tool = Map.of(
                "type", "function",
                "name", "get_note_by_id",
                "description", "根据笔记 ID 查询笔记标题和正文内容",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "id", Map.of(
                                        "type", "integer",
                                        "description", "要查询的笔记 ID"
                                )
                        ),
                        "required", List.of("id"),
                        "additionalProperties", false
                ),
                "strict", true
        );

        // 2. 向 MiMo 发送聊天历史和工具定义
        Map<?, ?> response = restClient.post()
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "input", history,
                        "tools", List.of(tool),
                        "tool_choice", "auto",
                        "stream", false,
                        "max_output_tokens", 1024
                ))
                .retrieve()
                .body(Map.class);

        if (response == null
                || !(response.get("output") instanceof List<?> output)) {
            throw new IllegalStateException("MiMo API 返回了无效响应");
        }

        // 3. 查找 AI 是否提出了工具调用请求
        Map<?, ?> functionCall = null;

        for (Object itemObject : output) {
            if (!(itemObject instanceof Map<?, ?> item)) {
                continue;
            }

            log.info("MiMo 返回的 output 项类型：{}", item.get("type"));

            if ("function_call".equals(item.get("type"))) {
                log.info("MiMo 请求调用工具：{}", item.get("name"));
                functionCall = item;
                break;
            }
        }

        // 4. 如果 AI 没有请求调用工具，直接返回文本回答
        if (functionCall == null) {
            return extractReply(response);
        }

        // 5. 检查工具名称
        if (!"get_note_by_id".equals(functionCall.get("name"))) {
            throw new IllegalArgumentException("不支持的工具名称");
        }

        // 6. 解析 AI 生成的参数 JSON
        Object argumentsObject = functionCall.get("arguments");

        if (!(argumentsObject instanceof String arguments)) {
            throw new IllegalArgumentException("工具参数格式错误");
        }

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile("\"id\"\\s*:\\s*(\\d+)")
                        .matcher(arguments);

        if (!matcher.find()) {
            throw new IllegalArgumentException("工具参数中缺少有效的笔记 ID");
        }

        Long noteId = Long.parseLong(matcher.group(1));

        // 7. 执行已有的笔记查询方法
        Note note = noteService.getNoteById(noteId);
        log.info("正在执行笔记查询工具，noteId={}", noteId);

        String toolResult;

        if (note == null || Integer.valueOf(1).equals(note.getIsDeleted())) {
            toolResult = "未找到该笔记。";
        } else {
            toolResult = "笔记标题：" + note.getTitle()
                    + "\n笔记正文：" + note.getContent();
        }

        // 8. 将 AI 的工具调用和工具执行结果放入下一次请求
        List<Object> nextInput = new ArrayList<>(history);
        nextInput.addAll(output);
        nextInput.add(Map.of(
                "type", "function_call_output",
                "call_id", functionCall.get("call_id"),
                "output", toolResult
        ));

        // 9. 将工具结果交还给 MiMo，请它生成最终回答
        Map<?, ?> finalResponse = restClient.post()
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "input", nextInput,
                        "tools", List.of(tool),
                        "tool_choice", "auto",
                        "stream", false,
                        "max_output_tokens", 1024
                ))
                .retrieve()
                .body(Map.class);

        return extractReply(finalResponse);
    }
    private String extractReply(Map<?, ?> response) {
        if (response == null
                || !(response.get("output") instanceof List<?> output)) {
            throw new IllegalStateException("MiMo API 返回了无效响应");
        }

        StringBuilder reply = new StringBuilder();

        for (Object itemObject : output) {
            if (!(itemObject instanceof Map<?, ?> item)) {
                continue;
            }

            if (!"message".equals(item.get("type"))) {
                continue;
            }

            if (!(item.get("content") instanceof List<?> contents)) {
                continue;
            }

            for (Object contentObject : contents) {
                if (!(contentObject instanceof Map<?, ?> content)) {
                    continue;
                }

                if ("output_text".equals(content.get("type"))
                        && content.get("text") instanceof String text) {
                    reply.append(text);
                }
            }
        }

        if (reply.isEmpty()) {
            throw new IllegalStateException("MiMo API 响应中没有文本回答");
        }

        return reply.toString();
    }

}
