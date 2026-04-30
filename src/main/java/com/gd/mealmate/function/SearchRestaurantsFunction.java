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
    private static final int[] FALLBACK_RADII = {5000, 10000, 20000};

    private final RestaurantService restaurantService;
    private final AmapService amapService;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(SearchRestaurantsRequest request) {
        try {
            log.info("========== [搜索餐厅] 开始 ==========");
            log.info("[搜索餐厅] 请求参数: latitude={}, longitude={}, radius={}, cuisineType={}, keywords={}",
                    request.getLatitude(), request.getLongitude(), request.getRadius(),
                    request.getCuisineType(), request.getKeywords());

            double radius = request.getRadius() != null ? request.getRadius().doubleValue() : 3000.0;
            Pageable pageable = PageRequest.of(0, 50);
            RestaurantQueryRequest queryRequest = new RestaurantQueryRequest(
                    request.getLatitude(),
                    request.getLongitude(),
                    radius,
                    request.getCuisineType()
            );

            log.info("[搜索餐厅] 实际查询参数: latitude={}, longitude={}, radius={}米, cuisineType={}",
                    request.getLatitude(), request.getLongitude(), radius, request.getCuisineType());

            // Step 1: search local database
            log.info("[搜索餐厅] Step 1: 查询本地数据库...");
            Page<RestaurantDto> localResults = restaurantService.findNearby(queryRequest, pageable);
            List<RestaurantDto> allResults = new ArrayList<>(localResults.getContent());
            log.info("[搜索餐厅] 本地数据库返回 {} 家餐厅 (总记录数: {})", allResults.size(), localResults.getTotalElements());
            for (int i = 0; i < Math.min(5, allResults.size()); i++) {
                RestaurantDto r = allResults.get(i);
                log.info("[搜索餐厅]   本地结果[{}]: id={}, name={}, address={}, cuisineType={}, lat={}, lng={}",
                        i, r.getId(), r.getName(), r.getAddress(), r.getCuisineType(), r.getLatitude(), r.getLongitude());
            }

            // Step 2: if not enough results, call AMap API with progressively larger radius
            if (allResults.size() < MIN_RESULTS_THRESHOLD && request.getLatitude() != null && request.getLongitude() != null) {
                log.info("[搜索餐厅] 本地结果不足 {} 条，尝试调用高德API补充", MIN_RESULTS_THRESHOLD);
                String keywords = request.getKeywords() != null ? request.getKeywords() : "美食";

                // Try original radius first, then fallback to larger radii
                List<Double> radiiToTry = new ArrayList<>();
                radiiToTry.add(radius);
                for (int fallback : FALLBACK_RADII) {
                    if (fallback > radius) {
                        radiiToTry.add((double) fallback);
                    }
                }

                for (int i = 0; i < radiiToTry.size(); i++) {
                    double tryRadius = radiiToTry.get(i);
                    log.info("[搜索餐厅] Step 2: 调用高德API (尝试 {}/{}, radius={}米), keywords={}, lat={}, lng={}",
                            i + 1, radiiToTry.size(), (int) tryRadius, keywords, request.getLatitude(), request.getLongitude());
                    try {
                        AmapPOIResponse amapResponse = amapService.searchPOI(
                                keywords, request.getLatitude(), request.getLongitude(), (int) tryRadius
                        );
                        log.info("[搜索餐厅] 高德API响应: status={}, info={}, count={}, pois={}",
                                amapResponse != null ? amapResponse.getStatus() : "null",
                                amapResponse != null ? amapResponse.getInfo() : "null",
                                amapResponse != null ? amapResponse.getCount() : "null",
                                amapResponse != null && amapResponse.getPois() != null ? amapResponse.getPois().size() : 0);

                        if (amapResponse != null && amapResponse.isSuccess() && amapResponse.getPois() != null && !amapResponse.getPois().isEmpty()) {
                            for (int j = 0; j < Math.min(5, amapResponse.getPois().size()); j++) {
                                var poi = amapResponse.getPois().get(j);
                                log.info("[搜索餐厅]   高德POI[{}]: id={}, name={}, address={}, location={}, type={}",
                                        j, poi.getId(), poi.getName(), poi.getAddress(), poi.getLocation(), poi.getType());
                            }
                            amapService.savePOIData(amapResponse);
                            log.info("[搜索餐厅] 高德返回 {} 个POI，已保存到数据库", amapResponse.getPois().size());

                            // Re-query with the expanded radius that actually found results
                            RestaurantQueryRequest expandedQuery = new RestaurantQueryRequest(
                                    request.getLatitude(), request.getLongitude(), tryRadius, request.getCuisineType()
                            );
                            log.info("[搜索餐厅] Step 3: 用 radius={}米 重新查询本地数据库...", (int) tryRadius);
                            Page<RestaurantDto> refreshed = restaurantService.findNearby(expandedQuery, pageable);
                            allResults = new ArrayList<>(refreshed.getContent());
                            log.info("[搜索餐厅] 重新查询后返回 {} 家餐厅", allResults.size());
                            break;
                        } else {
                            log.info("[搜索餐厅] 高德API在 {}米 范围内未找到结果，尝试更大半径", (int) tryRadius);
                        }
                    } catch (Exception e) {
                        log.warn("[搜索餐厅] 高德API调用失败(radius={}米): {}", (int) tryRadius, e.getMessage(), e);
                    }
                }
            } else if (allResults.size() >= MIN_RESULTS_THRESHOLD) {
                log.info("[搜索餐厅] 本地结果充足({}条)，跳过高德API调用", allResults.size());
            } else {
                log.warn("[搜索餐厅] 经纬度为空(latitude={}, longitude={})，跳过高德API调用",
                        request.getLatitude(), request.getLongitude());
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
            String result = objectMapper.writeValueAsString(response);
            log.info("[搜索餐厅] 最终返回 {} 家餐厅, JSON长度={}", simpleRestaurants.size(), result.length());
            log.info("========== [搜索餐厅] 结束 ==========");
            return result;

        } catch (JsonProcessingException e) {
            log.error("[搜索餐厅] 序列化响应失败: {}", e.getMessage(), e);
            return "{\"error\": \"Failed to search restaurants\"}";
        }
    }
}
