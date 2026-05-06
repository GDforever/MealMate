package com.gd.mealmate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunkDto {
    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private LocalDateTime createdAt;
}
