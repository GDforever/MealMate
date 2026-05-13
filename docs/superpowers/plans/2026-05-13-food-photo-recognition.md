# Food Photo Recognition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add food photo recognition to MealMate — users upload/take food photos, DeepSeek Vision identifies the dish and estimates calories, auto-creates a meal record.

**Architecture:** New `FoodRecognitionService` handles image storage, Base64 encoding, and calls DeepSeek Vision via REST API (Spring AI 1.0.0-M4 lacks reliable multimodal support, so we use RestTemplate directly against the OpenAI-compatible endpoint). Results are parsed and fed to existing `MealRecordService`. Chat integration extends the existing multipart flow — when an image is attached, it's recognized first, then injected into the conversation context.

**Tech Stack:** Spring Boot 3.5.13, Java 21, RestTemplate (vision API), Spring AI (chat), DeepSeek Vision, Vue 3 + Element Plus + TypeScript, Pinia stores.

---

### Task 1: Add RecordSource enum and update entities

**Files:**
- Create: `src/main/java/com/gd/mealmate/model/enums/RecordSource.java`
- Modify: `src/main/java/com/gd/mealmate/model/entity/MealRecord.java`
- Modify: `src/main/java/com/gd/mealmate/model/entity/ChatMessage.java`
- Modify: `src/main/java/com/gd/mealmate/exception/ErrorCode.java`

- [ ] **Step 1: Create RecordSource enum**

```java
// src/main/java/com/gd/mealmate/model/enums/RecordSource.java
package com.gd.mealmate.model.enums;

import lombok.Getter;

@Getter
public enum RecordSource {
    CHAT("聊天"),
    PHOTO("拍照识别"),
    MANUAL("手动录入");

    private final String description;

    RecordSource(String description) {
        this.description = description;
    }
}
```

- [ ] **Step 2: Add imageUrl and source fields to MealRecord entity**

Add after the `tags` field in `src/main/java/com/gd/mealmate/model/entity/MealRecord.java`:

```java
    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RecordSource source;
```

Add the import:
```java
import com.gd.mealmate.model.enums.RecordSource;
```

- [ ] **Step 3: Add imageUrl field to ChatMessage entity**

Add after the `content` field in `src/main/java/com/gd/mealmate/model/entity/ChatMessage.java`:

```java
    @Column(name = "image_url", length = 500)
    private String imageUrl;
```

- [ ] **Step 4: Add image-related error codes to ErrorCode enum**

Add to `src/main/java/com/gd/mealmate/exception/ErrorCode.java`:

```java
    // Food Recognition errors
    IMAGE_RECOGNITION_FAILED(40601, "食物识别失败"),
    IMAGE_TOO_LARGE(40602, "图片文件过大"),
    UNSUPPORTED_IMAGE_TYPE(40603, "不支持的图片格式");
```

- [ ] **Step 5: Build and verify compilation**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/gd/mealmate/model/enums/RecordSource.java \
        src/main/java/com/gd/mealmate/model/entity/MealRecord.java \
        src/main/java/com/gd/mealmate/model/entity/ChatMessage.java \
        src/main/java/com/gd/mealmate/exception/ErrorCode.java
git commit -m "feat: add RecordSource enum and image fields to entities"
```

---

### Task 2: Update DTOs and Mapper for new fields

**Files:**
- Modify: `src/main/java/com/gd/mealmate/dto/response/MealRecordDto.java`
- Modify: `src/main/java/com/gd/mealmate/dto/request/MealRecordRequest.java`
- Modify: `src/main/java/com/gd/mealmate/mapper/MealRecordMapper.java`
- Create: `src/main/java/com/gd/mealmate/dto/request/FoodRecognitionRequest.java`
- Create: `src/main/java/com/gd/mealmate/dto/response/FoodRecognitionResponse.java`

- [ ] **Step 1: Update MealRecordDto with new fields**

Add fields to `MealRecordDto.java` after `tags`:

```java
    private String imageUrl;
    private String source;
```

- [ ] **Step 2: Update MealRecordRequest with optional imageUrl and source**

Add fields to `MealRecordRequest.java` after `tags`:

```java
    private String imageUrl;
    private RecordSource source;
```

Add import:
```java
import com.gd.mealmate.model.enums.RecordSource;
```

- [ ] **Step 3: Update MealRecordMapper to map imageUrl and source**

Replace the `toDto` method in `MealRecordMapper.java`:

```java
    @Mapping(target = "userId", source = "user", qualifiedByName = "getUserId")
    @Mapping(target = "imageUrl", source = "imageUrl")
    @Mapping(target = "source", source = "source", qualifiedByName = "sourceToString")
    MealRecordDto toDto(MealRecord mealRecord);
```

Replace the `toEntity` method:

```java
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    MealRecord toEntity(MealRecordRequest request);
```

Add the new named method:

```java
    @Named("sourceToString")
    default String sourceToString(RecordSource source) {
        return source != null ? source.name() : null;
    }
```

Add import:
```java
import com.gd.mealmate.model.enums.RecordSource;
```

- [ ] **Step 4: Create FoodRecognitionRequest**

```java
// src/main/java/com/gd/mealmate/dto/request/FoodRecognitionRequest.java
package com.gd.mealmate.dto.request;

import com.gd.mealmate.model.enums.MealType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodRecognitionRequest {
    @NotNull(message = "用餐类型不能为空")
    private MealType mealType;
}
```

- [ ] **Step 5: Create FoodRecognitionResponse**

```java
// src/main/java/com/gd/mealmate/dto/response/FoodRecognitionResponse.java
package com.gd.mealmate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodRecognitionResponse {
    private String foodName;
    private Integer calories;
    private Double confidence;
    private Long mealRecordId;
    private String imageUrl;
    private String message;
}
```

- [ ] **Step 6: Build and verify compilation**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/gd/mealmate/dto/ \
        src/main/java/com/gd/mealmate/mapper/MealRecordMapper.java
git commit -m "feat: add food recognition DTOs and update mapper for image fields"
```

---

### Task 3: Create FoodRecognitionService

**Files:**
- Create: `src/main/java/com/gd/mealmate/service/FoodRecognitionService.java`
- Modify: `src/main/java/com/gd/mealmate/dto/response/ChatMessageDto.java` (add imageUrl)

- [ ] **Step 1: Check ChatMessageDto for imageUrl field**

Read `src/main/java/com/gd/mealmate/dto/response/ChatMessageDto.java`. If it doesn't have `imageUrl`, add it:

```java
    private String imageUrl;
```

- [ ] **Step 2: Update ChatMessageMapper to map imageUrl**

Read `src/main/java/com/gd/mealmate/mapper/ChatMessageMapper.java`. Ensure `imageUrl` is mapped from `ChatMessage` to `ChatMessageDto`. MapStruct should auto-map it if the field name matches.

- [ ] **Step 3: Create FoodRecognitionService**

```java
// src/main/java/com/gd/mealmate/service/FoodRecognitionService.java
package com.gd.mealmate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gd.mealmate.dto.request.MealRecordRequest;
import com.gd.mealmate.dto.response.FoodRecognitionResponse;
import com.gd.mealmate.dto.response.MealRecordDto;
import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.exception.ErrorCode;
import com.gd.mealmate.model.enums.MealType;
import com.gd.mealmate.model.enums.RecordSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FoodRecognitionService {

    private final RestTemplate restTemplate;
    private final MealRecordService mealRecordService;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${spring.ai.openai.base-url}")
    private String aiBaseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String aiApiKey;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String visionModel;

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private static final String RECOGNITION_PROMPT = """
        你是一个专业的食物识别助手。用户会上传一张食物照片，请分析并返回以下JSON格式的结果：
        {
          "foodName": "菜品名称",
          "calories": 估算热量(kcal,整数),
          "confidence": 识别置信度(0-1)
        }

        规则：
        1. 如果照片中有多道菜，返回主要的那道
        2. 热量为估算值，基于常见份量
        3. 如果无法识别，返回 {"error": "无法识别食物"}
        4. 只返回JSON，不要其他文字
        """;

    @SuppressWarnings("unchecked")
    public FoodRecognitionResponse recognize(MultipartFile image, MealType mealType, Long userId) {
        validateImage(image);

        String imageUrl = saveImage(image, userId);
        String base64Image;

        try {
            base64Image = Base64.getEncoder().encodeToString(image.getBytes());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_RECOGNITION_FAILED, "图片读取失败");
        }

        String contentType = image.getContentType();
        String dataUrl = "data:" + contentType + ";base64," + base64Image;

        String aiResponse = callVisionApi(dataUrl);
        FoodRecognitionResponse result = parseResponse(aiResponse);
        result.setImageUrl(imageUrl);

        if (result.getConfidence() != null && result.getConfidence() >= 0.5) {
            MealRecordRequest recordRequest = new MealRecordRequest();
            recordRequest.setUserId(userId);
            recordRequest.setMealType(mealType);
            recordRequest.setFoodName(result.getFoodName());
            recordRequest.setRecordedAt(LocalDateTime.now());
            recordRequest.setTags(result.getCalories() != null ? "热量约" + result.getCalories() + "kcal" : null);
            recordRequest.setImageUrl(imageUrl);
            recordRequest.setSource(RecordSource.PHOTO);

            MealRecordDto record = mealRecordService.createMealRecord(userId, recordRequest);
            result.setMealRecordId(record.getId());
            result.setMessage("已识别为「" + result.getFoodName() + "」，约" + result.getCalories() + "kcal，已自动记录");
        } else {
            result.setMessage("识别不确定，建议重新拍摄或手动记录");
        }

        return result;
    }

    public FoodRecognitionResponse recognizeForChat(MultipartFile image, Long userId) {
        validateImage(image);

        String imageUrl = saveImage(image, userId);
        String base64Image;

        try {
            base64Image = Base64.getEncoder().encodeToString(image.getBytes());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_RECOGNITION_FAILED, "图片读取失败");
        }

        String contentType = image.getContentType();
        String dataUrl = "data:" + contentType + ";base64," + base64Image;

        String aiResponse = callVisionApi(dataUrl);
        FoodRecognitionResponse result = parseResponse(aiResponse);
        result.setImageUrl(imageUrl);

        return result;
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_RECOGNITION_FAILED, "图片不能为空");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE, "图片不能超过10MB");
        }
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE, "仅支持 JPG、PNG、WEBP 格式");
        }
    }

    private String saveImage(MultipartFile image, Long userId) {
        try {
            Path dirPath = Path.of(uploadDir, "food", String.valueOf(userId));
            Files.createDirectories(dirPath);

            String originalFilename = image.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + extension;
            Path filePath = dirPath.resolve(filename);
            image.transferTo(filePath.toFile());

            return filePath.toString();
        } catch (IOException e) {
            log.error("Failed to save image: {}", e.getMessage());
            throw new BusinessException(ErrorCode.IMAGE_RECOGNITION_FAILED, "图片保存失败");
        }
    }

    private String callVisionApi(String base64DataUrl) {
        String url = aiBaseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiApiKey);

        Map<String, Object> textContent = Map.of("type", "text", "text", RECOGNITION_PROMPT);
        Map<String, Object> imageUrlContent = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", base64DataUrl)
        );

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(textContent, imageUrlContent)
        );

        Map<String, Object> requestBody = Map.of(
                "model", visionModel,
                "messages", List.of(userMessage),
                "max_tokens", 500
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("choices").path(0).path("message").path("content").asText();
            }
            throw new BusinessException(ErrorCode.IMAGE_RECOGNITION_FAILED, "AI服务返回异常");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Vision API call failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.IMAGE_RECOGNITION_FAILED, "食物识别服务调用失败");
        }
    }

    private FoodRecognitionResponse parseResponse(String aiResponse) {
        try {
            // Try to extract JSON from the response (may contain markdown code blocks)
            String json = aiResponse.trim();
            if (json.contains("```")) {
                int start = json.indexOf("```");
                int end = json.indexOf("```", start + 3);
                if (end > start) {
                    json = json.substring(start + 3, end).trim();
                    if (json.startsWith("json")) {
                        json = json.substring(4).trim();
                    }
                }
            }

            JsonNode node = objectMapper.readTree(json);

            if (node.has("error")) {
                FoodRecognitionResponse response = new FoodRecognitionResponse();
                response.setMessage(node.get("error").asText());
                return response;
            }

            return FoodRecognitionResponse.builder()
                    .foodName(node.path("foodName").asText())
                    .calories(node.path("calories").asInt())
                    .confidence(node.path("confidence").asDouble())
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse recognition response: {}", aiResponse, e);
            FoodRecognitionResponse response = new FoodRecognitionResponse();
            response.setMessage("食物识别结果解析失败");
            return response;
        }
    }
}
```

- [ ] **Step 4: Build and verify compilation**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/gd/mealmate/service/FoodRecognitionService.java
git commit -m "feat: add FoodRecognitionService with DeepSeek Vision integration"
```

---

### Task 4: Add recognize endpoint to MealRecordController

**Files:**
- Modify: `src/main/java/com/gd/mealmate/controller/MealRecordController.java`

- [ ] **Step 1: Add recognize endpoint**

Add to `MealRecordController.java` after the existing `createMealRecord` method. Add the required imports at the top:

```java
import com.gd.mealmate.dto.request.FoodRecognitionRequest;
import com.gd.mealmate.dto.response.FoodRecognitionResponse;
import com.gd.mealmate.service.FoodRecognitionService;
import org.springframework.web.multipart.MultipartFile;
```

Add the dependency injection (modify the class to use `@RequiredArgsConstructor` and add the field):

```java
    private final FoodRecognitionService foodRecognitionService;
```

Add the endpoint:

```java
    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Recognize food from photo", description = "Uploads a food photo, recognizes the dish, and auto-creates a meal record")
    public ApiResponse<FoodRecognitionResponse> recognizeFood(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("image") MultipartFile image,
            @Valid FoodRecognitionRequest request) {
        Long userId = userPrincipal.getUserId();
        FoodRecognitionResponse response = foodRecognitionService.recognize(image, request.getMealType(), userId);
        return ApiResponse.success(response);
    }
```

Add the import:
```java
import org.springframework.http.MediaType;
```

- [ ] **Step 2: Build and verify compilation**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/gd/mealmate/controller/MealRecordController.java
git commit -m "feat: add POST /api/meal-records/recognize endpoint"
```

---

### Task 5: Update ChatController for multipart image support

**Files:**
- Modify: `src/main/java/com/gd/mealmate/controller/ChatController.java`
- Modify: `src/main/java/com/gd/mealmate/service/ChatService.java`

- [ ] **Step 1: Update ChatController to accept multipart**

Replace the `chat` method in `ChatController.java`:

```java
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
```

Add imports:

```java
import org.springframework.web.multipart.MultipartFile;
```

Remove the `ChatRequest` import if no longer used.

- [ ] **Step 2: Update ChatService.chat() signature and add image handling**

Replace the `chat` method signature in `ChatService.java`:

```java
    private final FoodRecognitionService foodRecognitionService;
```

Add `FoodRecognitionService` to the constructor injection (already handled by `@RequiredArgsConstructor`).

Replace the `chat` method:

```java
    @Transactional
    public Flux<String> chat(String userMessage, Long sessionId, Double latitude, Double longitude, MultipartFile image) {
        User currentUser = getCurrentUser();

        ChatSession session = getOrCreateSession(sessionId, currentUser);

        // Handle image recognition if present
        String recognitionContext = null;
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            try {
                FoodRecognitionResponse recognition = foodRecognitionService.recognizeForChat(image, currentUser.getId());
                imageUrl = recognition.getImageUrl();
                if (recognition.getFoodName() != null && !recognition.getFoodName().isBlank()) {
                    recognitionContext = "用户上传了一张食物照片，已识别为「" + recognition.getFoodName()
                            + "」，估算热量约" + recognition.getCalories() + "kcal。";

                    // Auto-create meal record
                    MealType mealType = inferMealTypeFromTime();
                    MealRecordRequest recordRequest = new MealRecordRequest();
                    recordRequest.setUserId(currentUser.getId());
                    recordRequest.setMealType(mealType);
                    recordRequest.setFoodName(recognition.getFoodName());
                    recordRequest.setRecordedAt(LocalDateTime.now());
                    recordRequest.setTags(recognition.getCalories() != null ? "热量约" + recognition.getCalories() + "kcal" : null);
                    recordRequest.setImageUrl(imageUrl);
                    recordRequest.setSource(RecordSource.PHOTO);

                    mealRecordService.createMealRecord(currentUser.getId(), recordRequest);
                    recognitionContext += "已自动记录为" + mealType.getDescription() + "。";
                }
            } catch (Exception e) {
                log.warn("Food recognition failed for chat image: {}", e.getMessage());
                recognitionContext = "用户上传了一张食物照片，但识别失败。";
            }
        }

        // Save user message (with image indicator)
        String displayContent = userMessage;
        if (image != null && !image.isEmpty()) {
            displayContent = "[图片] " + userMessage;
        }
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setRole(ChatMessageRole.USER);
        userMsg.setContent(displayContent);
        userMsg.setImageUrl(imageUrl);
        messageRepository.save(userMsg);

        // Build system prompt with recognition context
        String systemContent = buildSystemPrompt(currentUser, latitude, longitude, userMessage);
        if (recognitionContext != null) {
            systemContent += "\n\n## 图片识别结果\n" + recognitionContext
                    + "\n\n**重要**：请在回复中告知用户识别结果和记录情况。如果用户说这是早餐/午餐/晚餐，请帮用户修改记录的餐次。";
        }
        SystemMessage systemMessage = new SystemMessage(systemContent);

        // Build message history
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
```

Add the `inferMealTypeFromTime` helper method and new imports to `ChatService`:

```java
import com.gd.mealmate.dto.request.MealRecordRequest;
import com.gd.mealmate.dto.response.FoodRecognitionResponse;
import com.gd.mealmate.model.enums.RecordSource;
import com.gd.mealmate.service.FoodRecognitionService;
import org.springframework.web.multipart.MultipartFile;
```

Add the helper method:

```java
    private MealType inferMealTypeFromTime() {
        int hour = LocalDateTime.now().getHour();
        if (hour >= 6 && hour < 9) return MealType.BREAKFAST;
        if (hour >= 11 && hour < 14) return MealType.LUNCH;
        if (hour >= 17 && hour < 20) return MealType.DINNER;
        return MealType.SNACK;
    }
```

Add the missing import:

```java
import com.gd.mealmate.model.enums.MealType;
```

Add the `FoodRecognitionService` and `MealRecordService` fields (they'll be injected via `@RequiredArgsConstructor`):

```java
    private final FoodRecognitionService foodRecognitionService;
    private final MealRecordService mealRecordService;
```

- [ ] **Step 3: Build and verify compilation**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/gd/mealmate/controller/ChatController.java \
        src/main/java/com/gd/mealmate/service/ChatService.java
git commit -m "feat: add image support to chat — multipart upload with food recognition"
```

---

### Task 6: Frontend — Update TypeScript types

**Files:**
- Modify: `frontend/src/types/meal.ts`
- Modify: `frontend/src/types/chat.ts`

- [ ] **Step 1: Update MealRecord and MealRecordRequest types in meal.ts**

Replace the contents of `frontend/src/types/meal.ts`:

```ts
export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK'

export interface MealRecord {
  id: number
  userId: number
  mealType: MealType
  foodName: string
  restaurantName?: string
  location?: string
  latitude?: number
  longitude?: number
  recordedAt: string
  userRating?: number
  tags?: string
  createdAt: string
  imageUrl?: string
  source?: string
}

export interface MealRecordRequest {
  mealType: MealType
  foodName: string
  restaurantName?: string
  location?: string
  latitude?: number
  longitude?: number
  recordedAt: string
  userRating?: number
  tags?: string
  imageUrl?: string
  source?: string
}

export interface MealFilters {
  startDate?: string
  endDate?: string
  mealType?: MealType
}

export interface MealStats {
  totalRecords: number
  totalCalories: number
  avgCalories: number
  byType: Record<MealType, number>
}

export interface FoodRecognitionResponse {
  foodName: string
  calories: number
  confidence: number
  mealRecordId?: number
  imageUrl?: string
  message: string
}
```

- [ ] **Step 2: Update ChatMessage type in chat.ts**

Replace the contents of `frontend/src/types/chat.ts`:

```ts
export interface ChatSession {
  id: number
  title: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  sessionId: number
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createdAt: string
  imageUrl?: string
  options?: string[]
}

export interface ChatRequest {
  message: string
  sessionId: number
  latitude?: number
  longitude?: number
}

export interface ChatSessionDto {
  id: number
  title: string
  createdAt: string
  updatedAt: string
  messageCount?: number
  messages?: ChatMessage[]
}

export interface ChatMessageDto {
  id: number
  sessionId: number
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createdAt: string
  imageUrl?: string
}

export interface SSEMessage {
  type: 'text' | 'options' | 'message' | 'done' | 'error'
  content?: string
  delta?: string
  items?: string[]
  error?: string
  sessionId?: number
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types/meal.ts frontend/src/types/chat.ts
git commit -m "feat: update TypeScript types for food recognition and image support"
```

---

### Task 7: Frontend — Update API layer

**Files:**
- Modify: `frontend/src/api/meal.ts`
- Modify: `frontend/src/api/chat.ts`

- [ ] **Step 1: Add recognizeFood to meal API**

Replace the contents of `frontend/src/api/meal.ts`:

```ts
import request from './request'
import type { MealRecord, MealRecordRequest, MealFilters, PageResponse, FoodRecognitionResponse } from '@/types'

export const mealApi = {
  getRecords(params: { page: number; size: number; startDate?: string; endDate?: string; mealType?: string }) {
    return request.get<PageResponse<MealRecord>>('/meal-records', { params })
  },

  getRecord(id: number) {
    return request.get<MealRecord>(`/meal-records/${id}`)
  },

  createRecord(data: MealRecordRequest) {
    return request.post<MealRecord>('/meal-records', data)
  },

  updateRecord(id: number, data: MealRecordRequest) {
    return request.put<MealRecord>(`/meal-records/${id}`, data)
  },

  deleteRecord(id: number) {
    return request.delete(`/meal-records/${id}`)
  },

  recognizeFood(image: File, mealType: string): Promise<FoodRecognitionResponse> {
    const formData = new FormData()
    formData.append('image', image)
    formData.append('mealType', mealType)
    return request.post('/meal-records/recognize', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
```

- [ ] **Step 2: Update chat API for multipart support**

Replace the contents of `frontend/src/api/chat.ts`:

```ts
import request from './request'
import type { ChatSessionDto, ChatMessageDto } from '@/types'

export const chatApi = {
  sendMessage(
    message: string,
    sessionId?: number | null,
    location?: { latitude: number; longitude: number },
    image?: File | null
  ): Response {
    const token = localStorage.getItem('token')
    const fullUrl = `${request.defaults.baseURL}/chat`

    // Use multipart when image is attached
    if (image) {
      const formData = new FormData()
      formData.append('message', message)
      if (sessionId) formData.append('sessionId', String(sessionId))
      if (location) {
        formData.append('latitude', String(location.latitude))
        formData.append('longitude', String(location.longitude))
      }
      formData.append('image', image)

      return fetch(fullUrl, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      })
    }

    return fetch(fullUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        message,
        sessionId: sessionId || undefined,
        ...(location ? { latitude: location.latitude, longitude: location.longitude } : {})
      })
    })
  },

  getSessions() {
    return request.get<ChatSessionDto[]>('/chat/sessions')
  },

  getSessionDetail(id: number) {
    return request.get<ChatSessionDto>(`/chat/sessions/${id}`)
  },

  deleteSession(id: number) {
    return request.delete(`/chat/sessions/${id}`)
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/meal.ts frontend/src/api/chat.ts
git commit -m "feat: add recognizeFood API and multipart chat support"
```

---

### Task 8: Frontend — Update ChatInput.vue with image picker

**Files:**
- Modify: `frontend/src/components/chat/ChatInput.vue`

- [ ] **Step 1: Replace ChatInput.vue**

```vue
<!-- src/components/chat/ChatInput.vue -->
<template>
  <div class="chat-input">
    <div v-if="imagePreview" class="image-preview">
      <img :src="imagePreview" alt="preview" />
      <el-button
        class="remove-btn"
        :icon="Close"
        circle
        size="small"
        @click="removeImage"
      />
    </div>
    <div class="input-row">
      <el-button
        :icon="PictureFilled"
        circle
        :disabled="disabled"
        @click="triggerFileInput"
      />
      <input
        ref="fileInput"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        style="display: none"
        @change="handleFileSelect"
      />
      <el-input
        v-model="message"
        type="textarea"
        :rows="rows"
        placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
        :disabled="disabled"
        @keydown="handleKeydown"
        class="message-input"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        :disabled="!canSend"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
    <div class="input-hint">{{ message.length }} / 2000</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Promotion, PictureFilled, Close } from '@element-plus/icons-vue'

const props = defineProps<{
  disabled?: boolean
}>()

const emit = defineEmits<{
  send: [message: string, image?: File]
}>()

const message = ref('')
const rows = ref(1)
const selectedImage = ref<File | null>(null)
const imagePreview = ref<string | null>(null)
const fileInput = ref<HTMLInputElement>()

const canSend = computed(() => {
  const hasContent = message.value.trim().length > 0 || selectedImage.value
  return !props.disabled && hasContent && message.value.length <= 2000
})

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    return
  }

  selectedImage.value = file
  const reader = new FileReader()
  reader.onload = (ev) => {
    imagePreview.value = ev.target?.result as string
  }
  reader.readAsDataURL(file)
}

const removeImage = () => {
  selectedImage.value = null
  imagePreview.value = null
  if (fileInput.value) fileInput.value.value = ''
}

const handleSend = () => {
  if (!canSend.value) return
  emit('send', message.value.trim(), selectedImage.value || undefined)
  message.value = ''
  rows.value = 1
  removeImage()
}
</script>

<style scoped lang="scss">
.chat-input {
  padding: 16px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.image-preview {
  position: relative;
  display: inline-block;
  margin-bottom: 12px;

  img {
    max-width: 200px;
    max-height: 150px;
    border-radius: 8px;
    object-fit: cover;
  }

  .remove-btn {
    position: absolute;
    top: -8px;
    right: -8px;
  }
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.message-input {
  flex: 1;
}

.input-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  text-align: right;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/chat/ChatInput.vue
git commit -m "feat: add image picker to ChatInput component"
```

---

### Task 9: Frontend — Update ChatMessage.vue for image display

**Files:**
- Modify: `frontend/src/components/chat/ChatMessage.vue`

- [ ] **Step 1: Update ChatMessage.vue to show images**

Add `imageUrl` prop support. Update the props:

```ts
const props = defineProps<{
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createdAt?: string
  imageUrl?: string
  options?: string[]
}>()
```

Add image display in the template, before the `.message-text` div:

```html
      <div v-if="imageUrl" class="message-image">
        <img :src="getImageUrl(imageUrl)" alt="上传的图片" @click="previewImage" />
      </div>
      <div class="message-text" v-html="renderedContent"></div>
```

Add image URL helper and preview method in the script:

```ts
const getImageUrl = (url: string) => {
  // If it's already a full URL or data URL, return as-is
  if (url.startsWith('http') || url.startsWith('data:')) return url
  // Otherwise, serve from backend
  return `/api/images/${encodeURIComponent(url)}`
}

const previewImage = () => {
  if (props.imageUrl) {
    window.open(getImageUrl(props.imageUrl), '_blank')
  }
}
```

Add styles for the image:

```scss
.message-image {
  margin-bottom: 8px;

  img {
    max-width: 100%;
    max-height: 300px;
    border-radius: 8px;
    cursor: pointer;
    object-fit: cover;
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/chat/ChatMessage.vue
git commit -m "feat: add image display to ChatMessage component"
```

---

### Task 10: Frontend — Update ChatView.vue for image support

**Files:**
- Modify: `frontend/src/views/chat/ChatView.vue`
- Modify: `frontend/src/composables/useChat.ts`

- [ ] **Step 1: Update useChat.ts to pass image**

Replace the `sendMessage` function in `useChat.ts`:

```ts
  const sendMessage = async (message: string, image?: File) => {
    chatStore.addUserMessage(message, image ? URL.createObjectURL(image) : undefined)
    chatStore.setLoading(true)

    let location: { latitude: number; longitude: number } | undefined
    try {
      const restaurantStore = useRestaurantStore()
      if (restaurantStore.userLocation) {
        location = restaurantStore.userLocation
      } else {
        location = await restaurantStore.getCurrentLocation()
      }
    } catch {
      // Location not available
    }

    const responsePromise = Promise.resolve(
      chatApi.sendMessage(message, chatStore.currentSessionId, location, image || null)
    )

    connect(responsePromise, {
      onSession: (sessionId) => {
        chatStore.migrateMessages(sessionId)
      },
      onMessage: (delta) => {
        chatStore.setStreamingContent(delta)
      },
      onOptions: (items) => {
        chatStore.setMessageOptions(items)
      },
      onDone: async (data) => {
        chatStore.finalizeMessage(data?.sessionId || chatStore.currentSessionId || -1)
        chatStore.setLoading(false)
        try {
          await chatStore.fetchSessions()
        } catch (e) {
          console.error('Failed to refresh sessions:', e)
        }
      },
      onError: (error) => {
        console.error('SSE error:', error)
        chatStore.setLoading(false)
      }
    }).catch(() => {
      chatStore.setLoading(false)
    })
  }
```

- [ ] **Step 2: Update ChatView.vue to pass image from ChatInput**

Update the `handleSendMessage` function:

```ts
const handleSendMessage = async (message: string, image?: File) => {
  await sendMessage(message, image)
}
```

Update the ChatMessage rendering to pass imageUrl:

```html
        <ChatMessage
          v-for="message in currentMessages"
          :key="message.id"
          :role="message.role"
          :content="message.content"
          :created-at="message.createdAt"
          :image-url="message.imageUrl"
          :options="message.options"
          @select-option="handleSelectOption"
        />
```

Update the ChatInput component binding:

```html
        <ChatInput
          :disabled="isLoading"
          @send="handleSendMessage"
        />
```

- [ ] **Step 3: Update chat store to support imageUrl in addUserMessage**

Read `frontend/src/stores/chat.ts`. Update the `addUserMessage` method to accept an optional `imageUrl` parameter and include it in the message object:

```ts
    addUserMessage(content: string, imageUrl?: string) {
      // Add imageUrl to the newly created message object
    }
```

The exact change depends on the store implementation — ensure the message created in `addUserMessage` includes the `imageUrl` field when provided.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/chat/ChatView.vue \
        frontend/src/composables/useChat.ts \
        frontend/src/stores/chat.ts
git commit -m "feat: wire image upload through chat view and composable"
```

---

### Task 11: Frontend — Create FoodCamera standalone photo page

**Files:**
- Create: `frontend/src/views/meal/FoodCameraView.vue`

- [ ] **Step 1: Create FoodCameraView.vue**

```vue
<!-- src/views/meal/FoodCameraView.vue -->
<template>
  <div class="food-camera-view">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <h2>拍照记录</h2>
      <div style="width: 60px"></div>
    </div>

    <!-- Step 1: Select/Take photo -->
    <div v-if="step === 1" class="step-content">
      <div class="upload-area" @click="triggerFileInput">
        <el-icon :size="64" color="#909399"><UploadFilled /></el-icon>
        <p>点击拍照或选择图片</p>
        <p class="hint">支持 JPG、PNG、WEBP，最大 10MB</p>
      </div>
      <input
        ref="fileInput"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        capture="environment"
        style="display: none"
        @change="handleFileSelect"
      />
    </div>

    <!-- Step 2: Preview and select meal type -->
    <div v-if="step === 2" class="step-content">
      <div class="preview-section">
        <img :src="imagePreview!" alt="preview" class="preview-image" />
      </div>

      <div class="meal-type-section">
        <h3>选择餐次</h3>
        <div class="meal-type-options">
          <div
            v-for="option in mealTypeOptions"
            :key="option.value"
            :class="['meal-type-card', { active: selectedMealType === option.value }]"
            @click="selectedMealType = option.value"
          >
            <span class="emoji">{{ option.emoji }}</span>
            <span class="label">{{ option.label }}</span>
          </div>
        </div>
      </div>

      <div class="actions">
        <el-button @click="resetPhoto">重新选择</el-button>
        <el-button type="primary" :loading="recognizing" @click="handleRecognize">
          识别并记录
        </el-button>
      </div>
    </div>

    <!-- Step 3: Result -->
    <div v-if="step === 3" class="step-content">
      <el-result
        :icon="resultSuccess ? 'success' : 'warning'"
        :title="resultSuccess ? '识别成功' : '识别未确定'"
        :sub-title="resultMessage"
      >
        <template #extra>
          <el-button type="primary" @click="resetAll">继续拍照</el-button>
          <el-button @click="router.push('/meals')">查看记录</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { mealApi } from '@/api/meal'
import type { FoodRecognitionResponse } from '@/types'
import type { MealType } from '@/types'

const router = useRouter()

const step = ref(1)
const fileInput = ref<HTMLInputElement>()
const selectedImage = ref<File | null>(null)
const imagePreview = ref<string | null>(null)
const selectedMealType = ref<MealType>('LUNCH')
const recognizing = ref(false)
const resultSuccess = ref(false)
const resultMessage = ref('')

const mealTypeOptions = [
  { value: 'BREAKFAST' as MealType, label: '早餐', emoji: '🌅' },
  { value: 'LUNCH' as MealType, label: '午餐', emoji: '☀️' },
  { value: 'DINNER' as MealType, label: '晚餐', emoji: '🌙' },
  { value: 'SNACK' as MealType, label: '加餐', emoji: '🍪' }
]

onMounted(() => {
  // Smart default based on current time
  const hour = new Date().getHours()
  if (hour >= 6 && hour < 9) selectedMealType.value = 'BREAKFAST'
  else if (hour >= 11 && hour < 14) selectedMealType.value = 'LUNCH'
  else if (hour >= 17 && hour < 20) selectedMealType.value = 'DINNER'
  else selectedMealType.value = 'SNACK'
})

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片不能超过10MB')
    return
  }

  selectedImage.value = file
  const reader = new FileReader()
  reader.onload = (ev) => {
    imagePreview.value = ev.target?.result as string
    step.value = 2
  }
  reader.readAsDataURL(file)
}

const resetPhoto = () => {
  selectedImage.value = null
  imagePreview.value = null
  step.value = 1
  if (fileInput.value) fileInput.value.value = ''
}

const handleRecognize = async () => {
  if (!selectedImage.value) return
  recognizing.value = true

  try {
    const result: FoodRecognitionResponse = await mealApi.recognizeFood(
      selectedImage.value,
      selectedMealType.value
    )
    resultSuccess.value = result.confidence >= 0.5
    resultMessage.value = result.message || `识别为「${result.foodName}」，约${result.calories}kcal`
    step.value = 3
    if (resultSuccess.value) {
      ElMessage.success('已自动记录')
    }
  } catch (e: any) {
    resultSuccess.value = false
    resultMessage.value = '识别失败，请重试或手动记录'
    step.value = 3
  } finally {
    recognizing.value = false
  }
}

const resetAll = () => {
  step.value = 1
  resetPhoto()
  resultSuccess.value = false
  resultMessage.value = ''
}
</script>

<style scoped lang="scss">
.food-camera-view {
  padding: 24px;
  max-width: 600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  h2 {
    margin: 0;
  }
}

.step-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-area {
  width: 100%;
  padding: 60px 20px;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;

  &:hover {
    border-color: #409eff;
  }

  p {
    margin: 12px 0 0;
    color: #606266;
  }

  .hint {
    font-size: 12px;
    color: #909399;
  }
}

.preview-section {
  width: 100%;
  text-align: center;
  margin-bottom: 24px;

  .preview-image {
    max-width: 100%;
    max-height: 300px;
    border-radius: 12px;
    object-fit: cover;
  }
}

.meal-type-section {
  width: 100%;

  h3 {
    margin: 0 0 16px;
    text-align: center;
  }
}

.meal-type-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.meal-type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 8px;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;

  .emoji {
    font-size: 28px;
    margin-bottom: 8px;
  }

  .label {
    font-size: 14px;
    color: #606266;
  }

  &.active {
    border-color: #409eff;
    background: #ecf5ff;

    .label {
      color: #409eff;
      font-weight: 600;
    }
  }
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  width: 100%;

  .el-button {
    flex: 1;
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/meal/FoodCameraView.vue
git commit -m "feat: create FoodCameraView standalone photo recognition page"
```

---

### Task 12: Frontend — Update MealListView and router

**Files:**
- Modify: `frontend/src/views/meal/MealListView.vue`
- Modify: `frontend/src/router/index.ts`

- [ ] **Step 1: Add photo button to MealListView.vue**

In `MealListView.vue`, add a "拍照记录" button next to the existing "添加记录" button in the page-header:

Replace the page-header section:

```html
    <div class="page-header">
      <h2>用餐记录</h2>
      <div class="header-actions">
        <el-button :icon="Camera" @click="router.push('/meals/photo')">拍照记录</el-button>
        <el-button type="primary" :icon="Plus" @click="showCreateDialog">添加记录</el-button>
      </div>
    </div>
```

Add the import and router:

```ts
import { Camera, Plus, Refresh } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
```

Add styles:

```scss
.header-actions {
  display: flex;
  gap: 8px;
}
```

- [ ] **Step 2: Add route for FoodCameraView**

In `router/index.ts`, add a new route inside the authenticated children array, after the `meals/stats` route:

```ts
      {
        path: 'meals/photo',
        name: 'FoodCamera',
        component: () => import('@/views/meal/FoodCameraView.vue'),
        meta: { title: '拍照记录' }
      },
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/meal/MealListView.vue frontend/src/router/index.ts
git commit -m "feat: add photo button to meal list and camera route"
```

---

### Task 13: Integration build and smoke test

**Files:**
- No new files

- [ ] **Step 1: Full backend build**

Run: `./mvnw clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: Full frontend build**

Run: `cd frontend && npm run build`
Expected: Build completes with no errors

- [ ] **Step 3: Final commit (if any fixes were needed)**

If any compilation fixes were needed, commit them:

```bash
git add -A
git commit -m "fix: resolve compilation issues from food recognition integration"
```
