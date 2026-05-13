package com.gd.mealmate.controller;

import com.gd.mealmate.dto.request.FoodRecognitionRequest;
import com.gd.mealmate.dto.request.MealRecordRequest;
import com.gd.mealmate.dto.response.ApiResponse;
import com.gd.mealmate.dto.response.FoodRecognitionResponse;
import com.gd.mealmate.dto.response.MealRecordDto;
import com.gd.mealmate.model.enums.MealType;
import com.gd.mealmate.security.UserPrincipal;
import com.gd.mealmate.service.FoodRecognitionService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/meal-records")
@RequiredArgsConstructor
@Tag(name = "Meal Records", description = "Meal record management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class MealRecordController {

    private final MealRecordService mealRecordService;
    private final FoodRecognitionService foodRecognitionService;

    @PostMapping
    @Operation(summary = "Create meal record", description = "Creates a new meal record for the authenticated user")
    public ApiResponse<MealRecordDto> createMealRecord(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody MealRecordRequest request) {
        Long userId = userPrincipal.getUserId();
        MealRecordDto mealRecord = mealRecordService.createMealRecord(userId, request);
        return ApiResponse.success(mealRecord);
    }

    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Recognize food from photo", description = "Uploads a food photo, recognizes the dish, and auto-creates a meal record")
    public ApiResponse<FoodRecognitionResponse> recognizeFood(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("image") MultipartFile image,
            @Valid FoodRecognitionRequest request) {
        Long userId = userPrincipal.getUserId();
        FoodRecognitionResponse response = foodRecognitionService.recognize(image, request.getMealType(), userId);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(summary = "Get meal records", description = "Returns paginated list of user's meal records with optional filters")
    public ApiResponse<Page<MealRecordDto>> getMealRecords(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Start date for filtering (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for filtering (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Meal type filter")
            @RequestParam(required = false) MealType mealType,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = userPrincipal.getUserId();
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
        Page<MealRecordDto> mealRecords = mealRecordService.getMealRecords(userId, startDateTime, endDateTime, mealType, pageable);
        return ApiResponse.success(mealRecords);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get meal record by ID", description = "Returns a specific meal record owned by the authenticated user")
    public ApiResponse<MealRecordDto> getMealRecordById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Meal record ID") @PathVariable Long id) {
        Long userId = userPrincipal.getUserId();
        MealRecordDto mealRecord = mealRecordService.getMealRecordById(userId, id);
        return ApiResponse.success(mealRecord);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update meal record", description = "Updates an existing meal record owned by the authenticated user")
    public ApiResponse<MealRecordDto> updateMealRecord(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Meal record ID") @PathVariable Long id,
            @Valid @RequestBody MealRecordRequest request) {
        Long userId = userPrincipal.getUserId();
        MealRecordDto mealRecord = mealRecordService.updateMealRecord(userId, id, request);
        return ApiResponse.success(mealRecord);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete meal record", description = "Deletes a meal record owned by the authenticated user")
    public ApiResponse<Void> deleteMealRecord(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Meal record ID") @PathVariable Long id) {
        Long userId = userPrincipal.getUserId();
        mealRecordService.deleteMealRecord(userId, id);
        return ApiResponse.success();
    }
}
