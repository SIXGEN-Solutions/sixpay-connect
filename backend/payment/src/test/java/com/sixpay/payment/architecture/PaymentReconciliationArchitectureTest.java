package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentReconciliationArchitectureTest {

    private static final Path SERVICE_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/application/service"
    );

    private static final Path PORT_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/application/port/in"
    );

    @Test
    void finalizationContainsNoTfjResponsibility()
            throws IOException {
        String service = Files.readString(
                SERVICE_ROOT.resolve(
                        "PaymentFinalizationService.java"
                )
        );

        String useCase = Files.readString(
                PORT_ROOT.resolve(
                        "PaymentFinalizationUseCase.java"
                )
        );

        for (String forbidden : List.of(
                "recordTfjConfirmation",
                "recordMatchedEndOfDayConfirmation",
                "EndOfDayConfirmationSnapshot",
                "UniqueTfjMatchProof"
        )) {
            assertFalse(
                    service.contains(forbidden),
                    () -> "Finalization service contains " + forbidden
            );
            assertFalse(
                    useCase.contains(forbidden),
                    () -> "Finalization use case contains " + forbidden
            );
        }
    }

    @Test
    void reconciliationContainsNoPostingExecution()
            throws IOException {
        String service = Files.readString(
                SERVICE_ROOT.resolve(
                        "PaymentReconciliationService.java"
                )
        );

        for (String forbidden : List.of(
                "PostingGateway",
                "PostingOutcomeSnapshot",
                "recordPostingOutcome",
                "resolvePostingOutcome",
                "authorizePosting",
                ".post("
        )) {
            assertFalse(
                    service.contains(forbidden),
                    () -> "Reconciliation contains posting concern: "
                            + forbidden
            );
        }

        assertTrue(
                service.contains(
                        "recordMatchedEndOfDayConfirmation"
                )
        );
    }

    @Test
    void reconciliationHasDedicatedInboundPort()
            throws IOException {
        String source = Files.readString(
                PORT_ROOT.resolve(
                        "PaymentReconciliationUseCase.java"
                )
        );

        assertTrue(source.contains("reconcileTfj"));
        assertTrue(source.contains("RecordTfjConfirmationCommand"));
        assertFalse(source.contains("PostingGateway"));
        assertFalse(source.contains("ReversalGateway"));
    }
}
