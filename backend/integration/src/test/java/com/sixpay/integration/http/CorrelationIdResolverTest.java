package com.sixpay.integration.http;

import com.sixpay.common.context.CorrelationId;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdResolverTest {
    @Test
    void preservesSafeIncomingValue() {
        CorrelationIdResolver resolver =
                new CorrelationIdResolver(() -> CorrelationId.of("generated"));
        assertThat(resolver.resolve("caller-123").value()).isEqualTo("caller-123");
    }

    @Test
    void replacesUnsafeIncomingValue() {
        CorrelationIdResolver resolver =
                new CorrelationIdResolver(() -> CorrelationId.of("generated"));
        assertThat(resolver.resolve("unsafe value").value()).isEqualTo("generated");
    }
}
