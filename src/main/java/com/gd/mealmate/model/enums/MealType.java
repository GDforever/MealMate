package com.gd.mealmate.model.enums;

import lombok.Getter;

@Getter
public enum MealType {
    BREAKFAST("早餐"),
    LUNCH("午餐"),
    DINNER("晚餐"),
    SNACK("加餐");

    private final String description;

    MealType(String description) {
        this.description = description;
    }
}
