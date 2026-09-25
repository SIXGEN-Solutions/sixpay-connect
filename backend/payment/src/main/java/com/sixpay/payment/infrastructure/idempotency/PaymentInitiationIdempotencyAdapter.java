package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.idempotency
        .PaymentInitiationIdempotencyPort;
import com.sixpay.payment.domain.model.ExternalPaymentReference;
import com.sixpay.payment.application.service
        .PaymentInitiationInProgressException;
import com.sixpay.payment.application.view.InitiateDebitResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * PostgreSQL-backed idempotent execution adapter for InitiateDebit.
 *
 * <p>The transaction covers the idempotency record and the complete new-request
 * action. Consequently, a successful commit contains both the replayable
 * response and the Payment/audit/outbox writes; a failure rolls them back
 * together. A PostgreSQL advisory lock serializes concurrent calls for the
 * same operation and key.</p>
 */
@Component
@ConditionalOnBean({
        PaymentIdempotencyConcurrencyCoordinator.class,
        PaymentIdempotencyReplayStore.class
})
public class PaymentInitiationIdempotencyAdapter
        implements PaymentInitiationIdempotencyPort {

    static final String OPERATION =
            "PAYMENT_INITIATE_DEBIT";

    private final PaymentInitiationCanonicalizer canonicalizer;
    private final PaymentIdempotencyHasher hasher;
    private final PaymentIdempotencyConcurrencyCoordinator coordinator;
    private final PaymentIdempotencyReplayStore replayStore;
    private final PaymentInitiationReplayCodec replayCodec;
    private final PaymentLookupPort paymentLookupPort;
    private final TimeProvider timeProvider;

    public PaymentInitiationIdempotencyAdapter(
            PaymentInitiationCanonicalizer canonicalizer,
            PaymentIdempotencyHasher hasher,
            PaymentIdempotencyConcurrencyCoordinator coordinator,
            PaymentIdempotencyReplayStore replayStore,
            PaymentInitiationReplayCodec replayCodec,
            PaymentLookupPort paymentLookupPort,
            TimeProvider timeProvider
    ) {
        this.canonicalizer = Objects.requireNonNull(
                canonicalizer,
                "Payment initiation canonicalizer"
        );

        this.hasher = Objects.requireNonNull(
                hasher,
                "Payment idempotency hasher"
        );

        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Payment idempotency concurrency coordinator"
        );

        this.replayStore = Objects.requireNonNull(
                replayStore,
                "Payment idempotency replay store"
        );

        this.replayCodec = Objects.requireNonNull(
                replayCodec,
                "Payment initiation replay codec"
        );

        this.paymentLookupPort = Objects.requireNonNull(
                paymentLookupPort,
                "Payment lookup port"
        );

        this.timeProvider = Objects.requireNonNull(
                timeProvider,
                "Time provider"
        );
    }

    /**
     * Fingerprints canonical business content, serializes competing requests
     * for the key, then either replays a completed response, rejects a
     * still-running request, or executes and stores a new response.
     *
     * <p>An {@code OUTCOME_UNKNOWN} decision is not valid for the local
     * PAYMENT_INITIATE_DEBIT operation. That state is reserved for external
     * operations requiring authoritative recovery.</p>
     */
    @Override
    @Transactional
    public InitiateDebitResult execute(
            InitiateDebitCommand command,
            Function<String, InitiateDebitResult> newRequest
    ) {
        Objects.requireNonNull(
                command,
                "InitiateDebit command"
        );

        Objects.requireNonNull(
                newRequest,
                "New-request action"
        );

        String requestHash = hasher.hash(
                canonicalizer.canonicalize(command)
        );
        String partnerIdentifier = command.applicationId();

        Optional<InitiateDebitResult> externalReferenceReplay =
                replayByExternalReference(
                        command,
                        partnerIdentifier,
                        requestHash
                );
        if (externalReferenceReplay.isPresent()) {
            return externalReferenceReplay.orElseThrow();
        }

        return coordinator.executeScopedLocked(
                partnerIdentifier,
                OPERATION,
                command.idempotencyKey(),
                () -> executeLocked(
                        command,
                        partnerIdentifier,
                        requestHash,
                        newRequest
                )
        );
    }

    private InitiateDebitResult executeLocked(
            InitiateDebitCommand command,
            String partnerIdentifier,
            String requestHash,
            Function<String, InitiateDebitResult> newRequest
    ) {
        PaymentIdempotencyDecision decision =
                replayStore.beginScoped(
                        partnerIdentifier,
                        OPERATION,
                        command.idempotencyKey(),
                        requestHash,
                        timeProvider.now()
                );

        return switch (decision.kind()) {
            case REPLAY ->
                    replay(decision);

            case IN_PROGRESS ->
                    throw new PaymentInitiationInProgressException();

            case OUTCOME_UNKNOWN ->
                    throw new IllegalStateException(
                            "PAYMENT_INITIATE_DEBIT cannot have an unknown external outcome"
                    );

            case NEW ->
                    completeNew(
                            command,
                            partnerIdentifier,
                            requestHash,
                            newRequest
                    );
        };
    }

    private InitiateDebitResult completeNew(
            InitiateDebitCommand command,
            String partnerIdentifier,
            String requestHash,
            Function<String, InitiateDebitResult> newRequest
    ) {
        InitiateDebitResult result =
                newRequest.apply(requestHash);

        replayStore.completeScoped(
                partnerIdentifier,
                OPERATION,
                command.idempotencyKey(),
                requestHash,
                result.paymentId().value(),
                result.status().name(),
                replayCodec.encode(result),
                timeProvider.now()
        );

        return result;
    }

    private Optional<InitiateDebitResult> replayByExternalReference(
            InitiateDebitCommand command,
            String partnerIdentifier,
            String requestHash
    ) {
        return paymentLookupPort
                .findByPartnerIdentifierAndExternalPaymentReference(
                        partnerIdentifier,
                        ExternalPaymentReference.of(command.endToEndId())
                )
                .map(payment -> {
                    String persistedHash = payment.toState()
                            .requestIdentity()
                            .requestFingerprint()
                            .value();

                    if (!persistedHash.equals(requestHash)) {
                        throw new PaymentExternalReferenceConflictException(
                                partnerIdentifier,
                                command.endToEndId()
                        );
                    }

                    PaymentIdempotencyDecision decision =
                            replayStore
                                    .findCompletedReplayForPayment(
                                            partnerIdentifier,
                                            OPERATION,
                                            payment.id().value()
                                    )
                                    .orElseThrow(() ->
                                            new IllegalStateException(
                                                    "Existing Payment has no completed initiation replay"
                                            )
                                    );

                    return replay(decision);
                });
    }

    /**
     * Decodes the stored response and verifies that its Payment identifier
     * agrees with the independently stored idempotency metadata before it is
     * returned to the caller.
     */
    private InitiateDebitResult replay(
            PaymentIdempotencyDecision decision
    ) {
        InitiateDebitResult result =
                replayCodec.decode(
                        decision.responsePayload()
                );

        if (!result.paymentId()
                .value()
                .equals(decision.paymentId())) {

            throw new IllegalStateException(
                    "Idempotency replay Payment ID mismatch"
            );
        }

        return result;
    }
}
