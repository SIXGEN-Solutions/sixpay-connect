package com.sixpay.accounting.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.assertj.core.api.Assertions.assertThat;

class AccountingT1ManualExecutionControllerActivationTest {

    @Test
    void requiresBothExternalT1Capabilities() {
        ConditionalOnProperty condition =
                AccountingT1ManualExecutionController.class
                        .getAnnotation(ConditionalOnProperty.class);

        assertThat(condition)
                .isNotNull();

        assertThat(condition.prefix())
                .isEqualTo("sixpay.accounting");

        assertThat(condition.name())
                .containsExactlyInAnyOrder(
                        "api.enabled",
                        "tresorpay-status.enabled"
                );

        assertThat(condition.havingValue())
                .isEqualTo("true");

        assertThat(condition.matchIfMissing())
                .isFalse();
    }
}
