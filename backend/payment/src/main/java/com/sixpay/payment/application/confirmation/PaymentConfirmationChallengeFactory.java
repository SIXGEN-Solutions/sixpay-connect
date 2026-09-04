package com.sixpay.payment.application.confirmation;

import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.domain.model.ConfirmationChallenge;
import com.sixpay.payment.domain.model.ConfirmationChallengeBinding;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;

import java.time.Instant;

/**
 * Builds Payment-native confirmation challenges from verified banking
 * evidence and Payment Confirmation provider results.
 */
public final class PaymentConfirmationChallengeFactory {

    private PaymentConfirmationChallengeFactory() {
    }

    public static ConfirmationChallengeBinding requireBinding(Payment payment) {
        BankingVerificationSnapshot verification =
                payment.toState()
                        .bankingVerificationEvidence()
                        .orElseThrow(() -> new IllegalStateException(
                                "Payment banking verification evidence "
                                        + "is required before confirmation"
                        ));

        String customerReference =
                verification.customerReferenceOptional()
                        .orElseThrow(() -> new IllegalStateException(
                                "Verified banking customerReference "
                                        + "is required before confirmation"
                        ));
        String accountReference =
                verification.accountReferenceOptional()
                        .orElseThrow(() -> new IllegalStateException(
                                "Verified banking accountReference "
                                        + "is required before confirmation"
                        ));

        return new ConfirmationChallengeBinding(
                payment.publicPaymentReference(),
                customerReference,
                accountReference,
                payment.toState().requestedAmount()
        );
    }

    public static ConfirmationChallenge fromBankResult(
            PaymentConfirmationBankResult result,
            ConfirmationChallengeBinding binding
    ) {
        return new ConfirmationChallenge(
                result.challengeReference(),
                binding,
                result.status(),
                result.businessCode(),
                result.deliveryChannel(),
                result.sentAt(),
                result.expiresAt(),
                result.optionalVerifiedAt().orElse(null)
        );
    }

    public static Instant observationInstant(
            Payment payment,
            PaymentConfirmationBankResult result
    ) {
        Instant observedAt = result.sentAt();
        if (observedAt.isBefore(payment.toState().updatedAt())) {
            return payment.toState().updatedAt();
        }
        return observedAt;
    }
}
