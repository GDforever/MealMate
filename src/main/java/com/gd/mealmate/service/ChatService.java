package com.gd.mealmate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.gd.mealmate.repository.UserRepository;
import com.gd.mealmate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final OpenAiChatModel chatModel;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${chat.history.max-messages:50}")
    private int maxHistoryMessages;

    private static final String SYSTEM_PROMPT = """
            你是"MealMate"，一位专业的私人营养师和美食推荐助手。你的核心职责是根据用户的饮食偏好、健康状况和日常饮食习惯，为用户推荐每日三餐的搭配方案。

            ## 你的工作原则
            1. **个性化推荐**：优先了解用户的口味偏好（如喜欢辣、清淡、甜等）、过敏信息、饮食限制（如素食、低碳水等），基于这些信息做推荐。
            2. **营养均衡**：推荐的餐食需注意荤素搭配、营养均衡，考虑蛋白质、碳水化合物、脂肪、维生素的合理比例。
            3. **实用性强**：推荐具体菜品或餐厅，而不是泛泛而谈。如果用户提到附近有餐厅需求，可以搜索附近餐厅给出建议。
            4. **记录饮食**：当用户告诉你他吃了什么时，主动帮他记录用餐信息。

            ## 交互策略
            - **必须先通过 `getUserPreferencesFunction` 查询用户的口味偏好**，只有在函数返回空结果时才向用户提问。
            - **当用户提到口味偏好时（如"我喜欢吃辣的"、"我不喜欢甜食"），必须主动调用 `saveUserPreferencesFunction` 保存偏好**，不需要询问用户是否需要保存。
            - **仔细阅读对话历史**，用户已经回答过的问题绝对不能重复提问。
            - 只有在缺少关键信息时才提问，给出具体的选项让用户选择，例如：
              - "你偏好哪种口味？A.川菜 B.粤菜 C.江浙菜 D.其他___"
            - 每次提问控制在1-2个问题，不要一次问太多。
            - 得到用户回答后，结合已有信息综合分析并给出推荐。
            - 推荐时给出具体的菜品名称、大概的营养说明，以及适合的用餐场景。
            - 切记不要问个不停，问用户最多三个问题之后就结合用户给的答案回答

            ## 可用工具
            - 你可以查询用户的用餐历史记录，了解用户近期的饮食习惯。
            - 你可以搜索附近的餐厅，帮用户找到合适的就餐地点。当用户提到附近美食、附近餐厅、附近吃什么等需求时，必须调用 `searchRestaurantsFunction`，并将用户的位置坐标（latitude、longitude）和可选的菜系类型（cuisineType）、搜索关键词（keywords）传入。如果系统提供了用户位置，直接使用，不要再问用户位置。
            - 你可以获取用户的口味偏好设置。
            - 你可以为用户创建用餐记录。
            - 你可以保存用户的口味偏好，当用户提到自己的口味喜好时，主动调用函数保存。
            当需要这些信息时，请主动使用对应的功能，不需要询问用户是否需要查询。

            请用中文回复，语气亲切自然，像朋友聊天一样。
            """;

    @Transactional
    public Flux<String> chat(String userMessage, Long sessionId, Double latitude, Double longitude) {
        User currentUser = getCurrentUser();

        ChatSession session = getOrCreateSession(sessionId, currentUser);

        // Save user message
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setRole(ChatMessageRole.USER);
        userMsg.setContent(userMessage);
        messageRepository.save(userMsg);

        // Build system prompt with user context
        String systemContent = buildSystemPrompt(currentUser, latitude, longitude);
        SystemMessage systemMessage = new SystemMessage(systemContent);

        // Build message history (already includes the just-saved user message)
        List<Message> messages = buildMessageHistory(session);
        messages.add(0, systemMessage);

        // Enable function calling
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .withFunction("getUserPreferencesFunction")
                .withFunction("getMealHistoryFunction")
                .withFunction("searchRestaurantsFunction")
                .withFunction("createMealRecordFunction")
                .withFunction("saveUserPreferencesFunction")
                .build();

        Prompt prompt = new Prompt(messages, chatOptions);

        log.info("=== AI Prompt Start ===");
        for (Message msg : messages) {
            log.info("[{}] {}", msg.getMessageType(), msg.getContent());
        }
        log.info("=== AI Prompt End ===");

        Long sessionIdValue = session.getId();

        return Flux.create(sink -> {
            try {
                // Send session ID as the first event so the frontend can track it
                Map<String, Object> sessionEvent = new LinkedHashMap<>();
                sessionEvent.put("type", "session");
                sessionEvent.put("sessionId", sessionIdValue);
                sink.next(objectMapper.writeValueAsString(sessionEvent));

                StringBuilder fullResponse = new StringBuilder();

                chatModel.stream(prompt).subscribe(chatResponse -> {
                    String content = chatResponse.getResult().getOutput().getContent();
                    if (content != null) {
                        fullResponse.append(content);
                        try {
                            Map<String, String> event = new LinkedHashMap<>();
                            event.put("type", "text");
                            event.put("content", content);
                            sink.next(objectMapper.writeValueAsString(event));
                        } catch (Exception e) {
                            sink.next(content);
                        }
                    }
                }, error -> {
                    log.error("Chat stream error: {}", error.getMessage(), error);
                    sink.complete();
                }, () -> {
                    try {
                        String responseText = fullResponse.toString();
                        log.info("=== AI Response ===\n{}", responseText);
                        if (!responseText.isEmpty()) {
                            ChatMessage assistantMsg = new ChatMessage();
                            assistantMsg.setSession(session);
                            assistantMsg.setRole(ChatMessageRole.ASSISTANT);
                            assistantMsg.setContent(responseText);
                            messageRepository.save(assistantMsg);

                            // Extract options and send as separate event
                            List<String> options = extractOptions(responseText);
                            if (!options.isEmpty()) {
                                Map<String, Object> optionsEvent = new LinkedHashMap<>();
                                optionsEvent.put("type", "options");
                                optionsEvent.put("items", options);
                                sink.next(objectMapper.writeValueAsString(optionsEvent));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to save assistant message: {}", e.getMessage(), e);
                    }
                    sink.complete();
                });

            } catch (Exception e) {
                log.error("Chat error: {}", e.getMessage());
                sink.error(new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE));
            }
        });
    }

    private String buildSystemPrompt(User user, Double latitude, Double longitude) {
        log.info("[Chat] buildSystemPrompt: userId={}, username={}, latitude={}, longitude={}",
                user.getId(), user.getUsername(), latitude, longitude);
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);

        User fullUser = userRepository.findById(user.getId()).orElse(user);
        if (fullUser.getTastePreferences() != null && !fullUser.getTastePreferences().isBlank()) {
            sb.append("\n\n## 当前用户的口味偏好\n").append(fullUser.getTastePreferences());
        }
        sb.append("\n\n## 当前用户信息\n用户ID：").append(fullUser.getId()).append("\n用户名：").append(fullUser.getUsername());
        sb.append("\n\n**重要**：调用任何函数时，必须传入 userId 参数，值为 ").append(fullUser.getId()).append("。");

        if (latitude != null && longitude != null) {
            sb.append("\n\n## 用户当前位置\n纬度：").append(latitude).append("\n经度：").append(longitude);
            sb.append("\n\n**重要**：当用户询问附近美食或餐厅推荐时，必须调用 `searchRestaurantsFunction`，并传入用户的当前位置坐标（latitude=").append(latitude).append(", longitude=").append(longitude).append("）。不要询问用户位置，直接使用这里的坐标。");
        } else {
            sb.append("\n\n**注意**：当前未获取到用户位置。如果用户询问附近美食或餐厅推荐，请礼貌地告诉用户系统暂时无法获取位置，建议用户在浏览器中允许定位权限后重试。");
        }

        return sb.toString();
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

    // Matches patterns like "A.xxx", "B.xxx", "C.xxx", "D.xxx" in lines like "A.川菜 B.粤菜 C.江浙菜"
    private static final Pattern OPTIONS_PATTERN = Pattern.compile("([A-D])\\.\\s*([^A-D\\n]+?)(?=\\s+[A-D]\\.|$)");

    private List<String> extractOptions(String text) {
        List<String> options = new ArrayList<>();
        Matcher matcher = OPTIONS_PATTERN.matcher(text);
        while (matcher.find()) {
            String option = matcher.group(1) + ". " + matcher.group(2).trim();
            options.add(option);
        }
        return options;
    }

    private User getCurrentUser() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return User.builder()
                .id(principal.getUserId())
                .username(principal.getUsername())
                .build();
    }
}
