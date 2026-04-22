package com.gd.mealmate.service;

import com.gd.mealmate.dto.request.RestaurantQueryRequest;
import com.gd.mealmate.dto.response.RestaurantDto;
import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.exception.ErrorCode;
import com.gd.mealmate.mapper.RestaurantMapper;
import com.gd.mealmate.model.entity.Restaurant;
import com.gd.mealmate.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;

    public Page<RestaurantDto> findNearby(RestaurantQueryRequest request, Pageable pageable) {
        Page<Restaurant> restaurants = restaurantRepository.findNearby(
                request.getLatitude(),
                request.getLongitude(),
                request.getRadius(),
                request.getCuisineType(),
                pageable
        );
        return restaurants.map(restaurantMapper::toDto);
    }

    public RestaurantDto getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return restaurantMapper.toDto(restaurant);
    }
}
