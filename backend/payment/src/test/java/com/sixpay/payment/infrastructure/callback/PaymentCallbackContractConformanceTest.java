package com.sixpay.payment.infrastructure.callback;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentCallbackContractConformanceTest {
    @Test
    void callbackContractKeepsApprovedSemantics() throws Exception {
        String contract = Files.readString(Path.of("..", "..", "documentation", "contracts", "partner", "partner-payment-callback-webhook-v1.yaml"));
        assertThat(contract)
                .contains("CUT_CREDITED")
                .contains("TREASURY_INTEGRATED")
                .contains("X-Webhook-Event-ID")
                .contains("X-Webhook-Delivery-ID")
                .contains("X-Webhook-Delivery-Attempt")
                .contains("HMAC-SHA256")
                .contains("externalPaymentReference")
                .doesNotContain("PAYMENT_STATUS_CHANGED")
                .doesNotContain("bankDebitReference")
                .doesNotContain("bankCutCreditReference");
    }
}
