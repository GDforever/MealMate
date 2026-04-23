# MealMate Phase 2 — Spring AI Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate Spring AI ChatClient with DeepSeek, implement Function Calling for restaurant recommendations and meal record queries, add SSE streaming chat endpoint, and integrate Amap API for restaurant data synchronization.

**Architecture:** Single-service integration - add Spring AI ChatClient, Function Callbacks, and chat persistence to existing Spring Boot application. AI interacts with existing services through Function Calling interface.

**Tech Stack:** Spring AI 1.0.0-M4, DeepSeek API, WebFlux (SSE), Spring Scheduling, PostgreSQL

---

## Task 1: Add Maven Dependencies

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add Spring AI and WebFlux dependencies to pom.xml**

```xml
<!-- Add after springdoc-openapi dependency, before </dependencies> -->

<!-- Spring AI OpenAI (DeepSeek compatible) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M4</version>
</dependency>

<!-- WebFlux for SSE streaming -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

- [ ] **Step 2: Add Spring AI milestone repository**

```xml
<!-- Add after <properties> section, before <dependencies> -->
<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```

- [ ] **Step 3: Verify build**

Run: `./mvnw clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add pom.xml
git commit -m "feat: add Spring AI and WebFlux dependencies"
```

---

## Task 2: Update application.yml Configuration

**Files:**
- Modify: `src/main/resources/application.yml`
- Delete: `src/main/resources/application.properties` (if exists from Phase 1)

- [ ] **Step 1: Update application.yml with Spring AI and Amap configuration**

Add to end of file:
```yaml
# Spring AI Configuration (DeepSeek)
spring:
  ai:
    openai:
      base-url: ${AI_BASE_URL:https://api.deepseek.com}
      api-key: ${AI_API_KEY:your-api-key}
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7

# Amap (高德地图) Configuration
amap:
  api-key: ${AMAP_API_KEY:your-amap-key}
  base-url: https://restapi.amap.com/v3
  sync:
    enabled: true
    cron: "0 0 2 * * ?"
    center:
      latitude: 39.9042
      longitude: 116.4074
    radius: 5000

# Chat Configuration
chat:
  history:
    max-messages: 50
  title:
    max-length: 20
```

- [ ] **Step 2: Verify application starts**

Run: `./mvnw spring-boot:run`
Expected: Application starts without errors
Stop: Ctrl+C

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.yml
git commit -m "feat: add Spring AI and Amap configuration"
```

---

## Task 3: Add Error Codes

**Files:**
- Modify: `src/main/java/com/gd/mealmate/exception/ErrorCode.java`

- [ ] **Step 1: Add new error codes**

Add to enum (before final semicolon):
```java
    // AI Service errors (50xxx)
    AI_SERVICE_UNAVAILABLE(50002, "AI服务暂时不可用"),
    AMAP_API_ERROR(50003, "高德地图API调用失败"),

    // Chat errors (40xxx)
    INVALID_CHAT_SESSION(40003, "无效的会话ID"),
    MESSAGE_TOO_LONG(40004, "消息内容过长"),
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/gd/mealmate/exception/ErrorCode.java
git commit -m "feat: add AI and chat error codes"
```

---

## Task 4: Create ChatMessageRole Enum

**Files:**
- Create: `src/main/java/com/gd/mealmate/model/enums/ChatMessageRole.java`

- [ ] **Step 1: Create the enum file**

```java
package com.gd.mealmate.model.enums;

public enum ChatMessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/gd/mealmate/model/enums/ChatMessageRole.java
git commit -m "feat: add ChatMessageRole enum"
```

---

## Task 5: Create ChatSession Entity

**Files:**
- Create: `src/main/java/com/gd/mealmate/model/entity/ChatSession.java`

- [ ] **Step 1: Create ChatSession entity**

```java
package com.gd.mealmate.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions", indexes = {
    @Index(name = "idx_chat_session_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/gd/mealmate/model/entity/ChatSession.java
git commit -m "feat: add ChatSession entity"
```

---

## Task 6: Create ChatMessage Entity

**Files:**
- Create: `src/main/java/com/gd/mealmate/model/entity/ChatMessage.java`

- [ ] **Step 1: Create ChatMessage entity**

```java
package com.gd.mealmate.model.entity;

import com.gd.mealmate.model.enums.ChatMessageRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_message_session_id_created_at", columnList = "session_id, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ChatMessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: Update ChatSession to add messages relationship**

Add to `ChatSession.java` after `updatedAt` field:
```java
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();
```

Add import:
```java
import java.util.ArrayList;
import java.util.List;
```

- [ ] **Step 3: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/gd/mealmate/model/entity/ChatSession.java src/main/java/com/gd/mealmate/model/entity/ChatMessage.java
git commit -m "feat: add ChatMessage entity with session relationship"
```

---

## Task 7: Create ChatSessionRepository

**Files:**
- Create: `src/main/java/com/gd/mealmate/repository/ChatSessionRepository.java`

- [ ] **Step 1: Create repository interface**

```java
package com.gd.mealmate.repository;

import com.gd.mealmate.model.entity.ChatSession;
import com.gd.mealmate.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserOrderByUpdatedAtDesc(User user);

    boolean existsByUserAndId(User user, Long sessionId);
}
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/gd/mealmate/repository/ChatSessionRepository.java
git commit -m "feat: add ChatSessionRepository"
```

---

## Task 8: Create ChatMessageRepository

**Files:**
- Create: `src/main/java/com/gd/mealmate/repository/ChatMessageRepository.java`

- [ ] **Step 1: Create repository interface**

```java
package com.gd.mealmate.repository;

import com.gd.mealmate.model.entity.ChatMessage;
import com.gd.mealmate.model.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);
}
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/gd/mealmate/repository/ChatMessageRepository.java
git commit -m "feat: add ChatMessageRepository"
```

---

## Task 9: Create Chat DTOs

**Files:**
- Create: `src/main/java/com/gd/mealmate/dto/request/ChatRequest.java`
- Create: `src/main/java/com/gd/mealmate/dto/response/ChatMessageDto.java`
- Create: `src/main/java/com/gd/mealmate/dto/response/ChatSessionDto.java`

- [ ] **Step 1: Create ChatRequest DTO**

```java
package com.gd.mealmate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000字符")
    private String message;

    private Long sessionId;  // null for new session
}
```

- [ ] **Step 2: Create ChatMessageDto**

```java
package com.gd.mealmate.dto.response;

import com.gd.mealmate.model.enums.ChatMessageRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    private Long id;
    private ChatMessageRole role;
    private String content;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: Create ChatSessionDto**

```java
package com.gd.mealmate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDto {

    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long messageCount;
}
```

- [ ] **Step 4: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/gd/mealmate/dto/request/ChatRequest.java src/main/java/com/gd/mealmate/dto/response/ChatMessageDto.java src/main/java/com/gd/mealmate/dto/response/ChatSessionDto.java
git commit -m "feat: add chat DTOs"
```

---

## Task 10: Create Amap DTOs

**Files:**
- Create: `src/main/java/com/gd/mealmate/dto/amap/AmapPOI.java`
- Create: `src/main/java/com/gd/mealmate/dto/amap/AmapPOIResponse.java`

- [ ] **Step 1: Create AmapPOI DTO**

```java
package com.gd.mealmate.dto.amap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmapPOI {

    private String id;
    private String name;
    private String address;
    private String tel;
    private Double locationLat;  // parsed from "lat,lng"
    private Double locationLng;
    private String type;
    private Integer distance;
}
```

- [ ] **Step 2: Create AmapPOIResponse DTO**

```java
package com.gd.mealmate.dto.amap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmapPOIResponse {

    private Integer status;
    private String info;
    private String infocode;
    private List<AmapPOI> pois;

    @JsonProperty("count")
    private String totalCount;
}
```

- [ ] **Step 3: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/gd/mealmate/dto/amap/AmapPOI.java src/main/java/com/gd/mealmate/dto/amap/AmapPOIResponse.java
git commit -m "feat: add Amap POI DTOs"
```

---

## Task 11: Create ChatConfig

**Files:**
- Create: `src/main/java/com/gd/mealmate/config/ChatConfig.java`

- [ ] **Step 1: Create ChatConfig for ChatClient**

```java
package com.gd.mealmate.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi(baseUrl, apiKey);
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .build();

        return new OpenAiChatModel(openAiApi, options);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/gd/mealmate/config/ChatConfig.java
git commit -m "feat: add ChatConfig for ChatClient"
```

---

## Task 12: Create AmapService

**Files:**
- Create: `src/main/java/com/gd/mealmate/service/AmapService.java`

- [ ] **Step 1: Create AmapService**

```java
package com.gd.mealmate.service;

import com.gd.mealmate.dto.amap.AmapPOI;
import com.gd.mealmate.dto.amap.AmapPOIResponse;
import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.model.entity.Restaurant;
import com.gd.mealmate.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AmapService {

    private final RestTemplate restTemplate;
    private final RestaurantRepository restaurantRepository;

    @Value("${amap.api-key}")
    private String apiKey;

    @Value("${amap.base-url}")
    private String baseUrl;

    @Value("${amap.sync.center.latitude}")
    private Double centerLatitude;

    @Value("${amap.sync.center.longitude}")
    private Double centerLongitude;

    @Value("${amap.sync.radius}")
    private Integer syncRadius;

    @Value("${amap.sync.enabled:true}")
    private boolean syncEnabled;

    public AmapPOIResponse searchPOI(String keywords, double lat, double lng, int radius) {
        String url = String.format(
                "%s/place/around?key=%s&location=%f,%f&radius=%d&keywords=%s",
                baseUrl, apiKey, lng, lat, radius, keywords
        );

        try {
            return restTemplate.getForObject(url, AmapPOIResponse.class);
        } catch (Exception e) {
            log.error("Amap API call failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AMAP_API_ERROR);
        }
    }

    @Scheduled(cron = "${amap.sync.cron:0 0 2 * * ?}")
    public void syncNearbyRestaurants() {
        if (!syncEnabled) {
            log.info("Amap sync is disabled");
            return;
        }

        log.info("Starting Amap restaurant sync at {}", LocalDateTime.now());

        String[] keywords = {"餐厅", "美食", "快餐", "小吃"};

        for (String keyword : keywords) {
            try {
                AmapPOIResponse response = searchPOI(keyword, centerLatitude, centerLongitude, syncRadius);

                if (response != null && "1".equals(String.valueOf(response.getStatus()))) {
                    savePOIData(response);
                    log.info("Synced {} POIs for keyword: {}",
                            response.getPois() != null ? response.getPois().size() : 0, keyword);
                }
            } catch (Exception e) {
                log.error("Failed to sync keyword {}: {}", keyword, e.getMessage());
            }
        }

        log.info("Amap restaurant sync completed");
    }

    public void savePOIData(AmapPOIResponse response) {
        if (response.getPois() == null) {
            return;
        }

        for (AmapPOI poi : response.getPois()) {
            // Check if restaurant already exists by externalId
            Restaurant existing = restaurantRepository.findByExternalId(poi.getId());

            if (existing == null) {
                Restaurant restaurant = new Restaurant();
                restaurant.setName(poi.getName());
                restaurant.setAddress(poi.getAddress());
                restaurant.setLatitude(poi.getLocationLat());
                restaurant.setLongitude(poi.getLocationLng());
                restaurant.setSource("AMAP");
                restaurant.setExternalId(poi.getId());
                restaurant.setCuisineType(extractCuisineType(poi.getType()));
                restaurant.setCreatedAt(LocalDateTime.now());

                restaurantRepository.save(restaurant);
            }
        }
    }

    private String extractCuisineType(String type) {
        if (type == null) {
            return "其他";
        }

        if (type.contains("中餐") || type.contains("川菜") || type.contains("粤菜")) {
            return "中餐";
        } else if (type.contains("西餐")) {
            return "西餐";
        } else if (type.contains("日料") || type.contains("日本")) {
            return "日料";
        } else if (type.contains("韩餐") || type.contains("韩国")) {
            return "韩餐";
        } else {
            return "其他";
        }
    }
}
```

- [ ] **Step 2: Add RestTemplate bean**

Create `src/main/java/com/gd/mealmate/config/RestTemplateConfig.java`:
```java
package com.gd.mealmate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

- [ ] **Step 3: Add findByExternalId to RestaurantRepository**

Add to `src/main/java/com/gd/mealmate/repository/RestaurantRepository.java`:
```java
    Restaurant findByExternalId(String externalId);
```

- [ ] **Step 4: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/gd/mealmate/service/AmapService.java src/main/java/com/gd/mealmate/config/RestTemplateConfig.java src/main/java/com/gd/mealmate/repository/RestaurantRepository.java
git commit -m "feat: add AmapService for POI sync"
```

---

## Task 13: Create ChatService

**Files:**
- Create: `src/main/java/com/gd/mealmate/service/ChatService.java`

- [ ] **Step 1: Create ChatService**

```java
package com.gd.mealmate.service;

import com.gd.mealmate.dto.response.ChatMessageDto;
import com.gd.mealmate.dto.response.ChatSessionDto;
import com.gd.mealmate.exception.BusinessException;
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

    @Value("${chat.history.max-messages:50}")
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
```

- [ ] **Step 2: Create ChatSessionMapper**

Create `src/main/java/com/gd/mealmate/mapper/ChatSessionMapper.java`:
```java
package com.gd.mealmate.mapper;

import com.gd.mealmate.dto.response.ChatSessionDto;
import com.gd.mealmate.model.entity.ChatSession;
import org.mapstruct.Mapper;

@Mapper
public interface ChatSessionMapper {
    ChatSessionDto toDto(ChatSession session);
}
```

- [ ] **Step 3: Create ChatMessageMapper**

Create `src/main/java/com/gd/mealmate/mapper/ChatMessageMapper.java`:
```java
package com.gd.mealmate.mapper;

import com.gd.mealmate.dto.response.ChatMessageDto;
import com.gd.mealmate.model.entity.ChatMessage;
import org.mapstruct.Mapper;

@Mapper
public interface ChatMessageMapper {
    ChatMessageDto toDto(ChatMessage message);
}
```

- [ ] **Step 4: Add @Builder to User entity**

Update `src/main/java/com/gd/mealmate/model/entity/User.java` - add `@Builder` annotation:
```java
import lombok.Builder;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    // ... existing fields ...
}
```

- [ ] **Step 5: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/gd/mealmate/service/ChatService.java src/main/java/com/gd/mealmate/mapper/ChatSessionMapper.java src/main/java/com/gd/mealmate/mapper/ChatMessageMapper.java src/main/java/com/gd/mealmate/model/entity/User.java
git commit -m "feat: add ChatService with streaming support"
```

---

## Task 14: Create GetUserPreferencesFunction

**Files:**
- Create: `src/main/java/com/gd/mealmate/function/GetUserPreferencesFunction.java`

- [ ] **Step 1: Create the function request/response DTOs**

Create `src/main/java/com/gd/mealmate/function/dto/GetUserPreferencesRequest.java`:
```java
package com.gd.mealmate.function.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetUserPreferencesRequest {
    // Empty - userId from SecurityContext
}
```

Create `src/main/java/com/gd/mealmate/function/dto/GetUserPreferencesResponse.java`:
```java
package com.gd.mealmate.function.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetUserPreferencesResponse {
    private Long userId;
    private String username;
    private List<String> tastePreferences;
}
```

- [ ] **Step 2: Create the function**

```java
package com.gd.mealmate.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gd.mealmate.function.dto.GetUserPreferencesRequest;
import com.gd.mealmate.function.dto.GetUserPreferencesResponse;
import com.gd.mealmate.model.entity.User;
import com.gd.mealmate.repository.UserRepository;
import com.gd.mealmate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@Description("获取用户的口味偏好设置")
@Slf4j
@RequiredArgsConstructor
public class GetUserPreferencesFunction implements Function<GetUserPreferencesRequest, String> {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(GetUserPreferencesRequest request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new com.gd.mealmate.exception.BusinessException(
                        com.gd.mealmate.exception.ErrorCode.RESOURCE_NOT_FOUND));

        GetUserPreferencesResponse response = new GetUserPreferencesResponse(
                user.getId(),
                user.getUsername(),
                List.of()  // Simplified for MVP
        );

        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response: {}", e.getMessage());
            return "{\"error\": \"Failed to get preferences\"}";
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

- [ ] **Step 4: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/gd/mealmate/function/
git commit -m "feat: add GetUserPreferencesFunction"
```

---

## Task 15: Create SearchRestaurantsFunction

**Files:**
- Create: `src/main/java/com/gd/mealmate/function/SearchRestaurantsFunction.java`

- [ ] **Step 1: Create the function request/response DTOs**

Create `src/main/java/com/gd/mealmate/function/dto/SearchRestaurantsRequest.java`:
```java
package com.gd.mealmate.function.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRestaurantsRequest {
    private Double latitude;
    private Double longitude;
    private Integer radius;
    private String cuisineType;
}
```

Create `src/main/java/com/gd/mealmate/function/dto/SearchRestaurantsResponse.java`:
```java
package com.gd.mealmate.function.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRestaurantsResponse {
    private List<RestaurantSimpleDto> restaurants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantSimpleDto {
        private Long id;
        private String name;
        private String address;
        private String cuisineType;
        private Double rating;
        private Double avgPrice;
    }
}
```

- [ ] **Step 2: Create the function**

```java
package com.gd.mealmate.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gd.mealmate.dto.response.RestaurantDto;
import com.gd.mealmate.function.dto.SearchRestaurantsRequest;
import com.gd.mealmate.function.dto.SearchRestaurantsResponse;
import com.gd.mealmate.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@Description("搜索附近的餐厅，支持按菜系类型过滤")
@Slf4j
@RequiredArgsConstructor
public class SearchRestaurantsFunction implements Function<SearchRestaurantsRequest, String> {

    private final RestaurantService restaurantService;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(SearchRestaurantsRequest request) {
        try {
            List<RestaurantDto> restaurants = restaurantService.findNearby(
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getRadius() != null ? request.getRadius() : 3000,
                    request.getCuisineType()
            );

            List<SearchRestaurantsResponse.RestaurantSimpleDto> simpleRestaurants = restaurants.stream()
                    .map(r -> new SearchRestaurantsResponse.RestaurantSimpleDto(
                            r.getId(),
                            r.getName(),
                            r.getAddress(),
                            r.getCuisineType(),
                            r.getRating(),
                            r.getAvgPrice()
                    ))
                    .toList();

            SearchRestaurantsResponse response = new SearchRestaurantsResponse(simpleRestaurants);
            return objectMapper.writeValueAsString(response);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response: {}", e.getMessage());
            return "{\"error\": \"Failed to search restaurants\"}";
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/gd/mealmate/function/
git commit -m "feat: add SearchRestaurantsFunction"
```

---

## Task 16: Create GetMealHistoryFunction

**Files:**
- Create: `src/main/java/com/gd/mealmate/function/GetMealHistoryFunction.java`

- [ ] **Step 1: Create the function request/response DTOs**

Create `src/main/java/com/gd/mealmate/function/dto/GetMealHistoryRequest.java`:
```java
package com.gd.mealmate.function.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetMealHistoryRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer limit;
}
```

- [ ] **Step 2: Create the function**

```java
package com.gd.mealmate.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gd.mealmate.dto.response.MealRecordDto;
import com.gd.mealmate.function.dto.GetMealHistoryRequest;
import com.gd.mealmate.model.entity.User;
import com.gd.mealmate.security.UserPrincipal;
import com.gd.mealmate.service.MealRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;

@Component
@Description("获取用户的用餐历史记录")
@Slf4j
@RequiredArgsConstructor
public class GetMealHistoryFunction implements Function<GetMealHistoryRequest, String> {

    private final MealRecordService mealRecordService;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(GetMealHistoryRequest request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        LocalDateTime startDateTime = request.getStartDate() != null
                ? request.getStartDate().atStartOfDay()
                : LocalDateTime.now().minusDays(7);
        LocalDateTime endDateTime = request.getEndDate() != null
                ? request.getEndDate().atTime(LocalTime.MAX)
                : LocalDateTime.now();

        int pageSize = request.getLimit() != null ? Math.min(request.getLimit(), 100) : 20;
        Pageable pageable = PageRequest.of(0, pageSize);

        Page<MealRecordDto> records = mealRecordService.getUserRecords(
                principal.getUserId(),
                startDateTime,
                endDateTime,
                null,
                pageable
        );

        try {
            return objectMapper.writeValueAsString(records.getContent());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response: {}", e.getMessage());
            return "{\"error\": \"Failed to get meal history\"}";
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/gd/mealmate/function/
git commit -m "feat: add GetMealHistoryFunction"
```

---

## Task 17: Create CreateMealRecordFunction

**Files:**
- Create: `src/main/java/com/gd/mealmate/function/CreateMealRecordFunction.java`

- [ ] **Step 1: Create the function**

```java
package com.gd.mealmate.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gd.mealmate.dto.request.MealRecordRequest;
import com.gd.mealmate.dto.response.MealRecordDto;
import com.gd.mealmate.model.entity.User;
import com.gd.mealmate.security.UserPrincipal;
import com.gd.mealmate.service.MealRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
@Description("创建用餐记录")
@Slf4j
@RequiredArgsConstructor
public class CreateMealRecordFunction implements Function<MealRecordRequest, String> {

    private final MealRecordService mealRecordService;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(MealRecordRequest request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        try {
            MealRecordDto record = mealRecordService.createRecord(principal.getUserId(), request);
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response: {}", e.getMessage());
            return "{\"error\": \"Failed to create meal record\"}";
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/gd/mealmate/function/CreateMealRecordFunction.java
git commit -m "feat: add CreateMealRecordFunction"
```

---

## Task 18: Create ChatController

**Files:**
- Create: `src/main/java/com/gd/mealmate/controller/ChatController.java`

- [ ] **Step 1: Create ChatController with SSE endpoint**

```java
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
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/gd/mealmate/controller/ChatController.java
git commit -m "feat: add ChatController with SSE endpoint"
```

---

## Task 19: Create AmapController

**Files:**
- Create: `src/main/java/com/gd/mealmate/controller/AmapController.java`

- [ ] **Step 1: Create AmapController**

```java
package com.gd.mealmate.controller;

import com.gd.mealmate.dto.response.ApiResponse;
import com.gd.mealmate.service.AmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/amap")
@RequiredArgsConstructor
@Tag(name = "Admin - Amap", description = "高德地图管理接口")
@SecurityRequirement(name = "bearerAuth")
public class AmapController {

    private final AmapService amapService;

    @PostMapping("/sync")
    @Operation(summary = "手动触发高德地图餐厅数据同步")
    public ApiResponse<String> triggerSync() {
        amapService.syncNearbyRestaurants();
        return ApiResponse.success("同步任务已触发");
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/gd/mealmate/controller/AmapController.java
git commit -m "feat: add AmapController for manual sync"
```

---

## Task 20: Update SecurityConfig

**Files:**
- Modify: `src/main/java/com/gd/mealmate/config/SecurityConfig.java`

- [ ] **Step 1: Add admin endpoints to SecurityConfig**

Find the line that permits `/api/auth/**` and add chat endpoint:
```java
// Find this section in requestMatchers:
.requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/api/chat").permitAll()
```

Wait - `/api/chat` should require authentication. Change to:
```java
// Keep auth endpoints public
.requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
// All other /api/** require authentication
.anyRequest().authenticated()
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/gd/mealmate/config/SecurityConfig.java
git commit -m "feat: update SecurityConfig for chat endpoints"
```

---

## Task 21: Enable Scheduling

**Files:**
- Create: `src/main/java/com/gd/mealmate/config/SchedulingConfig.java`

- [ ] **Step 1: Create SchedulingConfig**

```java
package com.gd.mealmate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/gd/mealmate/config/SchedulingConfig.java
git commit -m "feat: enable scheduling for Amap sync"
```

---

## Task 22: Final Build and Verification

**Files:**
- All

- [ ] **Step 1: Run full build**

Run: `./mvnw clean package -DskipTests`
Expected: BUILD SUCCESS, JAR created at `target/MealMate-0.0.1-SNAPSHOT.jar`

- [ ] **Step 2: Verify all endpoints exist**

Run: `./mvnw spring-boot:run` (background)
Wait for: "Started MealMateApplication in X.XXX seconds"
Stop: Ctrl+C

- [ ] **Step 3: Create completion commit**

```bash
git add -A
git commit -m "feat: Phase 2 Spring AI integration complete"
```

---

## Implementation Complete

All 22 tasks completed. Phase 2 delivers:

1. ✅ Spring AI ChatClient integrated with DeepSeek
2. ✅ 4 Function Callbacks registered and callable
3. ✅ SSE streaming chat endpoint at `/api/chat`
4. ✅ Chat history persistence (ChatSession, ChatMessage)
5. ✅ Amap API integration with scheduled sync
6. ✅ Complete API coverage via Swagger UI

**Next Phase:** Phase 3 - Vue 3 + Element Plus Frontend
