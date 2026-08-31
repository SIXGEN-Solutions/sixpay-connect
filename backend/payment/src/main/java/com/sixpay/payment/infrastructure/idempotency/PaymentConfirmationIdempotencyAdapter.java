package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyPort;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyResult;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Component
@ConditionalOnBean(PaymentOtpIdempotencyFingerprintSet.class)
public class PaymentConfirmationIdempotencyAdapter
        implements PaymentConfirmationIdempotencyPort {

    static final String CREATE_OPERATION =
            "PAYMENT_CONFIRMATION_CREATE";
    static final String VERIFY_OPERATION =
            "PAYMENT_CONFIRMATION_VERIFY";
    static final String REPLACE_OPERATION =
            "PAYMENT_CONFIRMATION_REPLACE";

    private final PaymentConfirmationCanonicalizer canonicalizer;
    private final PaymentIdempotencyHasher hasher;
    private final PaymentOtpIdempotencyFingerprintSet otpFingerprints;
    private final PaymentConfirmationIdempotencyTransactions transactions;
    private final PaymentConfirmationReplayCodec replayCodec;
    private final TimeProvider timeProvider;

    public PaymentConfirmationIdempotencyAdapter(
            PaymentConfirmationCanonicalizer canonicalizer,
            PaymentIdempotencyHasher hasher,
            PaymentOtpIdempotencyFingerprintSet otpFingerprints,
            PaymentConfirmationIdempotencyTransactions transactions,
            PaymentConfirmationReplayCodec replayCodec,
            TimeProvider timeProvider
    ) {
        this.canonicalizer = Objects.requireNonNull(canonicalizer);
        this.hasher = Objects.requireNonNull(hasher);
        this.otpFingerprints = Objects.requireNonNull(otpFingerprints);
        this.transactions = Objects.requireNonNull(transactions);
        this.replayCodec = Objects.requireNonNull(replayCodec);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    @Override
    public PaymentConfirmationIdempotencyResult executeCreate(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            IdempotencyKey idempotencyKey,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    ) {
        String requestHash = hasher.hash(
                canonicalizer.create(paymentId, paymentReference)
        );

        return executeRecoverable(
                CREATE_OPERATION,
                paymentId,
                idempotencyKey,
                requestHash,
                List.of(requestHash),
                newRequest,
                recovery
        );
    }

    @Override
    public PaymentConfirmationIdempotencyResult executeVerify(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            IdempotencyKey idempotencyKey,
            char[] otp,
            Supplier<PaymentConfirmationBankResult> newRequest
    ) {
        List<String> candidates =
                otpFingerprints.candidates(
                        paymentReference.value(),
                        otp
                );

        PaymentConfirmationIdempotencyTransactions.BeginResult begin =
                transactions.begin(
                        VERIFY_OPERATION,
                        idempotencyKey.value(),
                        candidates.get(0),
                        candidates,
                        timeProvider.now()
                );

        return switch (begin.decision().kind()) {
            case REPLAY ->
                    PaymentConfirmationIdempotencyResult.replayed(
                            replay(paymentId, begin.decision())
                    );

            case IN_PROGRESS ->
                    throw new IllegalStateException(
                            "Payment confirmation verification is already in progress"
                    );

            case OUTCOME_UNKNOWN ->
                    throw new IllegalStateException(
                            "Verify idempotency must not enter OUTCOME_UNKNOWN"
                    );

            case NEW ->
                    PaymentConfirmationIdempotencyResult.executed(
                            completeNew(
                                    VERIFY_OPERATION,
                                    paymentId,
                                    idempotencyKey.value(),
                                    begin.requestHash(),
                                    newRequest
                            )
                    );
        };
    }

    @Override
    public PaymentConfirmationIdempotencyResult executeReplace(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference,
            IdempotencyKey idempotencyKey,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    ) {
        String requestHash = hasher.hash(
                canonicalizer.replace(
                        paymentId,
                        paymentReference,
                        challengeReference
                )
        );

        return executeRecoverable(
                REPLACE_OPERATION,
                paymentId,
                idempotencyKey,
                requestHash,
                List.of(requestHash),
                newRequest,
                recovery
        );
    }

    private PaymentConfirmationIdempotencyResult executeRecoverable(
            String operation,
            PaymentId paymentId,
            IdempotencyKey idempotencyKey,
            String preferredHash,
            List<String> acceptedHashes,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    ) {
        PaymentConfirmationIdempotencyTransactions.BeginResult begin =
                transactions.begin(
                        operation,
                        idempotencyKey.value(),
                        preferredHash,
                        acceptedHashes,
                        timeProvider.now()
                );

        return switch (begin.decision().kind()) {
            case REPLAY ->
                    PaymentConfirmationIdempotencyResult.replayed(
                            replay(paymentId, begin.decision())
                    );

            case IN_PROGRESS ->
                    throw new IllegalStateException(
                            "Payment confirmation operation is already in progress"
                    );

            case OUTCOME_UNKNOWN ->
                    PaymentConfirmationIdempotencyResult.executed(
                            recover(
                                    operation,
                                    paymentId,
                                    idempotencyKey.value(),
                                    begin.requestHash(),
                                    recovery
                            )
                    );

            case NEW ->
                    PaymentConfirmationIdempotencyResult.executed(
                            completeNewRecoverable(
                                    operation,
                                    paymentId,
                                    idempotencyKey.value(),
                                    begin.requestHash(),
                                    newRequest,
                                    recovery
                            )
                    );
        };
    }

    private PaymentConfirmationBankResult completeNew(
            String operation,
            PaymentId paymentId,
            String idempotencyKey,
            String requestHash,
            Supplier<PaymentConfirmationBankResult> newRequest
    ) {
        try {
            PaymentConfirmationBankResult result =
                    Objects.requireNonNull(
                            newRequest.get(),
                            "Payment confirmation bank result"
                    );
            complete(
                    operation,
                    paymentId,
                    idempotencyKey,
                    requestHash,
                    result
            );
            return result;
        } catch (RuntimeException exception) {
            fail(
                    operation,
                    idempotencyKey,
                    requestHash,
                    exception
            );
            throw exception;
        }
    }

    private PaymentConfirmationBankResult completeNewRecoverable(
            String operation,
            PaymentId paymentId,
            String idempotencyKey,
            String requestHash,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    ) {
        try {
            PaymentConfirmationBankResult result =
                    Objects.requireNonNull(
                            newRequest.get(),
                            "Payment confirmation bank result"
                    );
            complete(
                    operation,
                    paymentId,
                    idempotencyKey,
                    requestHash,
                    result
            );
            return result;
        } catch (PaymentConfirmationGateway.OutcomeUnknownException unknown) {
            transactions.markOutcomeUnknown(
                    operation,
                    idempotencyKey,
                    requestHash,
                    paymentId.value(),
                    unknown.recoveryReference(),
                    unknown.getMessage(),
                    timeProvider.now()
            );
            return recover(
                    operation,
                    paymentId,
                    idempotencyKey,
                    requestHash,
                    recovery
            );
        } catch (RuntimeException exception) {
            fail(
                    operation,
                    idempotencyKey,
                    requestHash,
                    exception
            );
            throw exception;
        }
    }

    private PaymentConfirmationBankResult recover(
            String operation,
            PaymentId paymentId,
            String idempotencyKey,
            String requestHash,
            Supplier<PaymentConfirmationBankResult> recovery
    ) {
        PaymentConfirmationBankResult result =
                Objects.requireNonNull(
                        recovery.get(),
                        "Payment confirmation recovery result"
                );

        complete(
                operation,
                paymentId,
                idempotencyKey,
                requestHash,
                result
        );
        return result;
    }

    private void complete(
            String operation,
            PaymentId paymentId,
            String idempotencyKey,
            String requestHash,
            PaymentConfirmationBankResult result
    ) {
        transactions.complete(
                operation,
                idempotencyKey,
                requestHash,
                paymentId.value(),
                result.status().name(),
                replayCodec.encode(result),
                timeProvider.now()
        );
    }

    private void fail(
            String operation,
            String idempotencyKey,
            String requestHash,
            RuntimeException original
    ) {
        try {
            transactions.fail(
                    operation,
                    idempotencyKey,
                    requestHash,
                    original.getMessage(),
                    timeProvider.now()
            );
        } catch (RuntimeException persistenceFailure) {
            original.addSuppressed(persistenceFailure);
        }
    }

    private PaymentConfirmationBankResult replay(
            PaymentId paymentId,
            PaymentIdempotencyDecision decision
    ) {
        if (!paymentId.value().equals(decision.paymentId())) {
            throw new IllegalStateException(
                    "Payment confirmation replay Payment ID mismatch"
            );
        }

        PaymentConfirmationBankResult result =
                replayCodec.decode(
                        decision.responsePayload()
                );

        if (!result.status()
                .name()
                .equals(decision.responseStatus())) {
            throw new IllegalStateException(
                    "Payment confirmation replay status mismatch"
            );
        }

        return result;
    }
}
