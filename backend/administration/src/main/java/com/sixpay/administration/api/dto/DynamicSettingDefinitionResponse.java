package com.sixpay.administration.api.dto;

import com.sixpay.administration.domain.model.SettingDefinition;
import java.util.Set;

public record DynamicSettingDefinitionResponse(
        String key,
        String domain,
        String type,
        String defaultValue,
        String minimumValue,
        String maximumValue,
        Set<String> allowedValues,
        String description,
        boolean dynamic,
        boolean sensitive,
        boolean requiresRestart
) {
    public static DynamicSettingDefinitionResponse from(SettingDefinition d) {
        return new DynamicSettingDefinitionResponse(
                d.key(), d.domain().name(), d.type().name(),
                d.defaultValue(), d.minimumValue(), d.maximumValue(),
                d.allowedValues(), d.description(),
                d.dynamic(), d.sensitive(), d.requiresRestart()
        );
    }
}
