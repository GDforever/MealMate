package com.gd.mealmate.function.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetUserPreferencesResponse {
    private Long userId;
    private String username;
    private List<String> tastePreferences;
}
