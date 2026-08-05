package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerQuerySecurityObservabilityArchitectureTest {

    private static final Path API_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/api"
    );

    @Test
    void allEndpointsRequireExactReadScopeAndCorrelationHeader()
            throws Exception {

        String source = Files.readString(
                API_ROOT.resolve(
                        "controller/"
                                + "ObservedCustomerQueryController.java"
                )
        );

        assertTrue(source.contains(
                "hasAuthority('SCOPE_observed-customer.read')"
        ));

        assertEqualsOccurrences(
                source,
                "@PreAuthorize(REQUIRED_SCOPE)",
                3
        );

        assertEqualsOccurrences(
                source,
                "@RequestHeader(CORRELATION_HEADER)",
                3
        );

        assertTrue(source.contains(
                "X-Correlation-ID"
        ));
        assertTrue(source.contains(
                "response.setHeader("
        ));
    }

    @Test
    void metricsUseOnlyBoundedTags()
            throws Exception {

        String source = Files.readString(
                API_ROOT.resolve(
                        "observability/"
                                + "ObservedCustomerQueryObservation.java"
                )
        );

        for (String metric : List.of(
                "sixpay.customer.observation.query.requests",
                "sixpay.customer.observation.query.duration",
                "sixpay.customer.observation.query.results",
                "sixpay.customer.observation.query.failures"
        )) {
            assertTrue(
                    source.contains(metric),
                    () -> "Missing metric: " + metric
            );
        }

        for (String allowedTag : List.of(
                "\"operation\"",
                "\"result\"",
                "\"error_type\""
        )) {
            assertTrue(
                    source.contains(allowedTag),
                    () -> "Missing bounded tag: "
                            + allowedTag
            );
        }

        for (String forbiddenTag : List.of(
                ".tag(\"observedCustomerId\"",
                ".tag(\"correlationId\"",
                ".tag(\"niu\"",
                ".tag(\"legalName\"",
                ".tag(\"email\"",
                ".tag(\"phone\"",
                ".tag(\"cursor\"",
                ".tag(\"account\""
        )) {
            assertFalse(
                    source.contains(forbiddenTag),
                    () -> "Forbidden metric tag: "
                            + forbiddenTag
            );
        }
    }

    @Test
    void safeLogsContainOnlyApprovedOperationalFields()
            throws Exception {

        String source = Files.readString(
                API_ROOT.resolve(
                        "observability/"
                                + "ObservedCustomerQueryObservation.java"
                )
        );

        for (String allowed : List.of(
                "observedCustomerId={}",
                "correlationId={}",
                "operation={}",
                "result={}",
                "durationMs={}",
                "pageSize={}",
                "hasMore={}"
        )) {
            assertTrue(
                    source.contains(allowed),
                    () -> "Missing safe log field: "
                            + allowed
            );
        }

        for (String forbidden : List.of(
                "normalizedNiu",
                "legalName",
                "email",
                "phone",
                "accountBindingFingerprint",
                "maskedAccountReference",
                "cursor={}",
                "payload",
                "response={}",
                "resultObject"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Sensitive log concept: "
                            + forbidden
            );
        }
    }

    @Test
    void handlerCoversRequiredHttpFailureClasses()
            throws Exception {

        String source = Files.readString(
                API_ROOT.resolve(
                        "error/"
                                + "ObservedCustomerQueryExceptionHandler.java"
                )
        );

        for (String required : List.of(
                "HttpStatus.BAD_REQUEST",
                "HttpStatus.NOT_FOUND",
                "HttpStatus.TOO_MANY_REQUESTS",
                "HttpStatus.INTERNAL_SERVER_ERROR",
                "HttpStatus.SERVICE_UNAVAILABLE",
                "MissingRequestHeaderException",
                "MethodArgumentTypeMismatchException"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing error mapping: "
                            + required
            );
        }

        assertFalse(source.contains("exception.toString()"));
        assertFalse(source.contains("exception.getCause()"));
    }

    @Test
    void methodSecurityIsEnabledOnlyInServletApiComposition()
            throws Exception {

        String source = Files.readString(
                API_ROOT.resolve(
                        "configuration/"
                                + "ObservedCustomerQueryApiConfiguration.java"
                )
        );

        assertTrue(source.contains("@EnableMethodSecurity"));
        assertTrue(source.contains(
                "ConditionalOnWebApplication.Type.SERVLET"
        ));
    }

    private static void assertEqualsOccurrences(
            String source,
            String token,
            int expected
    ) {
        int count = 0;
        int index = 0;

        while ((index = source.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }

        int actualCount = count;

        assertTrue(
                actualCount == expected,
                () -> token
                        + " expected "
                        + expected
                        + " times but was "
                        + actualCount
        );
    }
}
