package com.gd.mealmate.function.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRestaurantsResponse {
    private List<RestaurantSimpleDto> restaurants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantSimpleDto {
        private Long id;
        private String name;
        private String address;
        private String cuisineType;
        private Double rating;
        private Double avgPrice;
    }
}
