package com.sixpay.customer.observation.application.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservedCustomerAuditContextTest {

    @Test
    void normalizesTechnicalIdentifiers() {
        ObservedCustomerAuditContext context =
                new ObservedCustomerAuditContext(
                        " service-account:customer ",
                        " correlation-001 "
                );

        assertEquals(
                "service-account:customer",
                context.actorId()
        );
        assertEquals(
                "correlation-001",
                context.correlationId()
        );
    }

    @Test
    void rejectsMissingOrOversizedIdentifiers() {
        assertThrows(
                NullPointerException.class,
                () -> new ObservedCustomerAuditContext(
                        null,
                        "correlation"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ObservedCustomerAuditContext(
                        " ",
                        "correlation"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ObservedCustomerAuditContext(
                        "actor",
                        "x".repeat(151)
                )
        );
    }

    @Test
    void renderingProtectsBothContextValues() {
        ObservedCustomerAuditContext context =
                new ObservedCustomerAuditContext(
                        "service-account:customer",
                        "correlation-001"
                );

        String rendered = context.toString();

        assertFalse(rendered.contains(context.actorId()));
        assertFalse(rendered.contains(context.correlationId()));
    }
}
