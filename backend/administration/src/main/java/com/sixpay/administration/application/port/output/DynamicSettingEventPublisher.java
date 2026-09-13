package com.sixpay.administration.application.port.output;

import com.sixpay.administration.domain.model.DynamicSettingChanged;

public interface DynamicSettingEventPublisher {
    void publishAfterCommit(DynamicSettingChanged event);
}
