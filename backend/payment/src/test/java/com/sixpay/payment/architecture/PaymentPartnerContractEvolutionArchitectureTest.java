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
    void canonicalPartnerPaymentContractsExistAfterExplicitGovernanceDecision()
            throws Exception {
        Path partnerContracts = CONTRACTS.resolve("partner");

        assertThat(partnerContracts)
                .as("PA-1 establishes the canonical Partner Payment contract root")
                .isDirectory();

        List<String> expectedContracts = List.of(
                "partner-payment-request-api-v1.yaml",
                "partner-payment-confirmation-api-v1.yaml",
                "partner-payment-status-query-api-v1.yaml",
                "partner-payment-callback-webhook-v1.yaml"
        );

        try (Stream<Path> paths = Files.list(partnerContracts)) {
            List<String> observed = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(expectedContracts::contains)
                    .sorted()
                    .toList();

            assertThat(observed)
                    .as("PA-1 approved canonical Partner Payment contracts")
                    .containsExactlyInAnyOrderElementsOf(expectedContracts);
        }
    }

}
