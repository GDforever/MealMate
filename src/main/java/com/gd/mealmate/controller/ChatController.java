package com.gd.mealmate.controller;

import com.gd.mealmate.dto.request.ChatRequest;
import com.gd.mealmate.dto.response.ApiResponse;
import com.gd.mealmate.dto.response.ChatMessageDto;
import com.gd.mealmate.dto.response.ChatSessionDto;
import com.gd.mealmate.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI聊天对话接口")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息并流式返回AI回复")
    public Flux<String> chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request.getMessage(), request.getSessionId());
    }

    @GetMapping("/sessions")
    @Operation(summary = "获取用户的所有对话会话")
    public ApiResponse<List<ChatSessionDto>> getSessions() {
        List<ChatSessionDto> sessions = chatService.getUserSessions();
        return ApiResponse.success(sessions);
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "获取会话详情（包含历史消息）")
    public ApiResponse<List<ChatMessageDto>> getSessionHistory(@PathVariable Long id) {
        List<ChatMessageDto> messages = chatService.getSessionHistory(id);
        return ApiResponse.success(messages);
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "删除对话会话")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        chatService.deleteSession(id);
        return ApiResponse.success();
    }
}
