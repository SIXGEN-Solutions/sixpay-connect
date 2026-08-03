package com.sixpay.customer.verification.domain.model;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.verification.domain.event.CustomerVerificationCompleted;
import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerVerificationTest {

    private static final Instant REQUESTED_AT =
            Instant.parse("2026-08-03T12:00:00Z");
    private static final Instant OBSERVED_AT =
            Instant.parse("2026-08-03T12:00:01Z");
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-08-03T12:00:02Z");

    private static final UUID EVENT_ID = UUID.fromString(
            "9dc8e15d-3e26-4cf1-9fd8-bc88aa39ac1e"
    );

    @Test
    void requestCreatesRequestedAggregateWithoutEvent() {
        CustomerVerification verification =
                CustomerVerification.request(request());

        assertEquals(
                VerificationStatus.REQUESTED,
                verification.status()
        );
        assertFalse(verification.result().isPresent());
        assertEquals(REQUESTED_AT, verification.updatedAt());
        assertTrue(verification.domainEvents().isEmpty());
    }

    @Test
    void completeProducesCanonicalResultAndOneSafeEvent() {
        CustomerVerification verification =
                CustomerVerification.request(request());

        CustomerVerificationResult result = verification.complete(
                verifiedEvidence(),
                EVENT_ID,
                COMPLETED_AT
        );

        assertEquals(
                VerificationStatus.COMPLETED,
                verification.status()
        );
        assertEquals(VerificationOutcome.VERIFIED, result.outcome());
        assertEquals(COMPLETED_AT, verification.updatedAt());
        assertEquals(1, verification.domainEvents().size());

        CustomerVerificationCompleted event =
                (CustomerVerificationCompleted)
                        verification.domainEvents().getFirst();

        assertEquals(EVENT_ID, event.eventId());
        assertEquals(verification.id(), event.verificationId());
        assertEquals(VerificationOutcome.VERIFIED, event.outcome());
        assertEquals(COMPLETED_AT, event.occurredAt());
        assertEquals(
                request().accountBindingFingerprint(),
                event.accountBindingFingerprint()
        );
    }

    @Test
    void verificationCanBeCompletedOnlyOnce() {
        CustomerVerification verification =
                CustomerVerification.request(request());

        verification.complete(
                verifiedEvidence(),
                EVENT_ID,
                COMPLETED_AT
        );

        assertThrows(
                CustomerVerificationDomainException.class,
                () -> verification.complete(
                        verifiedEvidence(),
                        UUID.fromString(
                                "b7d56189-f945-43a7-a936-b80e798afbea"
                        ),
                        COMPLETED_AT.plusSeconds(1)
                )
        );
    }

    @Test
    void rejectsEvidencePredatingTheRequest() {
        CustomerVerification verification =
                CustomerVerification.request(request());

        VerificationEvidence oldEvidence = VerificationEvidence.of(
                allPassed(),
                fingerprint(),
                REQUESTED_AT.minusSeconds(1),
                REQUESTED_AT.plusSeconds(60)
        );

        assertThrows(
                CustomerVerificationDomainException.class,
                () -> verification.complete(
                        oldEvidence,
                        EVENT_ID,
                        COMPLETED_AT
                )
        );
    }

    @Test
    void rejectsExpiredEvidence() {
        CustomerVerification verification =
                CustomerVerification.request(request());

        VerificationEvidence expired = VerificationEvidence.of(
                allPassed(),
                fingerprint(),
                OBSERVED_AT,
                COMPLETED_AT.minusMillis(1)
        );

        assertThrows(
                CustomerVerificationDomainException.class,
                () -> verification.complete(
                        expired,
                        EVENT_ID,
                        COMPLETED_AT
                )
        );
    }

    @Test
    void reconstitutionDoesNotRaiseNewEvents() {
        CustomerVerificationResult result =
                CustomerVerificationResult.from(
                        verifiedEvidence(),
                        COMPLETED_AT
                );

        CustomerVerification verification =
                CustomerVerification.reconstitute(
                        request(),
                        VerificationStatus.COMPLETED,
                        result,
                        COMPLETED_AT
                );

        assertTrue(verification.domainEvents().isEmpty());
        assertEquals(result, verification.result().orElseThrow());
    }

    private static CustomerVerificationRequest request() {
        return new CustomerVerificationRequest(
                new CustomerVerificationId(
                        UUID.fromString(
                                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                        )
                ),
                CustomerVerificationSubject.of(
                        CustomerIdentity.of(
                                CustomerNiu.of("M0123456"),
                                "Ada Lovelace"
                        )
                ),
                FinancialInstitutionCode.of("AMPLITUDE"),
                AccountBindingFingerprint.of(
                        "v1:" + "a".repeat(64)
                ),
                CustomerVerificationContext.of(
                        CorrelationId.of("corr-123"),
                        UUID.fromString(
                                "c74e165f-df46-463e-a520-188e6df3e5ae"
                        )
                ),
                REQUESTED_AT
        );
    }

    private static VerificationEvidence verifiedEvidence() {
        return VerificationEvidence.of(
                allPassed(),
                fingerprint(),
                OBSERVED_AT,
                COMPLETED_AT.plusSeconds(300)
        );
    }

    private static java.util.List<VerificationCheck> allPassed() {
        return Arrays.stream(VerificationCheckType.values())
                .map(VerificationCheck::passed)
                .toList();
    }

    private static VerificationEvidenceFingerprint fingerprint() {
        return VerificationEvidenceFingerprint.of(
                "v1:sha256:" + "b".repeat(64)
        );
    }
}
