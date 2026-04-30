package com.gd.mealmate.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gd.mealmate.dto.amap.AmapPOI;
import com.gd.mealmate.dto.amap.AmapPOIResponse;
import com.gd.mealmate.dto.request.RestaurantQueryRequest;
import com.gd.mealmate.dto.response.RestaurantDto;
import com.gd.mealmate.function.dto.SearchRestaurantsRequest;
import com.gd.mealmate.function.dto.SearchRestaurantsResponse;
import com.gd.mealmate.service.AmapService;
import com.gd.mealmate.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
@Description("搜索附近的餐厅，支持按菜系类型过滤")
@Slf4j
@RequiredArgsConstructor
public class SearchRestaurantsFunction implements Function<SearchRestaurantsRequest, String> {

    private static final int MIN_RESULTS_THRESHOLD = 5;

    private final RestaurantService restaurantService;
    private final AmapService amapService;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(SearchRestaurantsRequest request) {
        try {
            double radius = request.getRadius() != null ? request.getRadius().doubleValue() : 3000.0;
            Pageable pageable = PageRequest.of(0, 50);
            RestaurantQueryRequest queryRequest = new RestaurantQueryRequest(
                    request.getLatitude(),
                    request.getLongitude(),
                    radius,
                    request.getCuisineType()
            );

            // Step 1: search local database
            Page<RestaurantDto> localResults = restaurantService.findNearby(queryRequest, pageable);
            List<RestaurantDto> allResults = new ArrayList<>(localResults.getContent());
            log.info("Local database returned {} restaurants", allResults.size());

            // Step 2: if not enough results, call AMap API
            if (allResults.size() < MIN_RESULTS_THRESHOLD && request.getLatitude() != null && request.getLongitude() != null) {
                String keywords = request.getKeywords() != null ? request.getKeywords() : "美食";
                try {
                    AmapPOIResponse amapResponse = amapService.searchPOI(
                            keywords, request.getLatitude(), request.getLongitude(), (int) radius
                    );
                    if (amapResponse != null && amapResponse.isSuccess() && amapResponse.getPois() != null) {
                        amapService.savePOIData(amapResponse);
                        log.info("AMap returned {} POIs, saved to database", amapResponse.getPois().size());

                        // Re-query local database after saving AMap data
                        if (allResults.isEmpty()) {
                            Page<RestaurantDto> refreshed = restaurantService.findNearby(queryRequest, pageable);
                            allResults = new ArrayList<>(refreshed.getContent());
                        }
                    }
                } catch (Exception e) {
                    log.warn("AMap search failed, returning local results only: {}", e.getMessage());
                }
            }

            var simpleRestaurants = allResults.stream()
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
