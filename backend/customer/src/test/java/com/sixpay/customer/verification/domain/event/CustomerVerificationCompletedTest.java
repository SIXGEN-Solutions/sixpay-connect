package com.sixpay.customer.verification.domain.event;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.customer.verification.domain.model.AccountBindingFingerprint;
import com.sixpay.customer.verification.domain.model.CustomerVerificationId;
import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationCheckType;
import com.sixpay.customer.verification.domain.model.VerificationEvidenceFingerprint;
import com.sixpay.customer.verification.domain.model.VerificationOutcome;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerVerificationCompletedTest {

    @Test
    void eventDoesNotExposeSensitiveRawData() {
        CustomerVerificationCompleted event = event();

        String rendered = event.toString();

        assertFalse(rendered.contains("M0123456"));
        assertFalse(rendered.contains("Ada Lovelace"));
        assertFalse(rendered.contains("1234567890123456"));
    }

    @Test
    void eventRequiresNonNilUuidV4() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> new CustomerVerificationCompleted(
                        new UUID(0L, 0L),
                        event().verificationId(),
                        event().outcome(),
                        event().checks(),
                        event().evidenceFingerprint(),
                        event().accountBindingFingerprint(),
                        event().completedAt()
                )
        );
    }

    private static CustomerVerificationCompleted event() {
        return new CustomerVerificationCompleted(
                UUID.fromString(
                        "9dc8e15d-3e26-4cf1-9fd8-bc88aa39ac1e"
                ),
                new CustomerVerificationId(
                        UUID.fromString(
                                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                        )
                ),
                VerificationOutcome.VERIFIED,
                Arrays.stream(VerificationCheckType.values())
                        .map(VerificationCheck::passed)
                        .toList(),
                VerificationEvidenceFingerprint.of(
                        "v1:sha256:" + "a".repeat(64)
                ),
                AccountBindingFingerprint.of(
                        "v1:" + "b".repeat(64)
                ),
                Instant.parse("2026-08-03T12:00:02Z")
        );
    }
}
