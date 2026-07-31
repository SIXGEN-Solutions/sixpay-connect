package com.sixpay.payment.domain.model.evidence;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.ExternalSystem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvidenceMetadataTest {

    @Test
    void metadataRequiresRealSourceCanonicalCorrelationAndChronology() {
        EvidenceMetadata metadata = metadata(
                ExternalSystem.AMPLITUDE,
                EvidenceObservationChannel.DIRECT_RESPONSE
        );

        assertEquals(ExternalSystem.AMPLITUDE, metadata.sourceSystem());
        assertFalse(
                metadata.toString().contains(
                        metadata.evidenceFingerprint().value()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new EvidenceMetadata(
                        ExternalSystem.NOT_APPLICABLE,
                        CorrelationId.of(UUID.randomUUID().toString()),
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        fingerprint("a"),
                        Instant.parse("2026-07-31T10:00:00Z"),
                        Instant.parse("2026-07-31T10:01:00Z")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new EvidenceMetadata(
                        ExternalSystem.AMPLITUDE,
                        CorrelationId.of("not-a-uuid"),
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        fingerprint("a"),
                        Instant.parse("2026-07-31T10:00:00Z"),
                        Instant.parse("2026-07-31T10:01:00Z")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new EvidenceMetadata(
                        ExternalSystem.AMPLITUDE,
                        CorrelationId.of(UUID.randomUUID().toString()),
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        fingerprint("a"),
                        Instant.parse("2026-07-31T10:02:00Z"),
                        Instant.parse("2026-07-31T10:01:00Z")
                )
        );
    }

    @Test
    void supportIdentifiersUseNormativeFormats() {
        new AuthorizationEvidenceReference(
                "v1:hmac-sha256:" + "a".repeat(64)
        );
        new BankingVerificationId(UUID.randomUUID());
        new FundsVerificationReference("FUNDS-RESULT-0001");
        new TfjConfirmationId(UUID.randomUUID());
        new ReversalAuthorizationReference("RUNBOOK-AUTH-0001");
        new ReversalReference("REV-0001");
        new PostingInstructionId(UUID.randomUUID());
        new PostingIdempotencyKey("POSTING-IDEMPOTENCY-0001");
        new ReversalInstructionId(UUID.randomUUID());
        new ReversalIdempotencyKey("REVERSAL-IDEMPOTENCY-0001");

        assertThrows(
                IllegalArgumentException.class,
                () -> new EvidenceFingerprint(
                        "sha256:" + "a".repeat(64)
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PostingIdempotencyKey("too-short")
        );
    }

    static EvidenceMetadata metadata(
            ExternalSystem source,
            EvidenceObservationChannel channel
    ) {
        return new EvidenceMetadata(
                source,
                CorrelationId.of(
                        "40a11cb8-b32c-474e-bab2-e0b6f43138c8"
                ),
                channel,
                fingerprint("f"),
                Instant.parse("2026-07-31T10:00:00Z"),
                Instant.parse("2026-07-31T10:01:00Z")
        );
    }

    static EvidenceFingerprint fingerprint(String hex) {
        return EvidenceFingerprint.of(
                "v1:sha256:" + hex.repeat(64)
        );
    }
}
