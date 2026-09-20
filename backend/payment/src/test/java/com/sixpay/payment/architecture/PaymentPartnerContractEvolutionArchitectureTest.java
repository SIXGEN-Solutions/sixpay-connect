package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPartnerContractEvolutionArchitectureTest {

    private static final Path PAYMENT_API =
            Path.of("src/main/java/com/sixpay/payment/api");
    private static final Path CONTRACTS =
            Path.of("../../documentation/contracts");

    @Test
    void paymentDoesNotPreemptivelyExposeGenericPartnerWireTypes() throws Exception {
        List<String> forbiddenTypeNames = List.of(
                "PartnerPaymentRequest.java",
                "PartnerPaymentResponse.java",
                "GenericPartnerPaymentRequest.java",
                "GenericPartnerPaymentResponse.java",
                "StandardPartnerPaymentRequest.java",
                "StandardPartnerPaymentResponse.java"
        );

        try (Stream<Path> paths = Files.walk(PAYMENT_API)) {
            List<String> observed = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(forbiddenTypeNames::contains)
                    .toList();

            assertThat(observed)
                    .as("generic external Partner wire types require a demonstrated second Partner and explicit approval")
                    .isEmpty();
        }
    }

    @Test
    void noGenericPartnerPaymentContractExistsWithoutExplicitGovernanceDecision()
            throws Exception {
        if (!Files.exists(CONTRACTS)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(CONTRACTS)) {
            List<Path> genericContracts = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.contains("partner")
                                && name.contains("payment")
                                && !path.toString().replace('\\', '/').contains("/tresorpay/");
                    })
                    .toList();

            assertThat(genericContracts)
                    .as("a shared external Partner Payment contract must be an explicit future governance decision")
                    .isEmpty();
        }
    }
}
