package com.gd.mealmate.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gd.mealmate.dto.request.MealRecordRequest;
import com.gd.mealmate.dto.response.MealRecordDto;
import com.gd.mealmate.service.MealRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
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
        try {
            MealRecordDto record = mealRecordService.createMealRecord(request.getUserId(), request);
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response: {}", e.getMessage());
            return "{\"error\": \"Failed to create meal record\"}";
        }
    }
}
