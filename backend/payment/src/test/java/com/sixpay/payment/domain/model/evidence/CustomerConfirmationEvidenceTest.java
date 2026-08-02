package com.sixpay.payment.domain.model.evidence;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.ExternalSystem;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerConfirmationEvidenceTest {

    private static final EvidenceFingerprint FINGERPRINT =
            EvidenceFingerprint.of(
                    "v1:sha256:"
                            + "a".repeat(64)
            );

    @Test
    void acceptsBankEvidenceWithoutOtpMaterial() {
        Instant instant =
                Instant.parse("2026-08-03T10:31:00Z");

        CustomerConfirmationEvidence evidence =
                new CustomerConfirmationEvidence(
                        CustomerConfirmationReference.of(
                                "AMP-CONF-000001"
                        ),
                        FINGERPRINT,
                        instant,
                        metadata(instant)
                );

        assertThat(evidence.confirmationReference().value())
                .isEqualTo("AMP-CONF-000001");
        assertThat(evidence.confirmationFingerprint())
                .isEqualTo(FINGERPRINT);
    }

    @Test
    void rejectsNonBankEvidence() {
        Instant instant =
                Instant.parse("2026-08-03T10:31:00Z");

        EvidenceMetadata metadata =
                new EvidenceMetadata(
                        ExternalSystem.TRESOR_PAY,
                        CorrelationId.of(
                                "11111111-1111-1111-1111-111111111111"
                        ),
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        FINGERPRINT,
                        instant,
                        instant
                );

        assertThatThrownBy(() ->
                new CustomerConfirmationEvidence(
                        CustomerConfirmationReference.of(
                                "TP-CONF-000001"
                        ),
                        FINGERPRINT,
                        instant,
                        metadata
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private static EvidenceMetadata metadata(Instant instant) {
        return new EvidenceMetadata(
                ExternalSystem.AMPLITUDE,
                CorrelationId.of(
                        "11111111-1111-1111-1111-111111111111"
                ),
                EvidenceObservationChannel.DIRECT_RESPONSE,
                FINGERPRINT,
                instant,
                instant
        );
    }
}
