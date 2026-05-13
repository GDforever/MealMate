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
    MESSAGE_TOO_LONG(40004, "消息内容过长"),

    // Knowledge Base errors (40xxx)
    KNOWLEDGE_BASE_NOT_FOUND(40501, "知识库不存在"),
    KNOWLEDGE_DOCUMENT_NOT_FOUND(40502, "文档不存在"),
    KNOWLEDGE_BASE_ACCESS_DENIED(40503, "无权访问该知识库"),
    UNSUPPORTED_FILE_TYPE(40504, "不支持的文件类型"),
    FILE_PARSE_ERROR(40505, "文件解析失败"),
    EMBEDDING_ERROR(50004, "向量嵌入生成失败"),

    // Food Recognition errors
    IMAGE_RECOGNITION_FAILED(40601, "食物识别失败"),
    IMAGE_TOO_LARGE(40602, "图片文件过大"),
    UNSUPPORTED_IMAGE_TYPE(40603, "不支持的图片格式");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
