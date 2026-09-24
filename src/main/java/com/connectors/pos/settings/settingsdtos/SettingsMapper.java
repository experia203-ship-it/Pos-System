package com.connectors.pos.settings.settingsdtos;

import com.connectors.pos.settings.Settings;
import org.mapstruct.*;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.ERROR, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SettingsMapper {

    @Mapping(target = "trialEndsAt", ignore = true)
    @Mapping(target = "lastAccessedAt", ignore = true)
    @Mapping(target = "licensed", ignore = true)
    @Mapping(target = "logo", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateEntity(SettingsUpdateDto update, @MappingTarget Settings settings);


    SettingsResponseDto toResponse(Settings settings);
}
