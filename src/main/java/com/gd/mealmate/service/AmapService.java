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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private static final int AMAP_PAGE_SIZE = 25;
    private static final int AMAP_MAX_PAGES = 45;

    public AmapPOIResponse searchPOI(String keywords, double lat, double lng, int radius) {
        return searchPOI(keywords, lat, lng, radius, 0, AMAP_PAGE_SIZE);
    }

    public AmapPOIResponse searchPOI(String keywords, double lat, double lng, int radius, int offset, int limit) {
        String encodedKeywords = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
        String url = String.format(
                "%s/place/around?key=%s&location=%f,%f&radius=%d&keywords=%s&types=050000&offset=%d&limit=%d&output=json",
                baseUrl, apiKey, lng, lat, radius, encodedKeywords, offset, limit
        );

        String maskedUrl = url.replace(apiKey, apiKey.substring(0, Math.min(6, apiKey.length())) + "***");
        log.info("[高德API] 请求URL: {}, offset={}, limit={}", maskedUrl, offset, limit);

        try {
            AmapPOIResponse response = restTemplate.getForObject(new URI(url), AmapPOIResponse.class);
            if (response != null) {
                log.info("[高德API] 响应: status={}, count={}, pois数量={}, offset={}",
                        response.getStatus(), response.getCount(),
                        response.getPois() != null ? response.getPois().size() : 0, offset);
            }
            return response;
        } catch (Exception e) {
            log.error("[高德API] 调用失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AMAP_API_ERROR);
        }
    }

    /**
     * 分页遍历高德API所有结果页，返回全部POI
     */
    public List<AmapPOI> searchAllPOIs(String keywords, double lat, double lng, int radius) {
        List<AmapPOI> allPois = new ArrayList<>();
        int total = -1;

        for (int page = 1; page <= AMAP_MAX_PAGES; page++) {
            int offset = (page - 1) * AMAP_PAGE_SIZE;
            AmapPOIResponse response = searchPOI(keywords, lat, lng, radius, offset, AMAP_PAGE_SIZE);

            if (response == null || !response.isSuccess() || response.getPois() == null || response.getPois().isEmpty()) {
                break;
            }

            allPois.addAll(response.getPois());

            if (total < 0 && response.getCount() != null) {
                total = Integer.parseInt(response.getCount());
            }

            log.info("[高德API] 已获取第{}/{}页, 累计{}条POI", page,
                    total > 0 ? (int) Math.ceil((double) total / AMAP_PAGE_SIZE) : "?",
                    allPois.size());

            if (total >= 0 && allPois.size() >= total) {
                break;
            }
        }

        log.info("[高德API] 全量获取完成, 共{}条POI", allPois.size());
        return allPois;
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
                List<AmapPOI> allPois = searchAllPOIs(keyword, centerLatitude, centerLongitude, syncRadius);
                saveAllPOIs(allPois);
                log.info("Synced {} POIs for keyword: {}", allPois.size(), keyword);
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
        saveAllPOIs(response.getPois());
    }

    /**
     * Upsert逻辑：按externalId(高德POI ID)查找，存在则更新，不存在则插入
     */
    public void saveAllPOIs(List<AmapPOI> pois) {
        log.info("[高德API] saveAllPOIs: 开始处理 {} 个POI", pois.size());
        int inserted = 0;
        int updated = 0;
        int failed = 0;

        for (AmapPOI poi : pois) {
            try {
                Restaurant restaurant = restaurantRepository.findByExternalId(poi.getId())
                        .orElseGet(Restaurant::new);

                boolean isNew = (restaurant.getId() == null);

                restaurant.setName(poi.getName());
                restaurant.setAddress(poi.getAddressAsString());
                restaurant.setLatitude(poi.getLocationLat());
                restaurant.setLongitude(poi.getLocationLng());
                restaurant.setSource("AMAP");
                restaurant.setExternalId(poi.getId());
                restaurant.setCuisineType(extractCuisineType(poi.getType()));
                restaurant.setPhotoUrl(poi.getFirstPhotoUrl());

                restaurantRepository.save(restaurant);

                if (isNew) {
                    inserted++;
                } else {
                    updated++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("[高德API] 保存POI失败: name={}, id={}, error={}", poi.getName(), poi.getId(), e.getMessage());
            }
        }

        log.info("[高德API] saveAllPOIs完成: 新增={}, 更新={}, 失败={}", inserted, updated, failed);
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
