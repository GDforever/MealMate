package com.gd.mealmate.controller;

import com.gd.mealmate.dto.request.RestaurantQueryRequest;
import com.gd.mealmate.dto.response.ApiResponse;
import com.gd.mealmate.dto.response.RestaurantDto;
import com.gd.mealmate.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurants", description = "Restaurant discovery endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping("/nearby")
    @Operation(summary = "Find nearby restaurants", description = "Returns paginated list of nearby restaurants based on location and optional filters")
    public ApiResponse<Page<RestaurantDto>> findNearby(
            @Valid RestaurantQueryRequest request,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<RestaurantDto> restaurants = restaurantService.findNearby(request, pageable);
        return ApiResponse.success(restaurants);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant by ID", description = "Returns details of a specific restaurant")
    public ApiResponse<RestaurantDto> getRestaurantById(
            @Parameter(description = "Restaurant ID") @PathVariable Long id) {
        RestaurantDto restaurant = restaurantService.getRestaurantById(id);
        return ApiResponse.success(restaurant);
    }
}
