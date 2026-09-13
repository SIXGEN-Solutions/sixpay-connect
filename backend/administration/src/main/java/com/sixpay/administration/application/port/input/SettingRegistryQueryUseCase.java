package com.sixpay.administration.application.port.input;

import com.sixpay.administration.domain.model.SettingDefinition;
import java.util.Collection;
import java.util.Optional;

public interface SettingRegistryQueryUseCase {
    Collection<SettingDefinition> definitions();
    Optional<SettingDefinition> find(String key);
}
