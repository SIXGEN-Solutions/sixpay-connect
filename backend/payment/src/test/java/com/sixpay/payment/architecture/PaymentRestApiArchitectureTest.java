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

    private static final Path WEB_ROOT =
            Path.of(
                    "src/main/java/com/sixpay/payment/"
                            + "infrastructure/web"
            );

    private static final Path SECURITY_ROOT =
            Path.of(
                    "src/main/java/com/sixpay/payment/"
                            + "application/security"
            );

    @Test
    void exposesOnlyContractedReadEndpoints()
            throws IOException {

        String controller = Files.readString(
                WEB_ROOT.resolve(
                        "PaymentQueryController.java"
                )
        );

        assertTrue(
                controller.contains(
                        "@RequestMapping("
                                + "\"/internal/api/v1/payments\""
                                + ")"
                )
        );

        assertTrue(
                controller.contains("@GetMapping")
        );

        assertTrue(
                controller.contains(
                        "@GetMapping(\"/{paymentId}\")"
                )
        );

        for (String forbidden : List.of(
                "@PostMapping",
                "@PutMapping",
                "@PatchMapping",
                "@DeleteMapping"
        )) {
            assertFalse(
                    controller.contains(forbidden),
                    () -> "Payment query controller contains "
                            + forbidden
            );
        }
    }

    @Test
    void restAdapterNeverLoadsAggregate()
            throws IOException {

        List<String> forbiddenTokens = List.of(
                "domain.model.Payment;",
                "PaymentRepository",
                "PaymentJpaEntity",
                "PaymentStateDocument"
        );

        try (Stream<Path> paths = Files.walk(WEB_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return forbiddenTokens.stream()
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

            assertEquals(
                    List.of(),
                    violations
            );
        }
    }

    @Test
    void controllerRequiresReadPolicyAndProjectionPort()
            throws IOException {

        String controller = Files.readString(
                WEB_ROOT.resolve(
                        "PaymentQueryController.java"
                )
        );

        String accessPolicy = Files.readString(
                SECURITY_ROOT.resolve(
                        "PaymentAccessPolicy.java"
                )
        );

        String authority = Files.readString(
                SECURITY_ROOT.resolve(
                        "PaymentAuthority.java"
                )
        );

        assertTrue(
                controller.contains(
                        "@PreAuthorize("
                                + "\"@paymentAccessPolicy.canSearch()\""
                                + ")"
                )
        );

        assertTrue(
                controller.contains(
                        "@PreAuthorize("
                                + "\"@paymentAccessPolicy.canRead()\""
                                + ")"
                )
        );

        assertTrue(
                controller.contains(
                        "@ConditionalOnBean("
                                + "PaymentProjectionQueryUseCase.class"
                                + ")"
                )
        );

        assertTrue(
                accessPolicy.contains(
                        "case SEARCH, READ -> PaymentAuthority.READ"
                )
        );

        assertTrue(
                authority.contains(
                        "READ(\"SCOPE_payment.read\")"
                )
        );
    }

    @Test
    void controllerDoesNotDuplicateAuthorizationRules()
            throws IOException {

        String controller = Files.readString(
                WEB_ROOT.resolve(
                        "PaymentQueryController.java"
                )
        );

        assertFalse(
                controller.contains(
                        "hasAuthority('SCOPE_payment.read')"
                )
        );

        assertFalse(
                controller.contains(
                        "hasRole("
                )
        );
    }
}