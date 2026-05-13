package com.gd.mealmate.dto.request;

import com.gd.mealmate.model.enums.MealType;
import com.gd.mealmate.model.enums.RecordSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealRecordRequest {
    private Long userId;
    @NotNull(message = "用餐类型不能为空")
    private MealType mealType;
    @NotBlank(message = "食物名称不能为空")
    private String foodName;
    private String restaurantName;
    private String location;
    private Double latitude;
    private Double longitude;
    @NotNull(message = "用餐时间不能为空")
    private LocalDateTime recordedAt;
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer userRating;
    private String tags;
    private String imageUrl;
    private RecordSource source;
}
