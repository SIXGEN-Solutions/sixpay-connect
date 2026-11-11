package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.CustomerVerificationResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResolvedDebtorAccountReferenceFactoryTest {
    private final ResolvedDebtorAccountReferenceFactory factory =
            new ResolvedDebtorAccountReferenceFactory();

    @Test
    void usesTheCanonicalFingerprintAlreadyPersistedAtCustomerEnrollment() throws Exception {
        String institution = "REGIONAL";
        String customer = "0494223";
        String account = "04942231051";
        var resolved = factory.from(verified(institution, customer, account));
        String canonical = "SIXPAY-ACCOUNT-BINDING-v1\n"
                + institution.length() + ":" + institution
                + customer.length() + ":" + customer
                + account.length() + ":" + account;
        String expected = "v1:" + HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        assertEquals(expected, resolved.bindingFingerprint());
        assertNotEquals(legacyPipeFingerprint(institution, customer, account),
                resolved.bindingFingerprint());
    }

    @Test
    void bindingIsSpecificToInstitutionCustomerAndAccount() {
        String baseline = factory.from(verified("REGIONAL", "0494223", "04942231051"))
                .bindingFingerprint();
        assertNotEquals(baseline, factory.from(verified("OTHER", "0494223", "04942231051"))
                .bindingFingerprint());
        assertNotEquals(baseline, factory.from(verified("REGIONAL", "0494224", "04942231051"))
                .bindingFingerprint());
        assertNotEquals(baseline, factory.from(verified("REGIONAL", "0494223", "04942231052"))
                .bindingFingerprint());
    }

    @Test
    void refusesToResolveAccountWithoutVerifiedEvidence() {
        var verified = verified("REGIONAL", "0494223", "04942231051");
        var unknown = Arrays.stream(CustomerVerificationResponse.CheckType.values())
                .map(type -> new CustomerVerificationResponse.Check(
                        type, CustomerVerificationResponse.CheckResult.UNKNOWN,
                        "TECHNICAL_RESULT_UNKNOWN"))
                .toList();
        var indeterminate = new CustomerVerificationResponse(
                verified.verificationId(),
                CustomerVerificationResponse.Outcome.INDETERMINATE,
                unknown,
                verified.evidenceFingerprint(),
                verified.accountBindingFingerprint(),
                verified.customerReference(),
                verified.accountReference(),
                verified.accountFinancialInstitutionCode(),
                verified.maskedAccountIdentifier(),
                verified.observedAt(),
                verified.validUntil(),
                verified.completedAt());
        assertThrows(IllegalArgumentException.class, () -> factory.from(indeterminate));
    }

    private static CustomerVerificationResponse verified(
            String institution, String customer, String account) {
        var checks = Arrays.stream(CustomerVerificationResponse.CheckType.values())
                .map(type -> new CustomerVerificationResponse.Check(
                        type, CustomerVerificationResponse.CheckResult.PASS, null))
                .toList();
        Instant now = Instant.parse("2026-11-02T12:00:00Z");
        return new CustomerVerificationResponse(
                UUID.fromString("2389a527-a690-3dd5-8012-20460378c85b"),
                CustomerVerificationResponse.Outcome.VERIFIED,
                checks,
                "v1:sha256:" + "b".repeat(64),
                "v1:" + "a".repeat(64),
                customer,
                account,
                institution,
                "****1051",
                now,
                now.plusSeconds(300),
                now.plusSeconds(1));
    }

    private static String legacyPipeFingerprint(String institution, String customer,
                                                String account) throws Exception {
        var hash = MessageDigest.getInstance("SHA-256").digest(
                (institution + "|" + customer + "|" + account)
                        .getBytes(StandardCharsets.UTF_8));
        return "v1:" + HexFormat.of().formatHex(hash);
    }
}
