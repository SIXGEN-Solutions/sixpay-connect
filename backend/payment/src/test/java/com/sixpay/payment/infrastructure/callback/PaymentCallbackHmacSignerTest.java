package com.sixpay.payment.infrastructure.callback;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentCallbackHmacSignerTest {
    @Test
    void signsCanonicalContractInputWithHmacSha256() {
        PaymentCallbackProperties properties = new PaymentCallbackProperties();
        properties.setEnabled(true);
        properties.setSigningKeyId("partner-callback-2026-01");
        properties.setSigningSecret("local-test-secret-only");
        var result = new PaymentCallbackHmacSigner(properties).sign(
                "POST", "/callback?tenant=partner-a", "{\"eventId\":\"test\"}".getBytes(StandardCharsets.UTF_8), Instant.ofEpochSecond(1788000000L));
        assertThat(result.signature()).matches("^v1=[A-Za-z0-9_-]{43}$");
        assertThat(result.keyId()).isEqualTo("partner-callback-2026-01");
        assertThat(result.timestamp()).isEqualTo("1788000000");
    }
}
