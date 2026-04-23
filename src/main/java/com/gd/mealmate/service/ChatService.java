package com.gd.mealmate.service;

import com.gd.mealmate.dto.response.ChatMessageDto;
import com.gd.mealmate.dto.response.ChatSessionDto;
import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.exception.ErrorCode;
import com.gd.mealmate.mapper.ChatMessageMapper;
import com.gd.mealmate.mapper.ChatSessionMapper;
import com.gd.mealmate.model.entity.ChatMessage;
import com.gd.mealmate.model.entity.ChatSession;
import com.gd.mealmate.model.entity.User;
import com.gd.mealmate.model.enums.ChatMessageRole;
import com.gd.mealmate.repository.ChatMessageRepository;
import com.gd.mealmate.repository.ChatSessionRepository;
import com.gd.mealmate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final OpenAiChatModel chatModel;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    @org.springframework.beans.factory.annotation.Value("${chat.history.max-messages:50}")
    private int maxHistoryMessages;

    @Transactional
    public Flux<String> chat(String userMessage, Long sessionId) {
        User currentUser = getCurrentUser();

        // Get or create session
        ChatSession session = getOrCreateSession(sessionId, currentUser);

        // Save user message
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setRole(ChatMessageRole.USER);
        userMsg.setContent(userMessage);
        messageRepository.save(userMsg);

        // Build message history
        List<Message> messages = buildMessageHistory(session);
        messages.add(new UserMessage(userMessage));

        // Create prompt and call AI
        Prompt prompt = new Prompt(messages);

        return Flux.create(sink -> {
            try {
                StringBuilder fullResponse = new StringBuilder();

                chatModel.stream(prompt).subscribe(chatResponse -> {
                    String content = chatResponse.getResult().getOutput().getContent();
                    if (content != null) {
                        fullResponse.append(content);
                        sink.next(content);
                    }
                }, null, () -> {
                    // On complete, save assistant message
                    if (fullResponse.length() > 0) {
                        ChatMessage assistantMsg = new ChatMessage();
                        assistantMsg.setSession(session);
                        assistantMsg.setRole(ChatMessageRole.ASSISTANT);
                        assistantMsg.setContent(fullResponse.toString());
                        messageRepository.save(assistantMsg);
                    }
                    sink.complete();
                });

            } catch (Exception e) {
                log.error("Chat error: {}", e.getMessage());
                sink.error(new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE));
            }
        });
    }

    @Transactional(readOnly = true)
    public List<ChatSessionDto> getUserSessions() {
        User currentUser = getCurrentUser();
        List<ChatSession> sessions = sessionRepository.findByUserOrderByUpdatedAtDesc(currentUser);

        return sessions.stream()
                .map(session -> {
                    ChatSessionDto dto = sessionMapper.toDto(session);
                    dto.setMessageCount((long) session.getMessages().size());
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getSessionHistory(Long sessionId) {
        User currentUser = getCurrentUser();

        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CHAT_SESSION));

        if (!session.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.INVALID_CHAT_SESSION);
        }

        List<ChatMessage> messages = messageRepository.findBySessionOrderByCreatedAtAsc(session);
        return messages.stream()
                .map(messageMapper::toDto)
                .toList();
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        User currentUser = getCurrentUser();

        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CHAT_SESSION));

        if (!session.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.INVALID_CHAT_SESSION);
        }

        sessionRepository.delete(session);
    }

    private ChatSession getOrCreateSession(Long sessionId, User user) {
        if (sessionId == null) {
            ChatSession newSession = new ChatSession();
            newSession.setUser(user);
            newSession.setTitle("新对话");
            return sessionRepository.save(newSession);
        }

        return sessionRepository.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CHAT_SESSION));
    }

    private List<Message> buildMessageHistory(ChatSession session) {
        List<ChatMessage> history = messageRepository.findBySessionOrderByCreatedAtAsc(session);

        // Get last N messages for context
        int startIndex = Math.max(0, history.size() - maxHistoryMessages);
        List<ChatMessage> recentMessages = history.subList(startIndex, history.size());

        List<Message> messages = new ArrayList<>();
        for (ChatMessage msg : recentMessages) {
            if (msg.getRole() == ChatMessageRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else if (msg.getRole() == ChatMessageRole.ASSISTANT) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        return messages;
    }

    private User getCurrentUser() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return User.builder()
                .id(principal.getUserId())
                .username(principal.getUsername())
                .build();
    }
}
