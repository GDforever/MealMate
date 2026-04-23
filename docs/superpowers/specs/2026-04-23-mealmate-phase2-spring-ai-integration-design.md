# MealMate Phase 2 — Spring AI Integration Design

**Date:** 2026-04-23
**Phase:** 2 of 3 (Spring AI Integration)
**Status:** Approved

## Overview

集成 Spring AI 到 MealMate 后端，实现智能对话助手。用户可以通过自然语言与 AI 交互，完成餐厅推荐、用餐查询、记录用餐等操作。采用单服务集成架构，在现有 Spring Boot 应用中直接集成 Spring AI ChatClient 和 Function Calling。

## Tech Stack

| Component | Choice |
|-----------|--------|
| AI Model | DeepSeek (deepseek-chat) |
| AI Framework | Spring AI (OpenAI兼容) |
| Maps API | 高德地图 API (POI搜索) |
| Streaming | Server-Sent Events (SSE) |
| Chat Storage | PostgreSQL (新增表) |

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Spring Boot 应用                          │
├─────────────────────────────────────────────────────────────────┤
│  Controller 层                                                   │
│  ├── ChatController (新增) - SSE 流式聊天端点                    │
│  └── 现有 Controllers (保持不变)                                  │
├─────────────────────────────────────────────────────────────────┤
│  Service 层                                                       │
│  ├── ChatService (新增) - 对话管理、AI 调用、Function Calling    │
│  ├── AmapService (新增) - 高德地图 API 集成                      │
│  └── 现有 Services (保持不变)                                    │
├─────────────────────────────────────────────────────────────────┤
│  Function 层 (新增) - AI Function Calling 定义                   │
│  ├── GetUserPreferencesFunction                                 │
│  ├── SearchRestaurantsFunction                                   │
│  ├── GetMealHistoryFunction                                      │
│  └── CreateMealRecordFunction                                    │
├─────────────────────────────────────────────────────────────────┤
│  Repository 层                                                    │
│  ├── ChatSessionRepository (新增)                               │
│  ├── ChatMessageRepository (新增)                               │
│  └── 现有 Repositories (保持不变)                                │
├─────────────────────────────────────────────────────────────────┤
│  Entity 层 (新增)                                                 │
│  ├── ChatSession - 会话                                          │
│  └── ChatMessage - 消息                                          │
└─────────────────────────────────────────────────────────────────┘
```

## Data Models

### ChatSession

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-increment | |
| user | User | FK, not null | 所属用户 |
| title | String | | 会话标题（AI 生成或首消息摘要） |
| createdAt | LocalDateTime | | 创建时间 |
| updatedAt | LocalDateTime | | 最后消息时间 |

### ChatMessage

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-increment | |
| session | ChatSession | FK, not null | 所属会话 |
| role | String | not null | USER / ASSISTANT / SYSTEM |
| content | String | not null, text | 消息内容 |
| createdAt | LocalDateTime | | 发送时间 |

**关系：**
- User 1:N ChatSession
- ChatSession 1:N ChatMessage (CascadeType.ALL, LAZY)

**索引：**
- `idx_chat_session_user_id` - 加速查询用户的会话列表
- `idx_chat_message_session_id_created_at` - 加速消息时间排序

## Function Calling

定义 4 个 Function 供 AI 调用：

### 1. GetUserPreferencesFunction

```java
// 获取用户口味偏好
输入: (无参数，从 SecurityContext 获取 userId)
输出: { tastePreferences: List<String> }
```

### 2. SearchRestaurantsFunction

```java
// 搜索附近餐厅
输入: { latitude, longitude, radius, cuisineType }
输出: List<RestaurantDto>
```

### 3. GetMealHistoryFunction

```java
// 获取用餐历史
输入: { startDate, endDate, limit }
输出: List<MealRecordDto>
```

### 4. CreateMealRecordFunction

```java
// 创建用餐记录
输入: { mealType, foodName, restaurantName, location, latitude, longitude, tags }
输出: MealRecordDto
```

## API Endpoints

### ChatController

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/chat` | 发送消息，SSE 流式返回 | JWT |
| GET | `/api/chat/sessions` | 获取用户所有会话列表 | JWT |
| GET | `/api/chat/sessions/{id}` | 获取会话详情（含历史消息） | JWT |
| DELETE | `/api/chat/sessions/{id}` | 删除会话 | JWT |

### AmapController (Admin)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/admin/amap/sync` | 手动触发高德地图同步 | Admin |

**请求/响应示例：**

**POST /api/chat**
```json
// Request
{
  "message": "今天中午想吃川菜，附近有什么推荐吗？",
  "sessionId": null
}

// SSE Response (流式)
data: {"type":"message","delta":"根据"}
data: {"type":"message","delta":"您的"}
data: {"type":"message","delta":"口味"}
...
data: {"type":"done","sessionId":123}
```

**GET /api/chat/sessions**
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "title": "川菜推荐",
      "createdAt": "2026-04-23T10:00:00",
      "messageCount": 8
    }
  ]
}
```

## Component Design

### ChatService

```java
@Service
public class ChatService {
    private final ChatClient chatClient;
    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;

    // 流式对话
    Flux<String> chat(Long userId, String message, Long sessionId);

    // 获取会话历史
    List<ChatMessageDto> getHistory(Long sessionId, Long userId);

    // 删除会话
    void deleteSession(Long sessionId, Long userId);

    // 生成会话标题
    String generateTitle(String firstMessage);
}
```

### AmapService

```java
@Service
public class AmapService {
    private final RestTemplate restTemplate;
    private final RestaurantRepository restaurantRepo;

    // 同步附近餐厅（定时任务）
    @Scheduled(cron = "${amap.sync.cron}")
    void syncNearbyRestaurants(double lat, double lng, int radius);

    // 调用高德 POI 搜索
    AmapPOIResponse searchPOI(String keywords, double lat, double lng, int radius);

    // 解析并保存 POI 数据
    void savePOIData(AmapPOIResponse response);
}
```

### Function Callbacks

所有 Function 实现 `java.util.function.Function` 接口：

```java
@Component
public class GetUserPreferencesFunction implements Function<GetUserPreferencesRequest, GetUserPreferencesResponse> {
    private final UserService userService;

    @Override
    public GetUserPreferencesResponse apply(GetUserPreferencesRequest request) {
        // 从 SecurityContext 获取当前用户
        // 返回用户偏好
    }
}
```

## Error Handling

| 场景 | 处理方式 |
|------|---------|
| AI 调用失败 | 返回友好错误消息，记录日志，不中断对话 |
| Function 执行失败 | AI 基于错误信息继续对话 |
| SSE 连接断开 | 客户端重连，消息已持久化可恢复 |
| Token 超时 | 401 错误，前端引导重新登录 |
| 会话不存在 | 返回 404，提示创建新对话 |

**新增 ErrorCode：**

| Code | Meaning |
|------|---------|
| 50002 | AI service unavailable |
| 50003 | Amap API error |
| 40003 | Invalid chat session |
| 40004 | Message too long |

## Configuration

**Maven 依赖（新增）：**

```xml
<!-- Spring AI OpenAI (兼容 DeepSeek) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M4</version>
</dependency>

<!-- WebFlux (用于 SSE) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**application.yml 新增配置：**

```yaml
spring:
  ai:
    openai:
      base-url: ${AI_BASE_URL:https://api.deepseek.com}
      api-key: ${AI_API_KEY:your-api-key}
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7

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

chat:
  history:
    max-messages: 50
  title:
    max-length: 20
```

## Project Structure

```
src/main/java/com/gd/mealmate/
├── controller/
│   ├── ChatController.java (新增)
│   └── AmapController.java (新增)
├── service/
│   ├── ChatService.java (新增)
│   └── AmapService.java (新增)
├── repository/
│   ├── ChatSessionRepository.java (新增)
│   └── ChatMessageRepository.java (新增)
├── model/entity/
│   ├── ChatSession.java (新增)
│   └── ChatMessage.java (新增)
├── dto/
│   ├── request/
│   │   └── ChatRequest.java (新增)
│   └── response/
│       ├── ChatSessionDto.java (新增)
│       └── ChatMessageDto.java (新增)
├── function/ (新增目录)
│   ├── GetUserPreferencesFunction.java
│   ├── SearchRestaurantsFunction.java
│   ├── GetMealHistoryFunction.java
│   └── CreateMealRecordFunction.java
├── config/
│   └── ChatConfig.java (新增 - ChatClient配置)
└── dto/amap/ (新增目录)
    ├── AmapPOIRequest.java
    ├── AmapPOIResponse.java
    └── AmapPOI.java
```

## Testing Strategy

**单元测试：**

| 组件 | 测试内容 |
|------|---------|
| ChatService | 对话创建、消息保存、上下文管理 |
| AmapService | API 调用、数据解析、去重逻辑 |
| Function Callbacks | 参数解析、Service 调用、返回值格式 |
| Repository | CRUD 操作、关联查询 |

**集成测试：**

| 场景 | 测试内容 |
|------|---------|
| 端到端对话 | 发送消息 → AI 调用 Function → 返回结果 |
| Function Calling | AI 正确识别意图并调用对应函数 |
| SSE 流式响应 | 验证流式数据格式和完整性 |
| 会话管理 | 创建会话、历史查询、删除会话 |

**Mock 策略：**
- Mock ChatClient（避免真实 API 调用）
- Mock AmapService（返回固定 POI 数据）
- 使用 H2 内存数据库进行测试

## Phase 2 Deliverables

1. ChatClient 集成完成，支持 DeepSeek 模型
2. 4 个 Function Callback 正确注册和调用
3. SSE 流式聊天端点正常工作
4. 对话历史持久化和查询
5. 高德地图 API 集成和数据同步
6. 完整的单元测试和集成测试
7. Swagger API 文档更新

## Future Phases

- **Phase 3:** Vue 3 + Element Plus 前端（聊天 UI、历史记录、设置页面）
