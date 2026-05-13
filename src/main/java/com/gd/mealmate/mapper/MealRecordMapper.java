package com.gd.mealmate.mapper;

import com.gd.mealmate.model.entity.MealRecord;
import com.gd.mealmate.model.enums.RecordSource;
import com.gd.mealmate.dto.response.MealRecordDto;
import com.gd.mealmate.dto.request.MealRecordRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MealRecordMapper {

    @Mapping(target = "userId", source = "user", qualifiedByName = "getUserId")
    @Mapping(target = "source", source = "source", qualifiedByName = "sourceToString")
    MealRecordDto toDto(MealRecord mealRecord);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    MealRecord toEntity(MealRecordRequest request);

    @Named("getUserId")
    default Long getUserId(com.gd.mealmate.model.entity.User user) {
        return user != null ? user.getId() : null;
    }

    @Named("sourceToString")
    default String sourceToString(RecordSource source) {
        return source != null ? source.name() : null;
    }
}
