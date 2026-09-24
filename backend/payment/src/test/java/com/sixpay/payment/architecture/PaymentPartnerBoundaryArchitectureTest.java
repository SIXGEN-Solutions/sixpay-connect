package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentPartnerBoundaryArchitectureTest {
    private static final Path PAYMENT = Path.of("src/main/java/com/sixpay/payment");
    private static final Path PARTNER = PAYMENT.resolve("api/partner");

    @Test
    void paymentCommandBoundaryIsPartnerGeneric() {
        assertThat(PARTNER.resolve("PartnerPaymentCommandController.java")).exists();
        assertThat(PARTNER.resolve("PartnerPaymentApiMapper.java")).exists();
        assertThat(PARTNER.resolve("request/InitiateDebitRequest.java")).exists();
        assertThat(PARTNER.resolve("request/InitiateDebitBeneficiaryRequest.java")).exists();
        assertThat(PARTNER.resolve("tresorpay/TresorPayPaymentCommandController.java")).doesNotExist();
        assertThat(PARTNER.resolve("tresorpay/TresorPayPaymentApiMapper.java")).doesNotExist();
    }

    @Test
    void genericPreparationDoesNotSelectProviderSpecificSource() throws Exception {
        String preparation = Files.readString(PAYMENT.resolve("infrastructure/initiation/PaymentInitiationPreparationAdapter.java"));
        assertThat(preparation).contains("command.source()").doesNotContain("PaymentSource.of(\"TRESOR_PAY\")");
    }
}
