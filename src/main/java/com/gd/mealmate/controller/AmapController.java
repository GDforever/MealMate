package com.gd.mealmate.controller;

import com.gd.mealmate.dto.response.ApiResponse;
import com.gd.mealmate.service.AmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/amap")
@RequiredArgsConstructor
@Tag(name = "Admin - Amap", description = "高德地图管理接口")
@SecurityRequirement(name = "bearerAuth")
public class AmapController {

    private final AmapService amapService;

    @PostMapping("/sync")
    @Operation(summary = "手动触发高德地图餐厅数据同步")
    public ApiResponse<String> triggerSync() {
        amapService.syncNearbyRestaurants();
        return ApiResponse.success("同步任务已触发");
    }
}
