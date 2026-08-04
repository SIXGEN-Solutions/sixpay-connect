package com.sixpay.customer.observation.application.port.output;

import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.customer.observation.domain.model.ObservedPaymentReference;
import com.sixpay.customer.observation.domain.model.ProjectionWatermark;

import java.time.Instant;
import java.util.UUID;

/**
 * Customer-owned persistence boundary for Payment observations linked to an
 * Observed Customer.
 *
 * <p>The implementation must enforce source-event idempotence and persist no
 * raw bank-account value.</p>
 */
public interface ObservedPaymentRepository {

    void save(
            ObservedCustomerId observedCustomerId,
            UUID sourceEventId,
            ObservedPaymentReference payment,
            ProjectionWatermark watermark,
            Instant observedAt
    );
}
