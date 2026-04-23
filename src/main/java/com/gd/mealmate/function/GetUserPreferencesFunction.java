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
