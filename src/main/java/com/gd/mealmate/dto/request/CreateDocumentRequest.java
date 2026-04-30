package com.gd.mealmate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentRequest {

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 255, message = "标题最多255个字符")
    private String title;

    @NotBlank(message = "文档内容不能为空")
    private String content;
}
