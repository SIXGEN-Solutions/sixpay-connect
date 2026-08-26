package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerLot487SecurityArchitectureTest {

    private static final Path API = Path.of(
            "src/main/java/com/sixpay/customer/observation/api"
    );

    @Test
    void allQueryEndpointsRequireTheExactScopeAndCorrelationHeader()
            throws Exception {

        String source = Files.readString(
                API.resolve(
                        "controller/ObservedCustomerQueryController.java"
                )
        );

        String compactSource = compact(source);

        assertTrue(
                source.contains(
                        "hasAuthority('SCOPE_observed-customer.read')"
                ),
                "The exact Observed Customer read scope is missing"
        );

        assertEquals(
                3,
                occurrences(
                        source,
                        "@PreAuthorize(REQUIRED_SCOPE)"
                ),
                "All three endpoints must require the read scope"
        );

        assertEquals(
                3,
                occurrences(
                        source,
                        "@RequestHeader(CORRELATION_HEADER)"
                ),
                "All three endpoints must require the correlation header"
        );

        assertTrue(
                source.contains(
                        "X-Correlation-ID must be a valid UUID"
                ),
                "Correlation ID UUID validation is missing"
        );

        assertTrue(
                compactSource.contains(
                        "response.setHeader("
                                + "CORRELATION_HEADER,"
                ),
                "The correlation ID must be propagated to the response"
        );
    }

    @Test
    void observedCustomerApiExposesReadOnlyRoutes()
            throws Exception {

        String source = Files.readString(
                API.resolve(
                        "controller/ObservedCustomerQueryController.java"
                )
        );

        assertEquals(
                3,
                occurrences(source, "@GetMapping"),
                "Exactly three GET endpoints are expected"
        );

        for (String mutation : List.of(
                "@PostMapping",
                "@PutMapping",
                "@PatchMapping",
                "@DeleteMapping"
        )) {
            assertFalse(
                    source.contains(mutation),
                    () -> "Mutation endpoint exposed: " + mutation
            );
        }
    }

    @Test
    void errorHandlingDoesNotRevealSensitiveResources()
            throws Exception {

        Path handler = API.resolve(
                "error/ObservedCustomerQueryExceptionHandler.java"
        );

        String source = Files.readString(handler);

        for (String forbidden : List.of(
                "exception.toString()",
                "exception.getCause()",
                "exception.getStackTrace()",
                "normalizedNiu",
                "legalName",
                "accountBindingFingerprint",
                "maskedAccountReference",
                "payload",
                "jdbc:",
                "SELECT ",
                "INSERT ",
                "UPDATE ",
                "DELETE "
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Unsafe error concept: " + forbidden
            );
        }
    }

    private static String compact(
            String source
    ) {
        return source.replaceAll(
                "\\s+",
                ""
        );
    }

    private static int occurrences(
            String source,
            String token
    ) {
        int count = 0;
        int offset = 0;

        while ((offset = source.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }

        return count;
    }
}