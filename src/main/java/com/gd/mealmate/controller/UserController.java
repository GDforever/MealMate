package com.gd.mealmate.controller;

import com.gd.mealmate.dto.request.UpdatePreferencesRequest;
import com.gd.mealmate.dto.response.ApiResponse;
import com.gd.mealmate.dto.response.UserDto;
import com.gd.mealmate.security.UserPrincipal;
import com.gd.mealmate.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "User management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the authenticated user's profile")
    public ApiResponse<UserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        UserDto user = userService.getCurrentUser(userId);
        return ApiResponse.success(user);
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update user preferences", description = "Updates the authenticated user's taste preferences")
    public ApiResponse<UserDto> updatePreferences(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        Long userId = userPrincipal.getUserId();
        UserDto user = userService.updatePreferences(userId, request);
        return ApiResponse.success(user);
    }
}
