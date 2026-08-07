package com.sixpay.integration.event;

public interface DistributedEventSerializer {

    byte[] serialize(
            DistributedEventEnvelope<?> event
    );

    DistributedEventEnvelope<?> deserialize(
            byte[] payload
    );
}
