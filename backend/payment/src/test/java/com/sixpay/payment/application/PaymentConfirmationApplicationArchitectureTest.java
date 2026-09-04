package com.sixpay.payment.application;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentConfirmationApplicationArchitectureTest {

    @Test
    void publicInputPortsDoNotExposeConfirmationRevocation()
            throws Exception {

        Path inputPorts = Path.of(
                "src/main/java/com/sixpay/payment/application/port/input"
        );

        try (var paths = Files.list(inputPorts)) {
            var revocationPorts = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            String source = Files.readString(path);
                            return source.contains("revoke")
                                    || source.contains("Revocation");
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertThat(revocationPorts).isEmpty();
        }
    }

    private static final Path MAIN =
            Path.of("src/main/java/com/sixpay/payment");

    @Test
    void exposesExactlyTheFourRequiredPublicConfirmationUseCaseTypes()
            throws Exception {

        assertUseCaseExists("CreatePaymentConfirmationUseCase.java");
        assertUseCaseExists("ReadPaymentConfirmationUseCase.java");
        assertUseCaseExists("VerifyPaymentConfirmationUseCase.java");
        assertUseCaseExists("ResendPaymentConfirmationUseCase.java");

        assertFalse(Files.exists(
                MAIN.resolve(
                        "application/port/input/"
                                + "RevokePaymentConfirmationUseCase.java"
                )
        ));

        assertFalse(Files.exists(
                MAIN.resolve(
                        "application/port/input/"
                                + "RecoverPaymentConfirmationUseCase.java"
                )
        ));
    }

    @Test
    void bankingGatewayExposesExactlyTheSixApprovedOperationNames()
            throws Exception {

        String source = Files.readString(
                MAIN.resolve(
                        "application/port/output/banking/"
                                + "PaymentConfirmationGateway.java"
                )
        );

        assertTrue(source.contains(
                "PaymentConfirmationBankResult create(CreateRequest request)"
        ));
        assertTrue(source.contains(
                "PaymentConfirmationBankResult verify(VerifyRequest request)"
        ));
        assertTrue(source.contains(
                "PaymentConfirmationBankResult replace(ReplaceRequest request)"
        ));
        assertTrue(source.contains(
                "PaymentConfirmationBankResult lookup(LookupRequest request)"
        ));
        assertTrue(source.contains(
                "PaymentConfirmationBankResult recover(RecoveryRequest request)"
        ));
        assertTrue(source.contains(
                "PaymentConfirmationBankResult revoke(RevokeRequest request)"
        ));
    }

    @Test
    void createDerivesBankingContextFromExistingPayment()
            throws Exception {

        String gateway = Files.readString(
                MAIN.resolve(
                        "application/port/output/banking/"
                                + "PaymentConfirmationGateway.java"
                )
        );
        String service = Files.readString(
                MAIN.resolve(
                        "application/service/"
                                + "PaymentConfirmationService.java"
                )
        );
        String command = Files.readString(
                MAIN.resolve(
                        "application/command/"
                                + "CreatePaymentConfirmationCommand.java"
                )
        );

        assertTrue(gateway.contains("Payment payment"));
        assertTrue(service.contains(
                "findByPublicPaymentReference(paymentReference)"
        ));
        assertTrue(service.contains(
                "new PaymentConfirmationGateway.CreateRequest("
        ));

        assertFalse(command.contains("customerReference"));
        assertFalse(command.contains("debtorAccountReference"));
        assertFalse(command.contains("amount"));
    }

    @Test
    void verifyKeepsOtpTransientAndRedacted()
            throws Exception {

        String command = Files.readString(
                MAIN.resolve(
                        "application/command/"
                                + "VerifyPaymentConfirmationCommand.java"
                )
        );
        String gateway = Files.readString(
                MAIN.resolve(
                        "application/port/output/banking/"
                                + "PaymentConfirmationGateway.java"
                )
        );

        assertTrue(command.contains("char[] otp"));
        assertTrue(command.contains("otp=<redacted>"));
        assertTrue(gateway.contains("char[] otp"));
        assertTrue(gateway.contains("otp=<redacted>"));

        assertFalse(command.contains("String otp"));
        assertFalse(gateway.contains("String otp"));
    }

    @Test
    void publicViewDoesNotExposeInternalChallengeReference()
            throws Exception {

        String source = Files.readString(
                MAIN.resolve(
                        "application/view/"
                                + "PaymentConfirmationView.java"
                )
        );

        assertFalse(source.contains(
                "ConfirmationChallengeReference challengeReference"
        ));
    }

    @Test
    void lotOnePointThreeDoesNotBypassAtomicMutationInvariant()
            throws Exception {

        String source = Files.readString(
                MAIN.resolve(
                        "application/service/"
                                + "PaymentConfirmationService.java"
                )
        );

        assertFalse(source.contains("recordConfirmationChallenge("));
        assertFalse(source.contains("PaymentMutationCoordinator"));
        assertFalse(source.contains("PaymentAtomicPersistencePort"));
    }

    private static void assertUseCaseExists(String fileName) {
        assertTrue(Files.exists(
                MAIN.resolve("application/port/input/" + fileName)
        ));
    }
}
