package com.gd.mealmate.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gd.mealmate.dto.response.MealRecordDto;
import com.gd.mealmate.function.dto.GetMealHistoryRequest;
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

        Page<MealRecordDto> records = mealRecordService.getMealRecords(
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
