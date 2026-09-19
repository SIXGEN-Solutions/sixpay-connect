package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentFunctionalClassificationArchitectureTest {
    private static final Path PAYMENT = Path.of("src/main/java/com/sixpay/payment");

    @Test
    void treasuryConceptsRemainExplicitlyTreasurySpecific() throws Exception {
        String treasuryContext = Files.readString(PAYMENT.resolve("domain/model/TreasuryPaymentContext.java"));
        String beneficiary = Files.readString(PAYMENT.resolve("application/command/PaymentBeneficiaryCommand.java"));
        String allocation = Files.readString(PAYMENT.resolve("domain/model/TreasuryAllocationIntent.java"));
        assertThat(treasuryContext).contains("ClaimType claimType", "String taxpayerIdentifier", "not a universal Payment invariant");
        assertThat(beneficiary).contains("Treasury beneficiary allocation", "not a universal Payment beneficiary abstraction");
        assertThat(allocation).contains("Treasury allocation");
    }

    @Test
    void providerAndPartnerMetadataAreNotPromotedToPaymentInvariants() throws Exception {
        String subscription = Files.readString(PAYMENT.resolve("domain/model/ExternalSubscriptionReference.java"));
        String command = Files.readString(PAYMENT.resolve("application/command/InitiatePaymentCommand.java"));
        assertThat(subscription).contains("opaque provider trace metadata", "not a Payment invariant", "CustomerSubscription");
        assertThat(command)
                .contains("ExternalSubscriptionReference externalSubscriptionReference")
                .contains("String applicationId")
                .contains("opaque provider trace metadata")
                .contains("application metadata")
                .contains("None of those five values is promoted")
                .contains("Payment invariant");
    }

    @Test
    void classificationPreservesPersistedStateSchema() throws Exception {
        String state = Files.readString(PAYMENT.resolve("infrastructure/persistence/PaymentStateDocument.java"));
        String context = Files.readString(PAYMENT.resolve("domain/model/PaymentInitiationContext.java"));
        assertThat(state).contains("CURRENT_SCHEMA_VERSION = 8");
        assertThat(context).contains("TreasuryPaymentContext treasuryPaymentContext()", "ClaimType claimType", "String taxpayerIdentifier");
    }
}
