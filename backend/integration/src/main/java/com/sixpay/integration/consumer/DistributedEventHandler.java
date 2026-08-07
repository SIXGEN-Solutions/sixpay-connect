package com.sixpay.integration.consumer;

import com.sixpay.integration.event.DistributedEventEnvelope;

@FunctionalInterface
public interface DistributedEventHandler {

    void handle(
            DistributedEventEnvelope<?> event
    );
}
