package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentApplicationPartnerNeutralityArchitectureTest {

    private static final Path APPLICATION =
            Path.of("src/main/java/com/sixpay/payment/application");

    @Test
    void applicationLayerDoesNotExposePartnerSpecificTypesOrSemantics() throws Exception {
        try (Stream<Path> paths = Files.walk(APPLICATION)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java")).toList()) {
                String content = Files.readString(path);
                assertThat(path.getFileName().toString())
                        .as("provider-specific application type in %s", path)
                        .doesNotContain("Partner").doesNotContain("tresor" + "pay");
                assertThat(content)
                        .as("provider-specific application semantics in %s", path)
                        .doesNotContain("Partner").doesNotContain("tresor" + "pay")
                        .doesNotContain("partner").doesNotContain("PARTNER")
                        .doesNotContain("PARTNER").doesNotContain(".partner.")
                        .doesNotContain("com.sixpay.partner.");
            }
        }
    }

    @Test
    void canonicalPaymentApplicationVocabularyExists() {
        assertThat(APPLICATION.resolve("command/InitiatePaymentCommand.java")).exists();
        assertThat(APPLICATION.resolve("command/PaymentBeneficiaryCommand.java")).exists();
        assertThat(APPLICATION.resolve("port/input/PaymentRecoveryUseCase.java")).exists();
        assertThat(APPLICATION.resolve("service/PaymentRecoveryService.java")).exists();
        assertThat(APPLICATION.resolve("view/PaymentRecoveryView.java")).exists();
        assertThat(APPLICATION.resolve("view/PaymentInitiationResult.java")).exists();
        assertThat(APPLICATION.resolve("view/PaymentInitiationStatus.java")).exists();
    }
}
