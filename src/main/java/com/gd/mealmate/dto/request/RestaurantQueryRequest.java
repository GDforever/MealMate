package com.gd.mealmate.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantQueryRequest {
    private Double latitude;
    private Double longitude;
    private Double radius = 3000.0; // default 3km
    private String cuisineType;
    private String keyword;
}
