package com.gd.mealmate.dto.response;

import com.gd.mealmate.model.enums.MealType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealRecordDto {
    private Long id;
    private Long userId;
    private MealType mealType;
    private String foodName;
    private String restaurantName;
    private String location;
    private Double latitude;
    private Double longitude;
    private LocalDateTime recordedAt;
    private Integer userRating;
    private String tags;
    private String imageUrl;
    private String source;
    private LocalDateTime createdAt;
}
