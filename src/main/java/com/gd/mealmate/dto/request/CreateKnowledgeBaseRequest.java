package com.gd.mealmate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateKnowledgeBaseRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题最多255个字符")
    private String title;

    @Size(max = 1000, message = "描述最多1000个字符")
    private String description;

    private Boolean isPublic = false;
}
