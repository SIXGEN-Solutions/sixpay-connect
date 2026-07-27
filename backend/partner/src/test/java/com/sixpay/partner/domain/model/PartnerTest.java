package com.sixpay.partner.domain.model;

import com.sixpay.partner.domain.event.PartnerCreated;
import com.sixpay.partner.domain.event.PartnerStatusChanged;
import com.sixpay.partner.domain.exception.PartnerDomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerTest {

    private static final Instant NOW = Instant.parse("2026-07-26T12:00:00Z");

    @Test
    void createsPartnerPendingValidationAndRaisesEvent() {
        var partner = newPartner();

        assertThat(partner.status()).isEqualTo(PartnerStatus.PENDING_VALIDATION);
        assertThat(partner.pullDomainEvents())
                .singleElement()
                .isInstanceOf(PartnerCreated.class);
    }

    @Test
    void rejectsPendingPartnerOnlyWithAReason() {
        var partner = newPartner();
        partner.pullDomainEvents();

        assertThatThrownBy(() -> partner.reject(" ", NOW.plusSeconds(1)))
                .isInstanceOf(PartnerDomainException.class)
                .hasMessageContaining("reason");

        partner.reject("Dossier incomplet", NOW.plusSeconds(1));

        assertThat(partner.status()).isEqualTo(PartnerStatus.REJECTED);
        assertThat(partner.statusReason()).contains("Dossier incomplet");
        assertThat(partner.pullDomainEvents())
                .singleElement()
                .isInstanceOf(PartnerStatusChanged.class);
    }

    @Test
    void suspendsAndReactivatesOnlyAnActivePartner() {
        var partner = newPartner();
        partner.pullDomainEvents();

        assertThatThrownBy(() -> partner.suspend("Risque détecté", NOW.plusSeconds(1)))
                .isInstanceOf(PartnerDomainException.class);

        partner.approve(NOW.plusSeconds(1));
        partner.pullDomainEvents();
        partner.suspend("Risque détecté", NOW.plusSeconds(2));
        partner.reactivate(NOW.plusSeconds(3));

        assertThat(partner.status()).isEqualTo(PartnerStatus.ACTIVE);
        assertThat(partner.statusReason()).isEmpty();
    }

    @Test
    void configuresThresholdOnlyInsideAuthorizedPerimeter() {
        var partner = newPartner();
        partner.pullDomainEvents();

        partner.configureValidationThreshold(
                new ValidationThreshold("PAYMENT", "XAF", new java.math.BigDecimal("1000.00"), 2),
                NOW.plusSeconds(1)
        );

        assertThat(partner.thresholdFor("payment", "xaf"))
                .get()
                .extracting(ValidationThreshold::validationLevels)
                .isEqualTo(2);
        assertThatThrownBy(() -> partner.configureValidationThreshold(
                new ValidationThreshold("REFUND", "XAF", new java.math.BigDecimal("100"), 1),
                NOW.plusSeconds(2)
        )).isInstanceOf(PartnerDomainException.class)
                .hasMessageContaining("authorized perimeter");
    }

    private static Partner newPartner() {
        return Partner.create(
                new PartnerId(UUID.fromString("8ec6a427-406f-4f93-b271-cbc819a4c1dd")),
                new PartnerName("Acme Payments"),
                new TechnicalContact("Alice Ops", "alice.ops@example.com"),
                new AuthorizedPerimeter(Set.of("PAYMENT")),
                NOW
        );
    }
}
