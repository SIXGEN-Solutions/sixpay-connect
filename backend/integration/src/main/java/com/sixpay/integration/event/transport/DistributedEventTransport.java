package com.sixpay.integration.event.transport;

import com.sixpay.integration.event.DistributedEventEnvelope;

import java.util.concurrent.CompletionStage;

public interface DistributedEventTransport {

    CompletionStage<Void> publish(
            DistributedEventEnvelope<?> event
    );
}
