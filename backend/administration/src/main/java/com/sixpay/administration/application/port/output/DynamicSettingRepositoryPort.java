package com.sixpay.administration.application.port.output;

import com.sixpay.administration.domain.model.DynamicSettingValue;
import java.util.Optional;

public interface DynamicSettingRepositoryPort {
    Optional<DynamicSettingValue> findByKey(String key);
    DynamicSettingValue save(DynamicSettingValue value);
}
