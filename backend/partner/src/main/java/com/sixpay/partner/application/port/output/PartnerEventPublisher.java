package com.sixpay.partner.application.port.output;

import com.sixpay.partner.events.PartnerIntegrationEvent;

@FunctionalInterface
public interface PartnerEventPublisher {

    void publish(PartnerIntegrationEvent event);
}
