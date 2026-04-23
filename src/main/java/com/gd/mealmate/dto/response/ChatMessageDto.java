package com.gd.mealmate.dto.response;

import com.gd.mealmate.model.enums.ChatMessageRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    private Long id;
    private ChatMessageRole role;
    private String content;
    private LocalDateTime createdAt;
}
