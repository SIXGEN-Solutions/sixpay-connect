package com.sixpay.payment.architecture;

import com.sixpay.payment.domain.model.PaymentSource;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPartnerNeutralityArchitectureTest {

    private static final Path DOMAIN =
            Path.of("src/main/java/com/sixpay/payment/domain");

    @Test
    void paymentSourceAcceptsProviderNeutralIdentifiers() {
        assertThat(PaymentSource.of("PARTNER_A").value())
                .isEqualTo("PARTNER_A");
        assertThat(PaymentSource.TRESOR_PAY.value())
                .isEqualTo("TRESOR_PAY");
    }

    @Test
    void domainDoesNotEnforceTresorPayAsTheOnlyPaymentOrigin()
            throws Exception {

        String paymentState = Files.readString(
                DOMAIN.resolve("model/PaymentState.java")
        );

        assertThat(paymentState)
                .doesNotContain("source != PaymentSource.TRESOR_PAY")
                .doesNotContain("Payment source must be TRESOR_PAY");
    }

    @Test
    void prioritizedDomainTypesRemainProviderNeutralInDocumentation()
            throws Exception {

        for (String relative : new String[] {
                "model/PaymentInitiationContext.java",
                "model/NewPaymentIntent.java",
                "model/ExternalSubscriptionReference.java"
        }) {
            assertThat(Files.readString(DOMAIN.resolve(relative)))
                    .doesNotContain("TresorPay")
                    .doesNotContain("TRESOR PAY");
        }
    }
}
