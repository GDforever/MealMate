package com.gd.mealmate.service;

import com.gd.mealmate.dto.amap.AmapPOI;
import com.gd.mealmate.dto.request.RestaurantQueryRequest;
import java.util.List;
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

    private static final int MIN_RESULTS_THRESHOLD = 5;
    private static final int[] FALLBACK_RADII = {5000, 10000, 20000};

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;
    private final AmapService amapService;

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
                request.getKeyword(),
                pageable
        );

        log.info("[餐厅查询] 数据库返回: 当前页{}条, 总计{}条, 总页数{}",
                restaurants.getNumberOfElements(), restaurants.getTotalElements(), restaurants.getTotalPages());

        // 本地结果不足时，调用高德API全量拉取并保存
        if (restaurants.getTotalElements() < MIN_RESULTS_THRESHOLD
                && request.getLatitude() != null && request.getLongitude() != null) {
            log.info("[餐厅查询] 本地结果不足{}条，调用高德API全量补充", MIN_RESULTS_THRESHOLD);
            double radius = request.getRadius() != null ? request.getRadius() : 3000.0;

            for (int tryRadius : FALLBACK_RADII) {
                if (tryRadius < radius) continue;
                try {
                    List<AmapPOI> allPois = amapService.searchAllPOIs(
                            "美食", request.getLatitude(), request.getLongitude(), tryRadius
                    );
                    if (!allPois.isEmpty()) {
                        amapService.saveAllPOIs(allPois);
                        log.info("[餐厅查询] 高德API全量获取{}个POI，已upsert保存，用radius={}米重新查询",
                                allPois.size(), tryRadius);

                        RestaurantQueryRequest expandedQuery = new RestaurantQueryRequest(
                                request.getLatitude(), request.getLongitude(), (double) tryRadius,
                                request.getCuisineType(), request.getKeyword()
                        );
                        restaurants = restaurantRepository.findNearby(
                                expandedQuery.getLatitude(), expandedQuery.getLongitude(),
                                expandedQuery.getRadius(), expandedQuery.getCuisineType(),
                                request.getKeyword(), pageable
                        );
                        log.info("[餐厅查询] 重新查询后: 当前页{}条, 总计{}条",
                                restaurants.getNumberOfElements(), restaurants.getTotalElements());
                        break;
                    }
                } catch (Exception e) {
                    log.warn("[餐厅查询] 高德API调用失败(radius={}米): {}", tryRadius, e.getMessage());
                }
            }
        }

        return restaurants.map(restaurantMapper::toDto);
    }

    public RestaurantDto getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return restaurantMapper.toDto(restaurant);
    }
}
