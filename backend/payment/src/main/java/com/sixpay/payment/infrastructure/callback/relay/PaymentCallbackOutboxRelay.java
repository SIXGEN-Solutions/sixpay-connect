package com.sixpay.payment.infrastructure.callback.relay;

import com.sixpay.payment.application.port.output.callback
        .PaymentStatusCallbackTransportPort;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "sixpay.payment.callback",
        name = "enabled",
        havingValue = "true"
)
public class PaymentCallbackOutboxRelay {

    private final PaymentCallbackOutboxCoordinator coordinator;
    private final PaymentCallbackPlanFactory planFactory;
    private final PaymentStatusCallbackTransportPort transport;

    public PaymentCallbackOutboxRelay(
            PaymentCallbackOutboxCoordinator coordinator,
            PaymentCallbackPlanFactory planFactory,
            PaymentStatusCallbackTransportPort transport
    ) {
        this.coordinator = Objects.requireNonNull(coordinator);
        this.planFactory = Objects.requireNonNull(planFactory);
        this.transport = Objects.requireNonNull(transport);
    }

    @Scheduled(
            fixedDelayString =
                    "${sixpay.payment.callback.poll-delay:PT2S}"
    )
    public void publishAvailableCallbacks() {
        for (ClaimedPaymentOutboxEvent event
                : coordinator.claim()) {
            publish(event);
        }
    }

    private void publish(
            ClaimedPaymentOutboxEvent event
    ) {
        try {
            PaymentCallbackPlan plan =
                    planFactory.create(event);

            if (plan.deliver()) {
                transport.send(plan.delivery());
            }

            coordinator.markPublished(event.eventId(), event.attemptCount());
        } catch (PaymentCallbackOutboxCoordinator.StaleCallbackClaimException stale) {
            // Reclaimed by another attempt: the late worker cannot complete it.
        } catch (Exception exception) {
            try {
                coordinator.markFailed(
                        event.eventId(),
                        event.attemptCount(),
                        exception
                );
            } catch (PaymentCallbackOutboxCoordinator.StaleCallbackClaimException stale) {
                // The owning attempt has changed while this call was executing.
            }
        }
    }
}
