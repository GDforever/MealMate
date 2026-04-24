package com.gd.mealmate.mapper;

import com.gd.mealmate.dto.response.ChatMessageDto;
import com.gd.mealmate.model.entity.ChatMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {
    ChatMessageDto toDto(ChatMessage message);
}
