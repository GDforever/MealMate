package com.gd.mealmate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDto {
    private Long id;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String cuisineType;
    private BigDecimal avgPrice;
    private Double rating;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
