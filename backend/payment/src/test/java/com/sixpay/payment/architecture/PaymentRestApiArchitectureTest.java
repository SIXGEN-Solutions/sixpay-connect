package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentRestApiArchitectureTest {

    private static final Path API_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/api"
    );

    @Test
    void exposesOnlyContractedReadEndpoints()
            throws IOException {
        String controller = Files.readString(
                API_ROOT.resolve(
                        "PaymentQueryController.java"
                )
        );

        assertTrue(controller.contains(
                "@RequestMapping(\"/internal/api/v1/payments\")"
        ));
        assertTrue(controller.contains("@GetMapping"));

        for (String forbidden : List.of(
                "@PostMapping",
                "@PutMapping",
                "@PatchMapping",
                "@DeleteMapping"
        )) {
            assertFalse(controller.contains(forbidden));
        }
    }

    @Test
    void apiAdapterNeverLoadsAggregate()
            throws IOException {
        try (Stream<Path> paths = Files.walk(API_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);
                            return List.of(
                                    "domain.model.Payment;",
                                    "PaymentRepository",
                                    "PaymentJpaEntity",
                                    "PaymentStateDocument"
                            ).stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains " + token
                                    );
                        } catch (IOException exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();

            assertEquals(List.of(), violations);
        }
    }

    @Test
    void controllerDelegatesSecurityToPaymentPolicy()
            throws IOException {
        String controller = Files.readString(
                API_ROOT.resolve(
                        "PaymentQueryController.java"
                )
        );
        String accessPolicy = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/payment/"
                                + "application/security/"
                                + "PaymentAccessPolicy.java"
                )
        );
        String authority = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/payment/"
                                + "application/security/"
                                + "PaymentAuthority.java"
                )
        );

        assertTrue(controller.contains(
                "@paymentAccessPolicy.canSearch()"
        ));
        assertTrue(controller.contains(
                "@paymentAccessPolicy.canRead()"
        ));
        assertFalse(controller.contains(
                "@ConditionalOnBean"
        ));
        assertTrue(accessPolicy.contains(
                "case SEARCH, READ -> PaymentAuthority.READ"
        ));
        assertTrue(authority.contains(
                "READ(\"SCOPE_payment.read\")"
        ));
    }
}
