package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentProjectionAdaptersArchitectureTest {

    private static final Path PERSISTENCE = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "infrastructure/query"
    );

    @Test
    void projectionAdapterImplementsReadPort()
            throws IOException {
        String source = Files.readString(
                PERSISTENCE.resolve(
                        "PaymentProjectionReadAdapter.java"
                )
        );

        assertTrue(source.contains(
                "implements PaymentProjectionReadPort"
        ));
        assertTrue(source.contains(
                "NamedParameterJdbcTemplate"
        ));
        assertTrue(source.contains(
                "state_payload"
        ));
        assertFalse(source.contains(
                "Payment.reconstitute"
        ));
        assertFalse(source.contains(
                "PaymentRepository"
        ));
    }

    @Test
    void objectAccessAdapterImplementsAccessPort()
            throws IOException {
        String source = Files.readString(
                PERSISTENCE.resolve(
                        "PaymentObjectAccessAdapter.java"
                )
        );

        assertTrue(source.contains(
                "implements PaymentObjectAccessPort"
        ));
        assertFalse(source.contains(
                "PaymentJpaEntity"
        ));
        assertFalse(source.contains(
                "PaymentRepository"
        ));
    }

    @Test
    void partnerSearchIsFailClosedUntilOwnerIsPersisted()
            throws IOException {
        String source = Files.readString(
                PERSISTENCE.resolve(
                        "PaymentProjectionReadAdapter.java"
                )
        );

        assertTrue(source.contains(
                "visibility instanceof "
                        + "PaymentVisibilityScope.Partner"
        ));
        assertTrue(source.contains(
                "return emptyPage"
        ));
    }
}
