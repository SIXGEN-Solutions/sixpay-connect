package com.sixpay.notification.application.port.output;

import com.sixpay.notification.application.model.PartnerStatusChangedEvent;

@FunctionalInterface
public interface PartnerStatusChangedEventDecoder {

    PartnerStatusChangedEvent decode(String payload);
}
