package com.sixpay.reporting.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlledAuditExportArchitectureTest {

    private static final Path ROOT =
            Path.of("src/main/java/com/sixpay/reporting");

    @Test
    void exportControllerMatchesContractWorkflow()
            throws Exception {

        String controller = Files.readString(
                ROOT.resolve(
                        "api/controller/"
                                + "PaymentAuditExportController.java"
                )
        );

        assertTrue(controller.contains(
                "/internal/api/v1/payment-audit-exports"
        ));
        assertTrue(controller.contains("@PostMapping"));
        assertTrue(controller.contains("@GetMapping(\"/{exportId}\")"));
        assertTrue(controller.contains("Idempotency-Key"));
        assertTrue(controller.contains("ResponseEntity.accepted()"));
        assertTrue(controller.contains("HttpHeaders.LOCATION"));
    }

    @Test
    void exportRequiresBothScopes()
            throws Exception {

        String controller = Files.readString(
                ROOT.resolve(
                        "api/controller/"
                                + "PaymentAuditExportController.java"
                )
        );

        assertTrue(controller.contains(
                "SCOPE_payment.audit.read"
        ));
        assertTrue(controller.contains(
                "SCOPE_payment.audit.export"
        ));
    }

    @Test
    void exportIsNotKafkaReplay()
            throws Exception {

        String worker = Files.readString(
                ROOT.resolve(
                        "infrastructure/export/"
                                + "AsyncPaymentAuditExportWorker.java"
                )
        );

        assertFalse(worker.contains("Kafka"));
        assertFalse(worker.contains("replay"));
        assertFalse(worker.contains("DeadLetter"));
        assertTrue(worker.contains("ExecutorService"));
        assertTrue(worker.contains("findAccepted("));
    }

    @Test
    void durableAcceptancePrecedesDispatch()
            throws Exception {

        String service = Files.readString(
                ROOT.resolve(
                        "application/service/"
                                + "PaymentAuditExportService.java"
                )
        );

        int accept = service.indexOf("jobStore.accept(");
        int dispatch = service.indexOf("dispatchPort.dispatch(");

        assertTrue(accept >= 0);
        assertTrue(dispatch > accept);
    }

    @Test
    void idempotencyConflictIsMappedTo409()
            throws Exception {

        String handler = Files.readString(
                ROOT.resolve(
                        "api/exception/"
                                + "PaymentAuditQueryExceptionHandler.java"
                )
        );

        assertTrue(handler.contains("HttpStatus.CONFLICT"));
        assertTrue(handler.contains(
                "HttpStatus.UNPROCESSABLE_ENTITY"
        ));
    }

}
