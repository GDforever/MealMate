package com.gd.mealmate.mapper;

import com.gd.mealmate.dto.response.KnowledgeBaseDto;
import com.gd.mealmate.model.entity.KnowledgeBase;
import com.gd.mealmate.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface KnowledgeBaseMapper {

    @Mapping(target = "userId", source = "user", qualifiedByName = "getUserId")
    @Mapping(target = "username", source = "user", qualifiedByName = "getUsername")
    @Mapping(target = "documentCount", ignore = true)
    KnowledgeBaseDto toDto(KnowledgeBase knowledgeBase);

    @Named("getUserId")
    default Long getUserId(User user) {
        return user != null ? user.getId() : null;
    }

    @Named("getUsername")
    default String getUsername(User user) {
        return user != null ? user.getUsername() : null;
    }
}
