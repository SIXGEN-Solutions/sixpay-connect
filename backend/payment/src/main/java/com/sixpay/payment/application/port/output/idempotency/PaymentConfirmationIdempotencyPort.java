package com.sixpay.payment.application.port.output.idempotency;

import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.function.Supplier;

public interface PaymentConfirmationIdempotencyPort {

    PaymentConfirmationBankResult executeCreate(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            IdempotencyKey idempotencyKey,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    );

    PaymentConfirmationBankResult executeVerify(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            IdempotencyKey idempotencyKey,
            char[] otp,
            Supplier<PaymentConfirmationBankResult> newRequest
    );

    PaymentConfirmationBankResult executeReplace(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference,
            IdempotencyKey idempotencyKey,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    );
}
