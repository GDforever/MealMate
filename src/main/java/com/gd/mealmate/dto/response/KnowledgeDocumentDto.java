package com.gd.mealmate.dto.response;

import com.gd.mealmate.model.enums.DocumentContentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentDto {
    private Long id;
    private Long knowledgeBaseId;
    private String title;
    private DocumentContentType contentType;
    private LocalDateTime createdAt;
}
