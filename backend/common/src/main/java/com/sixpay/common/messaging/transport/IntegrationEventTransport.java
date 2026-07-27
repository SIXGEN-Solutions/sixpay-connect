package com.sixpay.common.messaging.transport;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;

/**
 * Publishes integration events without exposing the active transport.
 */
@FunctionalInterface
public interface IntegrationEventTransport {

    /**
     * Publishes one integration event.
     *
     * <p>The method returns only after the active transport has accepted
     * the event or throws when publication cannot be confirmed.</p>
     *
     * @param event event to publish
     */
    void publish(IntegrationEventEnvelope event);
}
