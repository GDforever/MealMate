package com.gd.mealmate.function.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRestaurantsRequest {
    private Double latitude;
    private Double longitude;
    private Integer radius;
    private String cuisineType;
}
