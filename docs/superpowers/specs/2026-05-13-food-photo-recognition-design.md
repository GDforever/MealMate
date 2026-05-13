# Food Photo Recognition Design

## Overview

Add food photo recognition to MealMate, allowing users to upload/take food photos and automatically create meal records with dish name and calorie estimates. Uses DeepSeek Vision (multimodal) via existing Spring AI infrastructure.

## Requirements

- Recognize dish name + estimate calories from food photos
- Two entry points: standalone photo upload in meal records page + image sending in chat
- Auto-create meal records without user confirmation
- Meal type (breakfast/lunch/dinner/snack) determined by user input or current time, not from photo analysis

## Backend Architecture

### New/Modified Files

| File | Change |
|------|--------|
| `service/FoodRecognitionService` | New. Core service: receive image, call DeepSeek Vision, parse result, auto-create record |
| `controller/MealRecordController` | Add `POST /api/meal-records/recognize` endpoint |
| `controller/ChatController` | Modify to accept multipart/form-data with optional image |
| `service/ChatService` | Add image detection branch, call FoodRecognitionService, inject result into context |
| `dto/request/FoodRecognitionRequest` | New. Contains image file + mealType |
| `dto/response/FoodRecognitionResponse` | New. Contains foodName, calories, confidence, mealRecordId |
| `model/entity/ChatMessage` | Add `imageUrl` field |
| `model/enums/RecordSource` | New enum: CHAT, PHOTO, MANUAL |
| `model/entity/MealRecord` | Add `imageUrl` and `source` fields |

### FoodRecognitionService Logic

```
recognize(MultipartFile image, MealType mealType, Long userId)
  -> Save image to uploads/food/{userId}/{uuid}.{ext}
  -> Convert image to Base64
  -> Build multimodal prompt with system instruction
  -> Call Spring AI OpenAiChatModel (DeepSeek Vision)
  -> Parse JSON response: { foodName, calories, confidence }
  -> If confidence >= 0.5: call MealRecordService.createMealRecord()
  -> Return FoodRecognitionResponse
```

### Chat Image Handling

- Chat endpoint changes to `multipart/form-data`: `message` (text) + `image` (optional file)
- When image detected: ChatService calls FoodRecognitionService first, then injects recognition result as system message into conversation context
- AI replies with recognition result in streaming response
- Auto-creates meal record; mealType determined by user text or current time

### Vision Prompt

System prompt (Chinese):

```
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
```

### MealType Determination

- **Standalone photo entry**: Frontend provides meal type selector with smart default based on current time (6-9am -> BREAKFAST, 11am-1pm -> LUNCH, 5-7pm -> DINNER, otherwise -> SNACK). User can override.
- **Chat scenario**: AI infers from user text (e.g. "这是我今天的午餐") or falls back to current time.

## Database Changes

```sql
-- chat_messages: add image support
ALTER TABLE chat_messages ADD COLUMN image_url VARCHAR(500);

-- meal_records: add source tracking and image reference
ALTER TABLE meal_records ADD COLUMN image_url VARCHAR(500);
ALTER TABLE meal_records ADD COLUMN source VARCHAR(20) DEFAULT 'CHAT';
```

JPA `ddl-auto: update` will handle schema migration.

## Frontend Design

### Standalone Photo Entry (Meal Records Page)

- Add floating action button "拍照记录" on meal records page
- Tap -> options: take photo / choose from gallery
- Preview screen: thumbnail + meal type selector (smart default by time) + confirm button
- On confirm: call `POST /api/meal-records/recognize`, show loading
- On success: return to meal records list, highlight new record

### Chat Image Support

- Add image picker button next to chat input (image icon)
- Selected image shows as thumbnail above input box
- User can type text + attach image together
- Sent message renders as image bubble in chat
- AI streaming reply includes recognition result text

### Frontend Files

| File | Change |
|------|--------|
| `components/chat/ChatInput.vue` | Add image picker button and preview |
| `components/chat/ChatMessage.vue` | Support image bubble rendering |
| `views/meal/MealRecords.vue` | Add photo action button |
| `views/meal/FoodCamera.vue` | New page: photo/preview/meal type selection |
| `api/meal.ts` | Add `recognizeFood` API method |
| `api/chat.ts` | Modify to support multipart upload |
| `types/index.ts` | Add FoodRecognitionResponse type, MealRecord source/imageUrl fields |

## Error Handling

| Scenario | Handling |
|----------|----------|
| No food in photo | Return "未检测到食物" message, no record created |
| Multiple dishes | Recognize main dish, AI follows up in chat if needed |
| Blurry/dark photo | Attempt recognition, if confidence < 0.5 prompt user to retake or record manually |
| Unrecognized food | Return "无法识别该食物" |
| Image too large (>10MB) | Frontend compresses via Canvas API, backend enforces 10MB limit |

### Chat Edge Cases

- Image without text: recognize and record directly, AI replies with result
- Image + text (e.g. "这是早餐"): recognize image + use text for meal type
- Multiple images in sequence: process each individually, create separate records

## Security

- Image format whitelist: JPG, PNG, WEBP
- Images stored under `uploads/food/{userId}/`, user-isolated
- All endpoints protected by JWT authentication
- Users can only access their own images

## Technical Notes

- DeepSeek Vision called via existing Spring AI `OpenAiChatModel` configuration, image as Base64-encoded `Media` in `UserMessage`
- Image storage follows same pattern as KnowledgeBase file uploads
- Frontend image compression uses native Canvas API, no extra dependencies
