package com.gd.mealmate.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.exception.ErrorCode;
import com.gd.mealmate.function.dto.SaveUserPreferencesRequest;
import com.gd.mealmate.model.entity.User;
import com.gd.mealmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Component
@Description("保存用户的口味偏好，支持增量添加新偏好")
@Slf4j
@RequiredArgsConstructor
public class SaveUserPreferencesFunction implements Function<SaveUserPreferencesRequest, String> {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(SaveUserPreferencesRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        Set<String> merged = parsePreferences(user.getTastePreferences());

        boolean added = false;
        if (request.getTastePreferences() != null && !request.getTastePreferences().isBlank()) {
            for (String pref : request.getTastePreferences().split("[,，;；]+")) {
                String trimmed = pref.trim();
                if (!trimmed.isEmpty() && merged.add(trimmed)) {
                    added = true;
                }
            }
        }

        if (added) {
            String mergedStr = String.join(",", merged);
            if (mergedStr.length() > 1000) {
                mergedStr = mergedStr.substring(0, 1000);
            }
            user.setTastePreferences(mergedStr);
            userRepository.save(user);
        }

        try {
            var response = new java.util.LinkedHashMap<String, Object>();
            response.put("success", true);
            response.put("userId", user.getId());
            response.put("tastePreferences", List.copyOf(merged));
            response.put("message", "口味偏好已更新");
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response: {}", e.getMessage());
            return "{\"error\": \"Failed to save preferences\"}";
        }
    }

    private Set<String> parsePreferences(String raw) {
        Set<String> result = new LinkedHashSet<>();
        if (raw != null && !raw.isBlank()) {
            for (String pref : raw.split("[,，;；]+")) {
                String trimmed = pref.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }
}
