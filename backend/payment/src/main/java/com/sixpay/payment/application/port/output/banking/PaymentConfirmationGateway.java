package com.sixpay.payment.application.port.output.banking;

import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Provider-neutral Core Banking boundary for Payment confirmation.
 *
 * <p>The six methods correspond exactly to the six approved Amplitude
 * operations: create, verify, replace, lookup by challenge, recovery by
 * original idempotency key and revoke.</p>
 */
public interface PaymentConfirmationGateway {

    PaymentConfirmationBankResult create(CreateRequest request);

    PaymentConfirmationBankResult verify(VerifyRequest request);

    PaymentConfirmationBankResult replace(ReplaceRequest request);

    PaymentConfirmationBankResult lookup(LookupRequest request);

    PaymentConfirmationBankResult recover(RecoveryRequest request);

    PaymentConfirmationBankResult revoke(RevokeRequest request);

    /**
     * Create deliberately receives the existing Payment aggregate rather than
     * an invented TRESOR PAY payload. Provider mapping is deferred to the
     * banking adapter.
     */
    record CreateRequest(
            Payment payment,
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey
    ) {
        public CreateRequest {
            payment = Objects.requireNonNull(payment, "Payment");
            context = Objects.requireNonNull(
                    context,
                    "Banking request context"
            );
            idempotencyKey = Objects.requireNonNull(
                    idempotencyKey,
                    "Banking idempotency key"
            );
        }
    }

    final class VerifyRequest {

        private final PaymentId paymentId;
        private final PublicPaymentReference paymentReference;
        private final ConfirmationChallengeReference challengeReference;
        private final BankingRequestContext context;
        private final BankingIdempotencyKey idempotencyKey;
        private final char[] otp;

        public VerifyRequest(
                PaymentId paymentId,
                PublicPaymentReference paymentReference,
                ConfirmationChallengeReference challengeReference,
                BankingRequestContext context,
                BankingIdempotencyKey idempotencyKey,
                char[] otp
        ) {
            this.paymentId = Objects.requireNonNull(
                    paymentId,
                    "Payment ID"
            );
            this.paymentReference = Objects.requireNonNull(
                    paymentReference,
                    "Payment reference"
            );
            this.challengeReference = Objects.requireNonNull(
                    challengeReference,
                    "Confirmation challenge reference"
            );
            this.context = Objects.requireNonNull(
                    context,
                    "Banking request context"
            );
            this.idempotencyKey = Objects.requireNonNull(
                    idempotencyKey,
                    "Banking idempotency key"
            );
            Objects.requireNonNull(otp, "OTP");
            if (otp.length == 0) {
                throw new IllegalArgumentException(
                        "OTP must not be empty"
                );
            }
            this.otp = Arrays.copyOf(otp, otp.length);
        }

        public PaymentId paymentId() {
            return paymentId;
        }

        public PublicPaymentReference paymentReference() {
            return paymentReference;
        }

        public ConfirmationChallengeReference challengeReference() {
            return challengeReference;
        }

        public BankingRequestContext context() {
            return context;
        }

        public BankingIdempotencyKey idempotencyKey() {
            return idempotencyKey;
        }

        public char[] otp() {
            return Arrays.copyOf(otp, otp.length);
        }

        @Override
        public String toString() {
            return "VerifyRequest[paymentId="
                    + paymentId
                    + ", paymentReference="
                    + paymentReference
                    + ", challengeReference="
                    + challengeReference
                    + ", otp=<redacted>]";
        }
    }

    record ReplaceRequest(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference,
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey
    ) {
        public ReplaceRequest {
            paymentId = Objects.requireNonNull(paymentId, "Payment ID");
            paymentReference = Objects.requireNonNull(
                    paymentReference,
                    "Payment reference"
            );
            challengeReference = Objects.requireNonNull(
                    challengeReference,
                    "Confirmation challenge reference"
            );
            context = Objects.requireNonNull(
                    context,
                    "Banking request context"
            );
            idempotencyKey = Objects.requireNonNull(
                    idempotencyKey,
                    "Banking idempotency key"
            );
        }
    }

    record LookupRequest(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference,
            BankingRequestContext context
    ) {
        public LookupRequest {
            paymentId = Objects.requireNonNull(paymentId, "Payment ID");
            paymentReference = Objects.requireNonNull(
                    paymentReference,
                    "Payment reference"
            );
            challengeReference = Objects.requireNonNull(
                    challengeReference,
                    "Confirmation challenge reference"
            );
            context = Objects.requireNonNull(
                    context,
                    "Banking request context"
            );
        }
    }

    record RecoveryRequest(
            BankingRequestContext context,
            BankingIdempotencyKey originalIdempotencyKey
    ) {
        public RecoveryRequest {
            context = Objects.requireNonNull(
                    context,
                    "Banking request context"
            );
            originalIdempotencyKey = Objects.requireNonNull(
                    originalIdempotencyKey,
                    "Original banking idempotency key"
            );
        }
    }

    record RevokeRequest(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference,
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey,
            String reasonCode
    ) {
        private static final Pattern REASON_CODE =
                Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");

        public RevokeRequest {
            paymentId = Objects.requireNonNull(paymentId, "Payment ID");
            paymentReference = Objects.requireNonNull(
                    paymentReference,
                    "Payment reference"
            );
            challengeReference = Objects.requireNonNull(
                    challengeReference,
                    "Confirmation challenge reference"
            );
            context = Objects.requireNonNull(
                    context,
                    "Banking request context"
            );
            idempotencyKey = Objects.requireNonNull(
                    idempotencyKey,
                    "Banking idempotency key"
            );
            if (reasonCode == null
                    || !REASON_CODE.matcher(reasonCode).matches()) {
                throw new IllegalArgumentException(
                        "Revocation reason code has an invalid format"
                );
            }
        }
    }
}
