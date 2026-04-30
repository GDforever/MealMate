package com.gd.mealmate.mapper;

import com.gd.mealmate.dto.response.KnowledgeDocumentDto;
import com.gd.mealmate.model.entity.KnowledgeDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface KnowledgeDocumentMapper {

    @Mapping(target = "knowledgeBaseId", source = "knowledgeBase", qualifiedByName = "getKnowledgeBaseId")
    KnowledgeDocumentDto toDto(KnowledgeDocument document);

    @Named("getKnowledgeBaseId")
    default Long getKnowledgeBaseId(com.gd.mealmate.model.entity.KnowledgeBase kb) {
        return kb != null ? kb.getId() : null;
    }
}
