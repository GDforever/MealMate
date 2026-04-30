package com.gd.mealmate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseDto {
    private Long id;
    private String title;
    private String description;
    private Long userId;
    private String username;
    private Boolean isPublic;
    private Integer documentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
