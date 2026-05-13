package com.gd.mealmate.model.enums;

import lombok.Getter;

@Getter
public enum RecordSource {
    CHAT("聊天"),
    PHOTO("拍照识别"),
    MANUAL("手动录入");

    private final String description;

    RecordSource(String description) {
        this.description = description;
    }
}
