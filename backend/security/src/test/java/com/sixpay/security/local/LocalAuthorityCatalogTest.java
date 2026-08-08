package com.sixpay.security.local;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAuthorityCatalogTest {

    @Test
    void adminReceivesPaymentAndObservedCustomerReadScopes() {
        var authorities = LocalAuthorityCatalog.authoritiesFor(LocalRole.ADMIN);

        assertThat(authorities)
                .extracting(Object::toString)
                .contains(
                        "ROLE_ADMIN",
                        "SCOPE_payment.read",
                        "SCOPE_observed-customer.read",
                        "SCOPE_partner.read"
                );
    }

    @Test
    void auditorReceivesAuditScopes() {
        var authorities = LocalAuthorityCatalog.authoritiesFor(LocalRole.AUDITOR);

        assertThat(authorities)
                .extracting(Object::toString)
                .contains(
                        "ROLE_AUDITOR",
                        "SCOPE_payment.read",
                        "SCOPE_observed-customer.read",
                        "SCOPE_payment.audit.read",
                        "SCOPE_payment.audit.export"
                );
    }

    @Test
    void partnerDoesNotReceiveInternalPaymentReadScope() {
        var authorities = LocalAuthorityCatalog.authoritiesFor(LocalRole.PARTNER);

        assertThat(authorities)
                .extracting(Object::toString)
                .contains("ROLE_PARTNER", "SCOPE_partner.self.read")
                .doesNotContain("SCOPE_payment.read");
    }
}
