package com.sixpay.customer.management.linking;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.ObservedCustomerLink;
import com.sixpay.customer.management.domain.model.ObservedCustomerLinkStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObservedCustomerLinkTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T20:00:00Z");

    @Test
    void supportsExplicitLinkUnlinkAndRelink() {
        UUID observedCustomerId = UUID.randomUUID();
        CustomerId firstCustomer =
                new CustomerId(UUID.randomUUID());
        CustomerId secondCustomer =
                new CustomerId(UUID.randomUUID());

        ObservedCustomerLink link =
                ObservedCustomerLink.create(
                        observedCustomerId,
                        firstCustomer,
                        "admin-user",
                        "corr-1",
                        "manual correlation confirmed",
                        NOW
                );

        assertThat(link.status())
                .isEqualTo(
                        ObservedCustomerLinkStatus.LINKED
                );

        link.unlink(
                "admin-user",
                "corr-2",
                "correlation invalidated",
                NOW.plusSeconds(1)
        );

        assertThat(link.status())
                .isEqualTo(
                        ObservedCustomerLinkStatus.UNLINKED
                );

        link.relink(
                secondCustomer,
                "admin-user",
                "corr-3",
                "corrected correlation",
                NOW.plusSeconds(2)
        );

        assertThat(link.status())
                .isEqualTo(
                        ObservedCustomerLinkStatus.LINKED
                );
        assertThat(link.customerId())
                .isEqualTo(secondCustomer);
        assertThat(link.unlinkedAt())
                .isEmpty();
    }

    @Test
    void refusesUnlinkTwice() {
        ObservedCustomerLink link =
                ObservedCustomerLink.create(
                        UUID.randomUUID(),
                        new CustomerId(UUID.randomUUID()),
                        "admin-user",
                        "corr-1",
                        "manual link",
                        NOW
                );

        link.unlink(
                "admin-user",
                "corr-2",
                "manual unlink",
                NOW.plusSeconds(1)
        );

        assertThatThrownBy(() ->
                link.unlink(
                        "admin-user",
                        "corr-3",
                        "duplicate unlink",
                        NOW.plusSeconds(2)
                )
        ).isInstanceOf(CustomerDomainException.class);
    }
}
