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
