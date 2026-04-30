package com.gd.mealmate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseDetailDto {
    private Long id;
    private String title;
    private String description;
    private Long userId;
    private String username;
    private Boolean isPublic;
    private List<KnowledgeDocumentDto> documents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
