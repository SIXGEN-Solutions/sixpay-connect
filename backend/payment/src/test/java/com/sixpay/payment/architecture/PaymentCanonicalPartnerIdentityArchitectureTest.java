package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentCanonicalPartnerIdentityArchitectureTest {
    private static final Path PAYMENT = Path.of("src/main/java/com/sixpay/payment");

    @Test
    void genericPaymentUsesCanonicalPartnerIdentityNotProviderLogin() throws Exception {
        String command = Files.readString(PAYMENT.resolve("application/command/InitiatePaymentCommand.java"));
        String context = Files.readString(PAYMENT.resolve("domain/model/PaymentInitiationContext.java"));
        String preparation = Files.readString(PAYMENT.resolve("infrastructure/initiation/PaymentInitiationPreparationAdapter.java"));
        String canonicalizer = Files.readString(PAYMENT.resolve("infrastructure/idempotency/PaymentInitiationCanonicalizer.java"));

        assertThat(command).contains("CanonicalPartnerIdentity partnerIdentity")
                .doesNotContain("partnerLoginName", "authenticatedPartnerLoginName");
        assertThat(context)
                .contains("CanonicalPartnerIdentity partnerIdentity")
                .doesNotContain(
                        "partnerLoginName",
                        "com.sixpay.partner."
                );
        assertThat(preparation).contains("command.partnerIdentity()").doesNotContain("command.partnerLoginName()");
        assertThat(canonicalizer).contains("command.partnerIdentity().toString()")
                .doesNotContain("command.partnerLoginName()", "command.authenticatedPartnerLoginName()");
    }

    @Test
    void tresorPayBoundaryOwnsProviderLoginValidationAndSubjectResolution() throws Exception {
        Path boundary = PAYMENT.resolve("api/partner");
        String mapper = Files.readString(boundary.resolve("PartnerPaymentApiMapper.java"));
        String controller = Files.readString(boundary.resolve("PartnerPaymentCommandController.java"));
        assertThat(mapper).contains("request.loginName()", "authenticatedPartnerLoginName", "CanonicalPartnerIdentity.from(authenticatedPartnerSubject)");
        assertThat(controller).contains("authenticatedPartner.username()", "authenticatedPartner.subject()");
    }

    @Test
    void paymentHasNoCompileTimeDependencyOnPartnerModule() throws Exception {
        String all = Files.walk(PAYMENT).filter(x -> x.toString().endsWith(".java"))
                .map(x -> { try { return Files.readString(x); } catch (Exception e) { throw new IllegalStateException(e); } })
                .reduce("", (a, b) -> a + "\n" + b);
        assertThat(all).doesNotContain("com.sixpay.partner.");
        assertThat(Files.readString(Path.of("pom.xml")))
                .doesNotContain("<artifactId>partner</artifactId>");
    }
}
