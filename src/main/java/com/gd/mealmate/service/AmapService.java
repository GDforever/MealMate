package com.gd.mealmate.service;

import com.gd.mealmate.dto.amap.AmapPOI;
import com.gd.mealmate.dto.amap.AmapPOIResponse;
import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.exception.ErrorCode;
import com.gd.mealmate.model.entity.Restaurant;
import com.gd.mealmate.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AmapService {

    private final RestTemplate restTemplate;
    private final RestaurantRepository restaurantRepository;

    @Value("${amap.api-key}")
    private String apiKey;

    @Value("${amap.base-url}")
    private String baseUrl;

    @Value("${amap.sync.center.latitude}")
    private Double centerLatitude;

    @Value("${amap.sync.center.longitude}")
    private Double centerLongitude;

    @Value("${amap.sync.radius}")
    private Integer syncRadius;

    @Value("${amap.sync.enabled:true}")
    private boolean syncEnabled;

    public AmapPOIResponse searchPOI(String keywords, double lat, double lng, int radius) {
        String encodedKeywords = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
        String url = String.format(
                "%s/place/around?key=%s&location=%f,%f&radius=%d&keywords=%s&output=json",
                baseUrl, apiKey, lng, lat, radius, encodedKeywords
        );

        try {
            return restTemplate.getForObject(url, AmapPOIResponse.class);
        } catch (Exception e) {
            log.error("Amap API call failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AMAP_API_ERROR);
        }
    }

    @Scheduled(cron = "${amap.sync.cron:0 0 2 * * ?}")
    public void syncNearbyRestaurants() {
        if (!syncEnabled) {
            log.info("Amap sync is disabled");
            return;
        }

        log.info("Starting Amap restaurant sync at {}", LocalDateTime.now());

        String[] keywords = {"餐厅", "美食", "快餐", "小吃"};

        for (String keyword : keywords) {
            try {
                AmapPOIResponse response = searchPOI(keyword, centerLatitude, centerLongitude, syncRadius);

                if (response != null && response.isSuccess()) {
                    savePOIData(response);
                    log.info("Synced {} POIs for keyword: {}",
                            response.getPois() != null ? response.getPois().size() : 0, keyword);
                }
            } catch (Exception e) {
                log.error("Failed to sync keyword {}: {}", keyword, e.getMessage());
            }
        }

        log.info("Amap restaurant sync completed");
    }

    public void savePOIData(AmapPOIResponse response) {
        if (response.getPois() == null) {
            return;
        }

        for (AmapPOI poi : response.getPois()) {
            if (restaurantRepository.findByExternalId(poi.getId()).isEmpty()) {
                Restaurant restaurant = new Restaurant();
                restaurant.setName(poi.getName());
                restaurant.setAddress(poi.getAddress());
                restaurant.setLatitude(poi.getLocationLat());
                restaurant.setLongitude(poi.getLocationLng());
                restaurant.setSource("AMAP");
                restaurant.setExternalId(poi.getId());
                restaurant.setCuisineType(extractCuisineType(poi.getType()));
                restaurant.setCreatedAt(LocalDateTime.now());

                restaurantRepository.save(restaurant);
            }
        }
    }

    private String extractCuisineType(String type) {
        if (type == null) {
            return "其他";
        }

        if (type.contains("中餐") || type.contains("川菜") || type.contains("粤菜")) {
            return "中餐";
        } else if (type.contains("西餐")) {
            return "西餐";
        } else if (type.contains("日料") || type.contains("日本")) {
            return "日料";
        } else if (type.contains("韩餐") || type.contains("韩国")) {
            return "韩餐";
        } else {
            return "其他";
        }
    }
}
