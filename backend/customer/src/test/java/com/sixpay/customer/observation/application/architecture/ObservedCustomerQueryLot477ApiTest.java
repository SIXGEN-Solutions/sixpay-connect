package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerQueryLot477ApiTest {

    private static final Path API = Path.of(
            "src/main/java/com/sixpay/customer/observation/api"
    );

    @Test
    void controllerMatchesPublishedRoutesScopeAndCorrelationContract()
            throws Exception {

        String source = Files.readString(
                API.resolve(
                        "controller/"
                                + "ObservedCustomerQueryController.java"
                )
        );

        String compactSource = compact(source);

        /*
         * Tokens whose formatting is stable.
         */
        for (String required : List.of(
                "@RequestMapping("
                        + "\"/internal/api/v1/observed-customers\"",
                "@GetMapping",
                "@GetMapping(\"/{observedCustomerId}\")",
                "@GetMapping(\"/{observedCustomerId}/payments\")",
                "SCOPE_observed-customer.read",
                "@RequestHeader(CORRELATION_HEADER)",
                "X-Correlation-ID"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing API contract behavior: "
                            + required
            );
        }

        /*
         * Formatting-insensitive checks.
         */
        assertTrue(
                compactSource.contains(
                        "@RequestParam("
                                + "name=\"niu\","
                                + "required=false"
                                + ")"
                ),
                "Missing NIU request parameter declaration"
        );

        assertTrue(
                compactSource.contains(
                        "UUID.fromString("
                                + "value.strip()"
                                + ")"
                ),
                "Missing correlation ID UUID validation"
        );

        assertTrue(
                compactSource.contains(
                        "response.setHeader("
                                + "CORRELATION_HEADER,"
                ),
                "Missing response correlation header propagation"
        );

        for (String forbidden : List.of(
                "@PostMapping",
                "@PutMapping",
                "@PatchMapping",
                "@DeleteMapping",
                "EntityManager",
                "JdbcTemplate",
                "ObservedCustomerDataProtector",
                "import com.sixpay.payment.",
                "Amplitude"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden controller concept: "
                            + forbidden
            );
        }
    }

    @Test
    void handlerSupportsRequiredHttpStatusesWithoutSensitiveEcho()
            throws Exception {

        String source = Files.readString(
                API.resolve(
                        "error/"
                                + "ObservedCustomerQueryExceptionHandler.java"
                )
        );

        for (String required : List.of(
                "HttpStatus.BAD_REQUEST",
                "HttpStatus.NOT_FOUND",
                "HttpStatus.TOO_MANY_REQUESTS",
                "HttpStatus.INTERNAL_SERVER_ERROR",
                "HttpStatus.SERVICE_UNAVAILABLE"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing handler status: "
                            + required
            );
        }

        for (String forbidden : List.of(
                "normalizedNiu",
                "legalName",
                "accountBindingFingerprint",
                "maskedAccountReference",
                "exception.toString()",
                "exception.getCause()"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Sensitive error behavior: "
                            + forbidden
            );
        }
    }

    @Test
    void paymentDtoMatchesNestedAmountOpenApiShape()
            throws Exception {

        String dto = Files.readString(
                API.resolve(
                        "dto/ObservedCustomerPaymentResponse.java"
                )
        );

        String mapper = Files.readString(
                API.resolve(
                        "mapper/ObservedCustomerQueryApiMapper.java"
                )
        );

        assertTrue(
                dto.contains("AmountResponse amount")
        );

        assertTrue(
                dto.contains(
                        "public record AmountResponse("
                )
        );

        assertTrue(
                dto.contains("BigDecimal amount")
        );

        assertTrue(
                dto.contains("String currency")
        );

        assertTrue(
                mapper.contains(
                        "new ObservedCustomerPaymentResponse"
                                + ".AmountResponse("
                )
        );

        assertFalse(
                dto.contains("String accountNumber")
        );

        assertFalse(
                dto.contains(
                        "accountBindingFingerprint"
                )
        );
    }

    @Test
    void apiObservabilityUsesOnlyBoundedMetricTags()
            throws Exception {

        String source = Files.readString(
                API.resolve(
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
                    () -> "Missing query metric: " + metric
            );
        }

        for (String forbidden : List.of(
                ".tag(\"observedCustomerId\"",
                ".tag(\"correlationId\"",
                ".tag(\"niu\"",
                ".tag(\"cursor\"",
                ".tag(\"account\""
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden metric tag: "
                            + forbidden
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
}