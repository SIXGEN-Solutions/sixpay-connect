package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationEvidenceSnapshotTest {

    @Test
    void approvedEvidenceIsCanonicalAndContainsNoRawToken() {
        AuthorizationEvidenceSnapshot snapshot = approvedSnapshot(
                List.of(
                        binding(
                                AuthorizationBindingType.PAYMENT_SCOPE,
                                AuthorizationBindingResult.MATCH
                        ),
                        binding(
                                AuthorizationBindingType
                                        .SUBSCRIPTION_REFERENCE,
                                AuthorizationBindingResult.MATCH
                        )
                )
        );

        assertEquals(
                AuthorizationBindingType.SUBSCRIPTION_REFERENCE,
                snapshot.bindingResults().getFirst().type()
        );
        assertFalse(snapshot.rejectionCode().isPresent());
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.bindingResults().clear()
        );
    }

    @Test
    void approvedEvidenceRejectsMismatchAndRejectionCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> approvedSnapshot(
                        List.of(
                                binding(
                                        AuthorizationBindingType
                                                .DEBTOR_ACCOUNT,
                                        AuthorizationBindingResult.MISMATCH
                                )
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthorizationEvidenceSnapshot(
                        reference(),
                        AuthorizationDecisionOutcome.APPROVED,
                        EvidenceMetadataTest.fingerprint("a"),
                        "https://tresor-pay.example",
                        "key-01",
                        "RS256",
                        "payment:initiate",
                        List.of(
                                binding(
                                        AuthorizationBindingType.PAYMENT_SCOPE,
                                        AuthorizationBindingResult.MATCH
                                )
                        ),
                        Instant.parse("2026-07-31T09:00:00Z"),
                        Instant.parse("2026-07-31T09:00:00Z"),
                        Instant.parse("2026-07-31T11:00:00Z"),
                        FailureCode.of("AUTHORIZATION_REJECTED"),
                        metadata()
                )
        );
    }

    @Test
    void rejectedEvidenceRequiresStableCode() {
        AuthorizationEvidenceSnapshot snapshot =
                new AuthorizationEvidenceSnapshot(
                        reference(),
                        AuthorizationDecisionOutcome.REJECTED,
                        EvidenceMetadataTest.fingerprint("a"),
                        "https://tresor-pay.example",
                        "key-01",
                        "RS256",
                        "payment:initiate",
                        List.of(
                                binding(
                                        AuthorizationBindingType
                                                .DEBTOR_ACCOUNT,
                                        AuthorizationBindingResult.MISMATCH
                                )
                        ),
                        Instant.parse("2026-07-31T09:00:00Z"),
                        Instant.parse("2026-07-31T09:00:00Z"),
                        Instant.parse("2026-07-31T11:00:00Z"),
                        FailureCode.of("ACCOUNT_BINDING_MISMATCH"),
                        metadata()
                );

        assertEquals(
                AuthorizationDecisionOutcome.REJECTED,
                snapshot.outcome()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthorizationEvidenceSnapshot(
                        reference(),
                        AuthorizationDecisionOutcome.REJECTED,
                        EvidenceMetadataTest.fingerprint("a"),
                        "issuer",
                        "key-01",
                        "RS256",
                        "payment:initiate",
                        List.of(
                                binding(
                                        AuthorizationBindingType.TOKEN_REPLAY,
                                        AuthorizationBindingResult.MISMATCH
                                )
                        ),
                        Instant.parse("2026-07-31T09:00:00Z"),
                        Instant.parse("2026-07-31T09:00:00Z"),
                        Instant.parse("2026-07-31T11:00:00Z"),
                        null,
                        metadata()
                )
        );
    }

    @Test
    void bindingTypesAreUniqueAndChronologyIsValid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> approvedSnapshot(
                        List.of(
                                binding(
                                        AuthorizationBindingType.PAYMENT_SCOPE,
                                        AuthorizationBindingResult.MATCH
                                ),
                                binding(
                                        AuthorizationBindingType.PAYMENT_SCOPE,
                                        AuthorizationBindingResult.MATCH
                                )
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthorizationEvidenceSnapshot(
                        reference(),
                        AuthorizationDecisionOutcome.APPROVED,
                        EvidenceMetadataTest.fingerprint("a"),
                        "issuer",
                        "key-01",
                        "RS256",
                        "payment:initiate",
                        List.of(
                                binding(
                                        AuthorizationBindingType.PAYMENT_SCOPE,
                                        AuthorizationBindingResult.MATCH
                                )
                        ),
                        Instant.parse("2026-07-31T10:00:00Z"),
                        Instant.parse("2026-07-31T09:00:00Z"),
                        Instant.parse("2026-07-31T11:00:00Z"),
                        null,
                        metadata()
                )
        );
    }

    private static AuthorizationEvidenceSnapshot approvedSnapshot(
            List<AuthorizationBindingEvidence> bindings
    ) {
        return new AuthorizationEvidenceSnapshot(
                reference(),
                AuthorizationDecisionOutcome.APPROVED,
                EvidenceMetadataTest.fingerprint("a"),
                "https://tresor-pay.example",
                "key-01",
                "RS256",
                "payment:initiate",
                bindings,
                Instant.parse("2026-07-31T09:00:00Z"),
                Instant.parse("2026-07-31T09:00:00Z"),
                Instant.parse("2026-07-31T11:00:00Z"),
                null,
                metadata()
        );
    }

    private static AuthorizationBindingEvidence binding(
            AuthorizationBindingType type,
            AuthorizationBindingResult result
    ) {
        return new AuthorizationBindingEvidence(type, result);
    }

    private static AuthorizationEvidenceReference reference() {
        return new AuthorizationEvidenceReference(
                "v1:hmac-sha256:" + "b".repeat(64)
        );
    }

    private static EvidenceMetadata metadata() {
        return EvidenceMetadataTest.metadata(
                ExternalSystem.TRESOR_PAY,
                EvidenceObservationChannel.LOCAL_VALIDATION
        );
    }
}
