package com.sixpay.integration.logging;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SensitiveValueSanitizerTest {
    @Test
    void redactsBearerTokenAndMasksLongNumbers() {
        String result = new SensitiveValueSanitizer().sanitize(
                "Authorization: Bearer abc.def.ghi account=123456789012"
        );
        assertThat(result)
                .doesNotContain("abc.def.ghi")
                .doesNotContain("123456789012")
                .contains("Bearer [REDACTED]")
                .endsWith("********9012");
    }
}
