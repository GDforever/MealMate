package com.gd.mealmate.service;

import com.gd.mealmate.dto.request.RestaurantQueryRequest;
import com.gd.mealmate.dto.response.RestaurantDto;
import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.exception.ErrorCode;
import com.gd.mealmate.mapper.RestaurantMapper;
import com.gd.mealmate.model.entity.Restaurant;
import com.gd.mealmate.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;

    public Page<RestaurantDto> findNearby(RestaurantQueryRequest request, Pageable pageable) {
        log.info("[餐厅查询] 开始查询: lat={}, lng={}, radius={}米, cuisineType={}, page={}, size={}",
                request.getLatitude(), request.getLongitude(), request.getRadius(),
                request.getCuisineType(), pageable.getPageNumber(), pageable.getPageSize());

        if (request.getLatitude() == null || request.getLongitude() == null) {
            log.warn("[餐厅查询] 经纬度为空! latitude={}, longitude={}，查询可能返回空结果",
                    request.getLatitude(), request.getLongitude());
        }

        Page<Restaurant> restaurants = restaurantRepository.findNearby(
                request.getLatitude(),
                request.getLongitude(),
                request.getRadius(),
                request.getCuisineType(),
                pageable
        );

        log.info("[餐厅查询] 数据库返回: 当前页{}条, 总计{}条, 总页数{}",
                restaurants.getNumberOfElements(), restaurants.getTotalElements(), restaurants.getTotalPages());
        return restaurants.map(restaurantMapper::toDto);
    }

    public RestaurantDto getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return restaurantMapper.toDto(restaurant);
    }
}
