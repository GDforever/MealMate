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
import java.nio.file.Paths;
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

    @Value("${app.vision.base-url}")
    private String visionBaseUrl;

    @Value("${app.vision.api-key}")
    private String visionApiKey;

    @Value("${app.vision.model}")
    private String visionModel;

    @Value("${app.vision.max-tokens:2048}")
    private int visionMaxTokens;

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
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

    public FoodRecognitionResponse recognize(MultipartFile image, MealType mealType, Long userId) {
        log.info("[recognize] 开始识别: userId={}, mealType={}, imageSize={}, contentType={}",
                userId, mealType, image != null ? image.getSize() : 0, image != null ? image.getContentType() : "null");
        validateImage(image);

        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_RECOGNITION_FAILED, "图片读取失败");
        }

        String imageUrl = saveImage(image, userId);
        log.info("[recognize] 图片已保存: {}", imageUrl);

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String contentType = image.getContentType();
        String dataUrl = "data:" + contentType + ";base64," + base64Image;
        log.info("[recognize] base64编码长度: {}", base64Image.length());

        String aiResponse = callVisionApi(dataUrl);
        log.info("[recognize] AI原始响应: {}", aiResponse);

        FoodRecognitionResponse result = parseResponse(aiResponse);
        log.info("[recognize] 解析结果: foodName={}, calories={}, confidence={}",
                result.getFoodName(), result.getCalories(), result.getConfidence());
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

        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_RECOGNITION_FAILED, "图片读取失败");
        }

        String imageUrl = saveImage(image, userId);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

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
            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path dirPath = basePath.resolve("food").resolve(String.valueOf(userId));
            Files.createDirectories(dirPath);

            String contentType = image.getContentType();
            String extension = switch (contentType != null ? contentType : "image/jpeg") {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };
            String filename = UUID.randomUUID() + extension;
            Path filePath = dirPath.resolve(filename);
            image.transferTo(filePath.toFile());

            return "/api/images/food/" + userId + "/" + filename;
        } catch (IOException e) {
            log.error("Failed to save image: {}", e.getMessage());
            throw new BusinessException(ErrorCode.IMAGE_RECOGNITION_FAILED, "图片保存失败");
        }
    }

    private String callVisionApi(String base64DataUrl) {
        String url = visionBaseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(visionApiKey);

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
                "max_tokens", visionMaxTokens
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("[callVisionApi] 请求URL: {}, model: {}", url, visionModel);
            log.info("[callVisionApi] 开始调用API...");
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            log.info("[callVisionApi] 响应状态: {}, body长度: {}", response.getStatusCode(), response.getBody() != null ? response.getBody().length() : 0);
            log.debug("[callVisionApi] 响应body: {}", response.getBody());
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
            log.info("[parseResponse] 输入: {}", aiResponse);
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
