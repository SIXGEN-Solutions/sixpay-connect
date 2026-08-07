package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentStatusReconciliationArchitectureTest {

    private static final Path STATUS = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "infrastructure/banking/amplitude/status"
    );

    @Test
    void dedicatedLookupAdapterDoesNotConflict()
            throws Exception {

        String source = Files.readString(
                STATUS.resolve(
                        "DedicatedAmplitudeLookupAdapter.java"
                )
        );

        assertTrue(
                source.contains(
                        "implements LookupGateway"
                )
        );

        assertTrue(
                source.contains(
                        "@ConditionalOnMissingBean(LookupGateway.class)"
                )
        );

        assertTrue(
                source.contains(
                        "AmplitudePostingStatusClient.class"
                )
        );
    }

    @Test
    void lookupClientNeverReplaysFinancialCommand()
            throws Exception {

        String source = Files.readString(
                STATUS.resolve(
                        "client/RestAmplitudePostingStatusClient.java"
                )
        );

        assertTrue(source.contains("restClient.get()"));
        assertFalse(source.contains("restClient.post()"));
        assertFalse(source.contains("postPayment("));
        assertFalse(source.contains("reversePayment("));
    }

    @Test
    void lookupUsesApprovedObservationChannels()
            throws Exception {

        String source = Files.readString(
                STATUS.resolve(
                        "client/RestAmplitudePostingStatusClient.java"
                )
        );

        assertTrue(
                source.contains(
                        "EvidenceObservationChannel.IDEMPOTENCY_LOOKUP"
                )
        );

        assertTrue(
                source.contains(
                        "EvidenceObservationChannel.BANK_REFERENCE_LOOKUP"
                )
        );
    }
}
