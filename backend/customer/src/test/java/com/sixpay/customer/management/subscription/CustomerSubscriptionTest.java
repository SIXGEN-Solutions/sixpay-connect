package com.sixpay.customer.management.subscription;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerSubscriptionTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T20:00:00Z");

    @Test
    void followsActivationSuspensionReactivationAndClosureLifecycle() {
        CustomerSubscription subscription =
                CustomerSubscription.create(
                        new CustomerSubscriptionId(
                                UUID.randomUUID()
                        ),
                        new CustomerId(UUID.randomUUID()),
                        UUID.randomUUID(),
                        new CustomerBankAccountId(
                                UUID.randomUUID()
                        ),
                        NOW
                );

        assertThat(subscription.status())
                .isEqualTo(
                        CustomerSubscriptionStatus.PENDING_ACTIVATION
                );

        subscription.activate(NOW.plusSeconds(1));
        assertThat(subscription.acceptsPayments())
                .isTrue();

        subscription.suspend(
                "manual review",
                NOW.plusSeconds(2)
        );
        assertThat(subscription.status())
                .isEqualTo(
                        CustomerSubscriptionStatus.SUSPENDED
                );

        subscription.activate(NOW.plusSeconds(3));
        assertThat(subscription.status())
                .isEqualTo(
                        CustomerSubscriptionStatus.ACTIVE
                );

        subscription.close(
                "customer unsubscribed",
                NOW.plusSeconds(4)
        );
        assertThat(subscription.status())
                .isEqualTo(
                        CustomerSubscriptionStatus.CLOSED
                );

        assertThatThrownBy(() ->
                subscription.activate(
                        NOW.plusSeconds(5)
                )
        ).isInstanceOf(CustomerDomainException.class);
    }
}
