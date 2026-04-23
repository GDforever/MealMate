package com.gd.mealmate.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gd.mealmate.dto.request.RestaurantQueryRequest;
import com.gd.mealmate.dto.response.RestaurantDto;
import com.gd.mealmate.function.dto.SearchRestaurantsRequest;
import com.gd.mealmate.function.dto.SearchRestaurantsResponse;
import com.gd.mealmate.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
@Description("搜索附近的餐厅，支持按菜系类型过滤")
@Slf4j
@RequiredArgsConstructor
public class SearchRestaurantsFunction implements Function<SearchRestaurantsRequest, String> {

    private final RestaurantService restaurantService;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(SearchRestaurantsRequest request) {
        try {
            RestaurantQueryRequest queryRequest = new RestaurantQueryRequest(
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getRadius() != null ? request.getRadius().doubleValue() : 3000.0,
                    request.getCuisineType()
            );

            Pageable pageable = PageRequest.of(0, 50);
            Page<RestaurantDto> restaurantPage = restaurantService.findNearby(queryRequest, pageable);

            var simpleRestaurants = restaurantPage.getContent().stream()
                    .map(r -> new SearchRestaurantsResponse.RestaurantSimpleDto(
                            r.getId(),
                            r.getName(),
                            r.getAddress(),
                            r.getCuisineType(),
                            r.getRating(),
                            r.getAvgPrice() != null ? r.getAvgPrice().doubleValue() : null
                    ))
                    .toList();

            SearchRestaurantsResponse response = new SearchRestaurantsResponse(simpleRestaurants);
            return objectMapper.writeValueAsString(response);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response: {}", e.getMessage());
            return "{\"error\": \"Failed to search restaurants\"}";
        }
    }
}
