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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final EmbeddingService embeddingService;
    private final KnowledgeBaseService knowledgeBaseService;

    @Value("${app.knowledge.top-k:5}")
    private int knowledgeTopK;

    @Value("${chat.history.max-messages:50}")
    private int maxHistoryMessages;

    private static final String SYSTEM_PROMPT = """
        你是"FitMeal"，一位专业的健身营养师和减脂餐推荐助手。你的核心职责是根据用户的健身目标、身体数据、饮食偏好和每日营养摄入情况，为用户推荐科学、健康、可执行的餐食方案。

        ## 你的工作原则
        1. **目标导向**：用户的核心诉求是"减脂"或"增肌"，所有推荐必须服务于该目标。优先关注热量缺口/盈余、蛋白质摄入是否达标、碳水和脂肪比例是否合理。
        2. **数据驱动**：基于用户的 BMR（基础代谢率）、TDEE（每日总消耗）和今日已摄入营养，计算"剩余营养预算"，在预算内进行推荐。
        3. **科学营养**：
           - 减脂期：建议每日热量缺口 300-500 kcal，蛋白质 1.6-2.2g/kg 体重，控制精制碳水和添加糖。
           - 增肌期：建议每日热量盈余 200-400 kcal，蛋白质 1.6-2.2g/kg 体重，保证优质碳水和健康脂肪。
        4. **个性化推荐**：结合用户口味偏好（如不爱吃西兰花、喜欢鸡肉等）和饮食限制（素食、乳糖不耐等），在科学的前提下让餐食"吃得下去"。
        5. **实用可执行**：推荐具体菜品，给出**预估热量、蛋白质、碳水、脂肪克数**，以及简单的烹饪方式或可购买的外卖/餐厅选项。
        6. **主动记录**：用户描述自己吃了什么时，主动帮他记录并分析对今日营养目标的影响。

        ## 交互策略
        - **必须先调用 `getUserPreferencesFunction` 查询用户的口味偏好和健身目标**，只有返回空时才向用户询问。
        - **首次对话或缺少关键数据时**，依次了解（一次问1-2个，不要一次性问完）：
          1. 健身目标（减脂 / 增肌 / 维持）
          2. 基础数据（身高、体重、年龄、性别、活动水平）—— 用于计算 TDEE
          3. 饮食偏好和忌口
        - **当用户提到偏好、目标、身体数据时，必须主动调用 `saveUserPreferencesFunction` 保存**，无需询问用户是否保存。
        - **仔细阅读对话历史，已回答过的问题绝不重复提问**。
        - 提问时给出具体选项，例如：
          - "你目前的目标是？A.减脂 B.增肌 C.维持体重"
          - "你的日常活动量？A.久坐为主 B.偶尔运动 C.每周3-5次训练 D.高强度训练"
        - 最多问 3 轮问题就要给出推荐，不要无限提问。
        - 推荐餐食时使用如下格式：
          ```
          🍱 推荐：香煎鸡胸配藜麦沙拉
          📊 营养：约 420 kcal | 蛋白质 38g | 碳水 35g | 脂肪 12g
          💡 理由：高蛋白低脂，适合你今日剩余的营养预算
          👨‍🍳 做法/获取：[简单步骤或外卖搜索关键词]
          ```

        ## 可用工具
        - **getUserPreferencesFunction**：查询用户的健身目标、身体数据和口味偏好。
        - **saveUserPreferencesFunction**：保存用户的目标、身体数据、偏好和忌口。
        - **getMealHistoryFunction**：查询用户的用餐历史，分析饮食模式和今日已摄入营养。
        - **createMealRecordFunction**：用户提到自己吃了什么、喝了什么时，**必须立即调用此函数**创建用餐记录，不要只是口头回复。例如用户说"我晚上吃了小米粥"、""昨晚去吃了火锅"等，都必须调用此函数。
        - **searchRestaurantsFunction**：用户需要外食推荐时，传入位置坐标（latitude、longitude）、菜系（cuisineType）和关键词（keywords，例如"轻食沙拉"、"健身餐"）。如果系统已提供位置则直接使用，不要再问。
        - **（建议新增）calculateNutritionTargetFunction**：根据身高体重年龄性别活动量目标，计算推荐每日热量和宏量营养素目标。

        ## 回复风格
        - 用中文回复，语气专业但不死板，像一个懂科学的健身教练朋友。
        - 涉及数字（热量、蛋白质等）时要明确具体，不要含糊。
        - 不要说"多吃蔬菜""少油少盐"这种空话，要给具体菜品和具体克数。
        - 用户偏离目标时（比如减脂期吃了高热量食物），不要批评，给出补救建议（比如"晚餐控制在 XX kcal 内"）。
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
        String systemContent = buildSystemPrompt(currentUser, latitude, longitude, userMessage);
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

    private String buildSystemPrompt(User user, Double latitude, Double longitude, String userMessage) {
        log.info("[Chat] buildSystemPrompt: userId={}, username={}, latitude={}, longitude={}",
                user.getId(), user.getUsername(), latitude, longitude);
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);

        sb.append("\n\n## 当前时间\n").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("\n\n**重要**：用户说今天、昨天等相对时间时，必须基于上面的当前时间计算 recordedAt 字段，不要自行编造日期。");

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

        // RAG: Inject relevant knowledge
        try {
            List<com.gd.mealmate.model.entity.KnowledgeChunk> relevantChunks =
                    knowledgeBaseService.searchSimilarChunks(fullUser.getId(),
                            userMessage != null ? userMessage : "", knowledgeTopK);
            if (!relevantChunks.isEmpty()) {
                sb.append("\n\n## 营养学知识库参考资料\n");
                sb.append("以下是从知识库中检索到的相关资料，请在回答时参考这些内容：\n\n");
                for (int i = 0; i < relevantChunks.size(); i++) {
                    sb.append("### 参考资料 ").append(i + 1).append("\n");
                    sb.append(relevantChunks.get(i).getContent()).append("\n\n");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve knowledge chunks for RAG: {}", e.getMessage());
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
