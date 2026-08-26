package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerProjectionBoundaryTest {

    private static final Path OUTPUT = Path.of(
            "src/main/java/com/sixpay/payment/application/port/output"
    );

    @Test
    void paymentOwnedProjectionContractHasNoCustomerOrInfrastructureType()
            throws Exception {

        for (String file : List.of(
                "ObservedCustomerProjectionPort.java",
                "ObservedCustomerProjectionRequest.java",
                "ObservedCustomerProjectionResult.java"
        )) {
            String source = Files.readString(OUTPUT.resolve(file));

            for (String forbidden : List.of(
                    "import com.sixpay.customer.",
                    "import org.springframework.",
                    "AmplitudeCustomerVerification",
                    "RestClient",
                    "HttpClient",
                    "JpaRepository",
                    "PaymentOutboxEntity"
            )) {
                assertFalse(
                        source.contains(forbidden),
                        () -> file + " contains " + forbidden
                );
            }
        }
    }

    @Test
    void requestProtectsSensitiveValuesInToString()
            throws Exception {

        String source = Files.readString(
                OUTPUT.resolve(
                        "ObservedCustomerProjectionRequest.java"
                )
        );

        for (String required : List.of(
                "normalizedNiu=[PROTECTED]",
                "legalName=[PROTECTED]",
                "accountBindingFingerprint=[PROTECTED]",
                "maskedAccountReference=[PROTECTED]"
        )) {
            assertTrue(source.contains(required));
        }
    }
}
