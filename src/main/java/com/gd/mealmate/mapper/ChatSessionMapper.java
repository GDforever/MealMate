package com.gd.mealmate.mapper;

import com.gd.mealmate.dto.response.ChatSessionDto;
import com.gd.mealmate.model.entity.ChatSession;
import org.mapstruct.Mapper;

@Mapper
public interface ChatSessionMapper {
    ChatSessionDto toDto(ChatSession session);
}
