package com.sixpay.payment.domain.model;

import com.sixpay.common.context.CorrelationId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentIdentityValueObjectsTest {

    @Test
    void paymentIdAcceptsOnlyCanonicalNonNilUuid() {
        String canonical =
                "5ee1764d-3b5f-4dd6-a13b-718f0555be83";

        assertEquals(
                canonical,
                PaymentId.from(canonical).toString()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> PaymentId.from(
                        "00000000-0000-0000-0000-000000000000"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PaymentId.from(canonical.toUpperCase())
        );
        assertThrows(
                NullPointerException.class,
                () -> new PaymentId(null)
        );
    }

    @Test
    void externalReferencesStripOnlyOuterAsciiWhitespaceAndPreserveCase() {
        assertEquals(
                "Pay/Ref:ABC-01",
                ExternalPaymentReference.of(
                        " \tPay/Ref:ABC-01\r\n"
                ).value()
        );
        assertEquals(
                "Subscription.Ref-01",
                ExternalSubscriptionReference.of(
                        " Subscription.Ref-01 "
                ).value()
        );

        assertNotEquals(
                ExternalPaymentReference.of("Payment-01"),
                ExternalPaymentReference.of("payment-01")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ExternalPaymentReference.of("Payment Ref")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ExternalSubscriptionReference.of("-invalid")
        );
    }

    @Test
    void publicReferenceRequiresPayPrefixedCrockfordUlid() {
        String reference =
                "PAY-01J8YH6M6VT8EF3Z7Q4N9P2KDC";

        assertEquals(
                reference,
                PublicPaymentReference.of(reference).value()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> PublicPaymentReference.of(
                        "PAY-01J8YH6M6VT8EF3Z7Q4N9P2KDI"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PublicPaymentReference.of(
                        reference.toLowerCase()
                )
        );
    }

    @Test
    void requestIdentityKeepsThreeDistinctIdentities() {
        IdempotencyKey idempotencyKey =
                IdempotencyKey.of("Payment-Request-0001");
        RequestFingerprint fingerprint =
                RequestFingerprint.of("a".repeat(64));
        CorrelationId correlationId = CorrelationId.of(
                "40a11cb8-b32c-474e-bab2-e0b6f43138c8"
        );

        PaymentRequestIdentity identity =
                new PaymentRequestIdentity(
                        idempotencyKey,
                        fingerprint,
                        correlationId
                );

        assertEquals(
                idempotencyKey,
                identity.idempotencyKey()
        );
        assertEquals(
                fingerprint,
                identity.requestFingerprint()
        );
        assertEquals(
                correlationId,
                identity.correlationId()
        );
        assertTrue(
                identity.toString().contains(
                        correlationId.value()
                )
        );
        assertTrue(
                !identity.toString().contains(
                        fingerprint.value()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentRequestIdentity(
                        idempotencyKey,
                        fingerprint,
                        CorrelationId.of("not-a-uuid")
                )
        );
    }

    @Test
    void idempotencyAndFingerprintFormatsAreStrict() {
        assertEquals(
                "Abc:def-01",
                IdempotencyKey.of(" Abc:def-01 ").value()
        );
        assertEquals(
                "0".repeat(64),
                RequestFingerprint.of("0".repeat(64)).value()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> IdempotencyKey.of("short")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RequestFingerprint.of("A".repeat(64))
        );
    }

    @Test
    void financialInstitutionCodeUsesLocaleIndependentUppercase() {
        assertEquals(
                "BANK_CM-01",
                FinancialInstitutionCode.of(
                        " bank_cm-01 "
                ).value()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> FinancialInstitutionCode.of("B")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> FinancialInstitutionCode.of("BANK.CM")
        );
    }

    @Test
    void paymentSourceIsClosedForTheMvp() {
        assertEquals(
                1,
                PaymentSource.values().length
        );
        assertEquals(
                PaymentSource.TRESOR_PAY,
                PaymentSource.valueOf("TRESOR_PAY")
        );
    }
}
