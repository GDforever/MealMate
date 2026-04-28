package com.gd.mealmate.controller;

import com.gd.mealmate.dto.request.MealRecordRequest;
import com.gd.mealmate.dto.response.ApiResponse;
import com.gd.mealmate.dto.response.MealRecordDto;
import com.gd.mealmate.model.enums.MealType;
import com.gd.mealmate.service.MealRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/meal-records")
@RequiredArgsConstructor
@Tag(name = "Meal Records", description = "Meal record management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class MealRecordController {

    private final MealRecordService mealRecordService;

    @PostMapping
    @Operation(summary = "Create meal record", description = "Creates a new meal record for the authenticated user")
    public ApiResponse<MealRecordDto> createMealRecord(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MealRecordRequest request) {
        MealRecordDto mealRecord = mealRecordService.createMealRecord(userId, request);
        return ApiResponse.success(mealRecord);
    }

    @GetMapping
    @Operation(summary = "Get meal records", description = "Returns paginated list of user's meal records with optional filters")
    public ApiResponse<Page<MealRecordDto>> getMealRecords(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "Start date for filtering (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for filtering (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Meal type filter")
            @RequestParam(required = false) MealType mealType,
            @PageableDefault(size = 20) Pageable pageable) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
        Page<MealRecordDto> mealRecords = mealRecordService.getMealRecords(userId, startDateTime, endDateTime, mealType, pageable);
        return ApiResponse.success(mealRecords);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get meal record by ID", description = "Returns a specific meal record owned by the authenticated user")
    public ApiResponse<MealRecordDto> getMealRecordById(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "Meal record ID") @PathVariable Long id) {
        MealRecordDto mealRecord = mealRecordService.getMealRecordById(userId, id);
        return ApiResponse.success(mealRecord);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update meal record", description = "Updates an existing meal record owned by the authenticated user")
    public ApiResponse<MealRecordDto> updateMealRecord(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "Meal record ID") @PathVariable Long id,
            @Valid @RequestBody MealRecordRequest request) {
        MealRecordDto mealRecord = mealRecordService.updateMealRecord(userId, id, request);
        return ApiResponse.success(mealRecord);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete meal record", description = "Deletes a meal record owned by the authenticated user")
    public ApiResponse<Void> deleteMealRecord(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "Meal record ID") @PathVariable Long id) {
        mealRecordService.deleteMealRecord(userId, id);
        return ApiResponse.success();
    }
}
