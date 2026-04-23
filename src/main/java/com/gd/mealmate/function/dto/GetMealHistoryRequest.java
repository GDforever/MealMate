package com.gd.mealmate.function.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetMealHistoryRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer limit;
}
