package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerQueryContractConformanceTest {

    private static final Path CUSTOMER = Path.of(
            "src/main/java/com/sixpay/customer/observation"
    );

    @Test
    void httpApiDoesNotExposeSnapshotAtAsRequestParameter()
            throws Exception {

        String controller = Files.readString(
                CUSTOMER.resolve(
                        "api/controller/ObservedCustomerQueryController.java"
                )
        );

        assertFalse(controller.contains("Instant snapshotAt"));
        assertTrue(controller.contains("snapshot(effectiveCursor)"));
    }

    @Test
    void cursorRestoresServerOwnedSnapshot()
            throws Exception {

        String codec = Files.readString(
                CUSTOMER.resolve(
                        "infrastructure/query/cursor/"
                                + "HmacObservedCustomerCursorCodec.java"
                )
        );

        /*
         * snapshotAt is serialized into the authenticated cursor
         * and restored while decoding continuation requests.
         */
        assertTrue(
                codec.contains(
                        "writeInstant(output, criteria.snapshotAt())"
                )
        );

        assertTrue(
                codec.contains(
                        "Instant snapshotAt = readInstant(input);"
                )
        );

        /*
         * Continuation requests must no longer provide their own
         * snapshotAt value.
         */
        assertFalse(
                codec.contains(
                        "cursor snapshot does not match the request"
                )
        );

        /*
         * The cursor itself remains authenticated.
         */
        assertTrue(
                codec.contains("MessageDigest.isEqual(")
        );

        assertTrue(
                codec.contains("HmacSHA256")
        );
    }

    @Test
    void queryViewsRemainMasked()
            throws Exception {

        String mapper = Files.readString(
                CUSTOMER.resolve(
                        "infrastructure/query/mapper/"
                                + "ObservedCustomerQueryRowMapper.java"
                )
        );

        assertTrue(mapper.contains("maskNiu(niu)"));
        assertTrue(mapper.contains("account.maskedValue()"));
        assertFalse(mapper.contains("account.clearValue()"));
    }

    @Test
    void problemDetailsCarryCorrelationAndRetryHeaders()
            throws Exception {

        String handler = Files.readString(
                CUSTOMER.resolve(
                        "api/error/ObservedCustomerQueryExceptionHandler.java"
                )
        );

        assertTrue(handler.contains("MediaType.APPLICATION_PROBLEM_JSON"));
        assertTrue(handler.contains("\"correlationId\""));
        assertTrue(handler.contains("HttpHeaders.RETRY_AFTER"));
        assertTrue(handler.contains("X-Correlation-ID"));
    }

    @Test
    void readScopeRemainsContractual()
            throws Exception {

        String controller = Files.readString(
                CUSTOMER.resolve(
                        "api/controller/ObservedCustomerQueryController.java"
                )
        );

        assertTrue(controller.contains("SCOPE_observed-customer.read"));
    }
}
