package com.sixpay.bootstrap.readiness;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentQueryContractTest {

    private static final Path ROOT =
            Path.of("../..").normalize();

    @Test
    void openApiAndSpringMvcExposeSamePaymentOperations()
            throws Exception {

        String contract = Files.readString(
                ROOT.resolve(
                        "documentation/contracts/internal/"
                                + "payment-query-api-v1.yaml"
                )
        );
        String controller = Files.readString(
                ROOT.resolve(
                        "backend/payment/src/main/java/"
                                + "com/sixpay/payment/api/"
                                + "PaymentQueryController.java"
                )
        );

        assertTrue(contract.contains(
                "/internal/api/v1/payments:"
        ));
        assertTrue(contract.contains(
                "/internal/api/v1/payments/{paymentId}:"
        ));

        assertTrue(controller.contains(
                "@RequestMapping(\"/internal/api/v1/payments\")"
        ));
        assertTrue(controller.contains("@GetMapping"));
        assertTrue(controller.contains(
                "@GetMapping(\"/{paymentId}\")"
        ));

        assertTrue(contract.contains("payment.read"));
        assertTrue(
                Files.readString(
                        ROOT.resolve(
                                "backend/payment/src/main/java/"
                                        + "com/sixpay/payment/"
                                        + "application/security/"
                                        + "PaymentAuthority.java"
                        )
                ).contains(
                        "SCOPE_payment.read"
                )
        );
    }
}
