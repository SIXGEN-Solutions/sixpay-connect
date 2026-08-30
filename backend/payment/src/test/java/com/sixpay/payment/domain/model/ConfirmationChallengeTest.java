package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmationChallengeTest {

    @Test
    void exposesOnlyTheSixApprovedChallengeStatuses() {
        assertEquals(
                List.of(
                        "ACTIVE",
                        "VERIFIED",
                        "EXPIRED",
                        "LOCKED",
                        "REPLACED",
                        "REVOKED"
                ),
                Arrays.stream(ConfirmationChallengeStatus.values())
                        .map(Enum::name)
                        .toList()
        );
    }

    @Test
    void challengeModelContainsNoOtpField() {
        List<String> fields =
                Arrays.stream(ConfirmationChallenge.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .map(String::toLowerCase)
                        .toList();

        assertTrue(
                fields.stream().noneMatch(name -> name.contains("otp")),
                "ConfirmationChallenge must never persist OTP material"
        );
    }

    @Test
    void bindingRequiresTheApprovedPaymentContext() {
        ConfirmationChallengeBinding binding =
                new ConfirmationChallengeBinding(
                        PublicPaymentReference.of(
                                "PAY-01J00000000000000000000000"
                        ),
                        "CUSTOMER-0001",
                        "ACCOUNT-0001",
                        Money.of(new BigDecimal("1000"), "XAF")
                );

        assertEquals(
                "CUSTOMER-0001",
                binding.customerReference()
        );
        assertEquals(
                "ACCOUNT-0001",
                binding.debtorAccountReference()
        );
        assertEquals(
                Money.of(new BigDecimal("1000"), "XAF"),
                binding.amount()
        );
    }

    @Test
    void bindingRejectsNonPositiveAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ConfirmationChallengeBinding(
                        PublicPaymentReference.of(
                                "PAY-01J00000000000000000000000"
                        ),
                        "CUSTOMER-0001",
                        "ACCOUNT-0001",
                        Money.zero(java.util.Currency.getInstance("XAF"))
                )
        );
    }

    @Test
    void challengeMayRepresentAuthoritativeExpiryWithoutOtpMaterial() {
        ConfirmationChallengeBinding binding =
                new ConfirmationChallengeBinding(
                        PublicPaymentReference.of(
                                "PAY-01J00000000000000000000000"
                        ),
                        "CUSTOMER-0001",
                        "ACCOUNT-0001",
                        Money.of(new BigDecimal("1000"), "XAF")
                );

        ConfirmationChallenge challenge =
                new ConfirmationChallenge(
                        new ConfirmationChallengeReference(
                                "challenge-opaque-001"
                        ),
                        binding,
                        ConfirmationChallengeStatus.EXPIRED,
                        ConfirmationBusinessCode.CHALLENGE_EXPIRED,
                        ConfirmationDeliveryChannel.SMS,
                        null,
                        null,
                        null
                );

        assertEquals(
                ConfirmationChallengeStatus.EXPIRED,
                challenge.status()
        );
        assertFalse(challenge.active());
    }
}
