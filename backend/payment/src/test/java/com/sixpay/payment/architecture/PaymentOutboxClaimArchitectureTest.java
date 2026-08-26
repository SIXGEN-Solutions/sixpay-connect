package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentOutboxClaimArchitectureTest {

    private static final Path OUTBOX_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "infrastructure/outbox"
    );

    @Test
    void repositoryUsesPostgresqlSkipLockedAndBoundedClaim()
            throws Exception {

        String source = Files.readString(
                OUTBOX_ROOT.resolve(
                        "PaymentOutboxRepository.java"
                )
        );

        for (String required : List.of(
                "FOR UPDATE OF candidate SKIP LOCKED",
                "LIMIT :batchSize",
                "status IN ('PENDING', 'FAILED')",
                "status = 'PROCESSING'",
                "claimed_at < :staleBefore",
                "NOT EXISTS",
                "predecessor.aggregate_id",
                "'PUBLISHED'",
                "'DEAD'"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing claim SQL concept: "
                            + required
            );
        }
    }

    @Test
    void claimTransactionEndsBeforeDeliveryAndReturnsNoJpaEntity()
            throws Exception {

        String service = Files.readString(
                OUTBOX_ROOT.resolve(
                        "claim/PaymentOutboxClaimService.java"
                )
        );

        String claim = Files.readString(
                OUTBOX_ROOT.resolve(
                        "claim/PaymentOutboxClaim.java"
                )
        );

        assertTrue(service.contains("TransactionTemplate"));
        assertTrue(service.contains(
                "repository.lockClaimable("
        ));
        assertTrue(service.contains("repository.flush()"));
        assertTrue(service.contains(
                "List<PaymentOutboxClaim>"
        ));

        for (String forbidden : List.of(
                "ObservedCustomerProjectionPort",
                "PaymentOutboxEventDeserializer",
                "RestClient",
                "WebClient",
                "KafkaTemplate",
                "@Scheduled"
        )) {
            assertFalse(
                    service.contains(forbidden),
                    () -> "Claim service contains delivery "
                            + "concept: "
                            + forbidden
            );
        }

        int recordStart = claim.indexOf(
                "public record PaymentOutboxClaim("
        );

        int recordEnd = claim.indexOf(
                ") {",
                recordStart
        );

        assertTrue(recordStart >= 0);
        assertTrue(recordEnd > recordStart);

        String recordComponents = claim.substring(
                recordStart,
                recordEnd
        );

        assertFalse(
                recordComponents.contains(
                        "PaymentOutboxEntity"
                ),
                "Claim record must not retain a JPA entity"
        );

        assertTrue(
                claim.contains("payload=[PROTECTED]")
        );
    }

    @Test
    void noSchemaOrSecondOutboxModelIsInvented()
            throws Exception {

        String service = Files.readString(
                OUTBOX_ROOT.resolve(
                        "claim/PaymentOutboxClaimService.java"
                )
        );

        for (String forbidden : List.of(
                "CREATE TABLE",
                "ALTER TABLE",
                "RETRY_PENDING",
                "DEAD_LETTER",
                "processing_started_at"
        )) {
            assertFalse(
                    service.contains(forbidden),
                    () -> "Claim implementation invents schema "
                            + "concept: "
                            + forbidden
            );
        }
    }
}
