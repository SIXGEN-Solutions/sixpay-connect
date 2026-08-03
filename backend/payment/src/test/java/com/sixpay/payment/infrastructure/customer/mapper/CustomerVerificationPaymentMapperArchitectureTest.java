package com.sixpay.payment.infrastructure.customer.mapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerVerificationPaymentMapperArchitectureTest {

    private static final Path MAPPER = Path.of(
            "src/main/java/com/sixpay/payment/infrastructure/"
                    + "customer/mapper/"
                    + "CustomerVerificationPaymentMapper.java"
    );

    @Test
    void mapperRemainsPaymentOwnedAndCustomerFree()
            throws Exception {

        String source = Files.readString(MAPPER);

        for (String forbidden : List.of(
                "import com.sixpay.customer.",
                "import com.sixpay.customer.verification."
                        + "application.port.input.VerifyCustomerResult;",
                "import com.sixpay.customer.verification."
                        + "domain.model.VerificationOutcome;",
                "import com.sixpay.customer.verification."
                        + "domain.model.VerificationCheck;",
                "import com.sixpay.customer.verification."
                        + "domain.model.VerificationCheckType;",
                "import com.sixpay.customer.verification."
                        + "domain.model.VerificationCheckResult;",
                "AmplitudeCustomerVerificationRequest",
                "AmplitudeCustomerVerificationResponse",
                "AmplitudeVerificationCheckResponse",
                "AmplitudeErrorResponse",
                "RestClient",
                "WebClient",
                "HttpClient",
                "HttpStatus",
                ".valueOf("
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Mapper contains forbidden dependency "
                            + "or implicit enum conversion: "
                            + forbidden
            );
        }

        for (String required : List.of(
                "switch (outcome)",
                "switch (type)",
                "switch (result)",
                "case CUSTOMER_EXISTS",
                "case REQUIRED_KYC_VERIFIED",
                "case VERIFIED",
                "case REJECTED",
                "case INDETERMINATE",
                "case PASS",
                "case FAIL",
                "case UNKNOWN"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Explicit mapping missing: "
                            + required
            );
        }
    }

    @Test
    void mapperCreatesCanonicalPaymentEvidence()
            throws Exception {

        String source = Files.readString(MAPPER);

        for (String required : List.of(
                "new BankingVerificationSnapshot(",
                "new BankingVerificationId(",
                "new BankingVerificationCheckEvidence(",
                "new EvidenceMetadata(",
                "ExternalSystem.AMPLITUDE",
                "EvidenceObservationChannel.DIRECT_RESPONSE",
                "EvidenceFingerprint.of("
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Canonical evidence construction missing: "
                            + required
            );
        }
    }
}