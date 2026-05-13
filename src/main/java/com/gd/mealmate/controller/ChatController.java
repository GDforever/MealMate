package com.gd.mealmate.controller;

import com.gd.mealmate.dto.response.ApiResponse;
import com.gd.mealmate.dto.response.ChatMessageDto;
import com.gd.mealmate.dto.response.ChatSessionDto;
import com.gd.mealmate.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI聊天对话接口")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息并流式返回AI回复（支持图片）")
    public Flux<String> chat(
            @RequestParam("message") String message,
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        return chatService.chat(message, sessionId, latitude, longitude, image);
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
