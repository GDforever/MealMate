package com.gd.mealmate.dto.request;

import com.gd.mealmate.model.enums.MealType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodRecognitionRequest {
    @NotNull(message = "用餐类型不能为空")
    private MealType mealType;
}
