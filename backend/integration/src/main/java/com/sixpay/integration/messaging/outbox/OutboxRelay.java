package com.sixpay.integration.messaging.outbox;

import com.sixpay.common.messaging.model.OutboxMessage;
import com.sixpay.common.messaging.outbox.OutboxMessageSource;
import com.sixpay.common.messaging.transport.IntegrationEventTransport;
import com.sixpay.common.validation.Preconditions;
import com.sixpay.integration.messaging.properties.OutboxRelayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Polls domain-owned Outboxes and delegates publication to the active
 * transport.
 */
public final class OutboxRelay {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(OutboxRelay.class);

    private final List<OutboxMessageSource> sources;
    private final IntegrationEventTransport transport;
    private final OutboxRelayProperties properties;
    private final Clock clock;

    public OutboxRelay(
            List<OutboxMessageSource> sources,
            IntegrationEventTransport transport,
            OutboxRelayProperties properties,
            Clock clock
    ) {
        this.sources = List.copyOf(
                Preconditions.requireNonNull(
                        sources,
                        "Outbox sources must not be null"
                )
        );
        this.transport = Preconditions.requireNonNull(
                transport,
                "Integration event transport must not be null"
        );
        this.properties = Preconditions.requireNonNull(
                properties,
                "Outbox relay properties must not be null"
        );
        this.clock = Preconditions.requireNonNull(
                clock,
                "Outbox relay clock must not be null"
        );
    }

    /**
     * Executes one bounded polling cycle.
     */
    @Scheduled(
            fixedDelayString =
                    "${sixpay.messaging.outbox.polling-delay:1000}"
    )
    public void poll() {
        for (OutboxMessageSource source : sources) {
            relay(source);
        }
    }

    private void relay(OutboxMessageSource source) {
        Instant now = clock.instant();
        List<OutboxMessage> messages;

        try {
            messages = source.claimPending(
                    properties.batchSize(),
                    now,
                    properties.processingTimeout()
            );
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Unable to claim Outbox messages from source {}",
                    source.sourceName(),
                    exception
            );
            return;
        }

        for (OutboxMessage message : messages) {
            publish(source, message);
        }
    }

    private void publish(
            OutboxMessageSource source,
            OutboxMessage message
    ) {
        Instant attemptTime = clock.instant();

        try {
            transport.publish(message.event());
            source.markPublished(
                    message.event().eventId(),
                    clock.instant()
            );
        } catch (RuntimeException exception) {
            String reason = safeFailureReason(exception);

            if (message.attemptCount() >= properties.maxAttempts()) {
                source.markDead(
                        message.event().eventId(),
                        reason,
                        attemptTime
                );
                LOGGER.error(
                        "Outbox event {} from source {} reached terminal failure",
                        message.event().eventId(),
                        source.sourceName()
                );
                return;
            }

            source.markFailed(
                    message.event().eventId(),
                    reason,
                    attemptTime,
                    attemptTime.plus(
                            properties.retryDelay()
                                    .multipliedBy(
                                            message.attemptCount()
                                    )
                    )
            );
            LOGGER.warn(
                    "Outbox event {} from source {} will be retried",
                    message.event().eventId(),
                    source.sourceName()
            );
        }
    }

    private static String safeFailureReason(
            RuntimeException exception
    ) {
        String reason = exception.getClass().getSimpleName();
        return reason.length() <= 1000
                ? reason
                : reason.substring(0, 1000);
    }
}
