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

        assertTrue(source.contains(
                "hasAuthority('SCOPE_observed-customer.read')"
        ));
        assertEquals(
                3,
                occurrences(source, "@PreAuthorize(REQUIRED_SCOPE)")
        );
        assertEquals(
                3,
                occurrences(
                        source,
                        "@RequestHeader(CORRELATION_HEADER)"
                )
        );
        assertTrue(source.contains(
                "X-Correlation-ID must be a valid UUID"
        ));
        assertTrue(source.contains(
                "response.setHeader(CORRELATION_HEADER"
        ));
    }

    @Test
    void observedCustomerApiExposesReadOnlyRoutes()
            throws Exception {

        String source = Files.readString(
                API.resolve(
                        "controller/ObservedCustomerQueryController.java"
                )
        );

        assertEquals(3, occurrences(source, "@GetMapping"));
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
