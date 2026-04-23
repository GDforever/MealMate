package com.gd.mealmate.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USERNAME_ALREADY_EXISTS(40001, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(40002, "邮箱已被注册"),
    INVALID_CREDENTIALS(40100, "用户名或密码错误"),
    TOKEN_EXPIRED(40102, "Token已过期"),
    TOKEN_INVALID(40101, "Token无效"),
    RESOURCE_NOT_FOUND(40401, "资源不存在"),
    GENERAL_ERROR(50001, "系统错误"),

    // AI Service errors (50xxx)
    AI_SERVICE_UNAVAILABLE(50002, "AI服务暂时不可用"),
    AMAP_API_ERROR(50003, "高德地图API调用失败"),

    // Chat errors (40xxx)
    INVALID_CHAT_SESSION(40003, "无效的会话ID"),
    MESSAGE_TOO_LONG(40004, "消息内容过长");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
