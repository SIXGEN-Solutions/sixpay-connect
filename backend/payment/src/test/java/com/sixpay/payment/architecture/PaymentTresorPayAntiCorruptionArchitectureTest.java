package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTresorPayAntiCorruptionArchitectureTest {

    private static final Path API = Path.of("src/main/java/com/sixpay/payment/api");
    private static final Path TRESOR_PAY = API.resolve("partner/tresorpay");

    @Test
    void tresorPayWireTypesAreConfinedToProviderBoundary() {
        assertThat(TRESOR_PAY.resolve("TresorPayPaymentCommandController.java")).exists();
        assertThat(TRESOR_PAY.resolve("TresorPayPaymentApiMapper.java")).exists();
        assertThat(TRESOR_PAY.resolve("TresorPayPaymentRecoveryController.java")).exists();
        assertThat(TRESOR_PAY.resolve("request/InitiateDebitRequest.java")).exists();
        assertThat(TRESOR_PAY.resolve("request/InitiateDebitBeneficiaryRequest.java")).exists();
        assertThat(TRESOR_PAY.resolve("response/InitiateDebitResponse.java")).exists();
        assertThat(TRESOR_PAY.resolve("response/TresorPayPaymentRecoveryResponse.java")).exists();
    }

    @Test
    void genericApiPackagesDoNotOwnTresorPayWireDtos() {
        for (String relative : List.of(
                "request/InitiateDebitRequest.java",
                "request/InitiateDebitBeneficiaryRequest.java",
                "response/InitiateDebitResponse.java",
                "response/TresorPayPaymentRecoveryResponse.java")) {
            assertThat(API.resolve(relative)).doesNotExist();
        }
    }

    @Test
    void providerBoundaryKeepsContractedWireVocabulary() throws Exception {
        String request = Files.readString(TRESOR_PAY.resolve("request/InitiateDebitRequest.java"));
        String controller = Files.readString(TRESOR_PAY.resolve("TresorPayPaymentCommandController.java"));

        assertThat(request).contains(
                "@JsonProperty(\"LoginName\")",
                "@JsonProperty(\"AppID\")",
                "@JsonProperty(\"montantTotal\")",
                "@JsonProperty(\"NUI\")",
                "@JsonProperty(\"callbackURL\")"
        );
        assertThat(controller).contains(
                "@RequestMapping(\"/v1/payments\")",
                "@PostMapping(\"/initiate\")",
                "SCOPE_payment.initiate"
        );
    }
    @Test
    void genericPreparationDoesNotSelectTresorPaySource() throws Exception {
        String preparation = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/payment/"
                                + "infrastructure/initiation/"
                                + "PaymentInitiationPreparationAdapter.java"
                )
        );

        assertThat(preparation)
                .contains("command.source()")
                .doesNotContain("PaymentSource.TRESOR_PAY");
    }

}
