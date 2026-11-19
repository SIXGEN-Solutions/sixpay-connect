package com.sixpay.bootstrap.architecture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreBankingMtlsAlignmentArchitectureTest {
    @Test
    void outboundBankingClientsDoNotUseOauth2Bearer() throws Exception {
        Path root = Path.of("..");
        List<String> files = List.of(
            "payment/src/main/java/com/sixpay/payment/infrastructure/banking/amplitude/posting/client/RestAmplitudePaymentEventClient.java",
            "payment/src/main/java/com/sixpay/payment/infrastructure/banking/amplitude/posting/client/RestAmplitudePaymentEventRecoveryClient.java",
            "payment/src/main/java/com/sixpay/payment/infrastructure/banking/amplitude/confirmation/client/RestAmplitudePaymentConfirmationClient.java",
            "payment/src/main/java/com/sixpay/payment/infrastructure/banking/amplitude/reservation/client/RestAmplitudeFundsReservationClient.java",
            "payment/src/main/java/com/sixpay/payment/infrastructure/banking/amplitude/release/client/RestAmplitudeFundsReleaseClient.java",
            "payment/src/main/java/com/sixpay/payment/infrastructure/banking/amplitude/reversal/client/RestAmplitudeReversalClient.java",
            "payment/src/main/java/com/sixpay/payment/infrastructure/banking/amplitude/treasury/RestTreasuryAccountLookupClient.java",
            "accounting/src/main/java/com/sixpay/accounting/infrastructure/accountingapi/client/RestAccountingBatchClient.java",
            "accounting/src/main/java/com/sixpay/accounting/infrastructure/tfj/client/AmplitudeTfjLookupClient.java"
        );
        for (String name : files) {
            String source = Files.readString(root.resolve(name));
            assertFalse(source.contains("setBearerAuth("), name);
            assertFalse(source.contains("HttpHeaders.AUTHORIZATION"), name);
            assertFalse(source.contains("tokenProvider"), name);
        }
    }

    @Test
    void onlyTwoAllowedProfilesUseUnifiedMtlsFlag() throws Exception {
        Path profiles = Path.of("src/main/resources");
        for (String name : List.of("application-production.yml",
                "application-payment-banking-capabilities.yml")) {
            String source = Files.readString(profiles.resolve(name));
            assertTrue(source.contains("SIXPAY_BANKING_MTLS_ENABLED:false"), name);
            assertFalse(source.contains("oauth2-enabled:"), name);
            assertFalse(source.contains("AMPLITUDE_PAYMENT_OAUTH2_"), name);
        }
    }
}
