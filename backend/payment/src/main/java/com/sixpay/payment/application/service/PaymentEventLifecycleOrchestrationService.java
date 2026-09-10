package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.banking.BankingIdempotencyKey;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.PaymentEventExecutionPort;
import com.sixpay.payment.application.port.output.banking.PaymentEventRecoveryPort;
import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.PaymentEventObservationSource;
import com.sixpay.payment.domain.model.evidence.PaymentEventOutcome;
import com.sixpay.payment.domain.model.financial.FinancialSnapshotStatus;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import com.sixpay.payment.domain.repository.PaymentFinancialSnapshotRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * LOT 2.8.2 orchestration for the sole atomic T0 execution path.
 *
 * <p>The service consumes a finalized Payment financial snapshot and an already
 * Payment event context resolved by SIXPAY from Administration parameters
 * and Core Banking allocated context.
 * or retries the financial POST after an uncertain outcome.</p>
 */
@Service
@ConditionalOnBean({
        PaymentEventExecutionPort.class,
        PaymentEventRecoveryPort.class
})
public class PaymentEventLifecycleOrchestrationService {

    private final PaymentLookupPort paymentLookupPort;
    private final PaymentFinancialSnapshotRepository snapshotRepository;
    private final PaymentPostingPreparationService postingPreparationService;
    private final PaymentFinalizationService finalizationService;
    private final PaymentEventContextService contextService;
    private final PaymentEventExecutionPort executionPort;
    private final PaymentEventRecoveryPort recoveryPort;
    private final PaymentEventOutcomeMapper outcomeMapper =
            new PaymentEventOutcomeMapper();

    public PaymentEventLifecycleOrchestrationService(
            PaymentLookupPort paymentLookupPort,
            PaymentFinancialSnapshotRepository snapshotRepository,
            PaymentPostingPreparationService postingPreparationService,
            PaymentFinalizationService finalizationService,
            PaymentEventContextService contextService,
            PaymentEventExecutionPort executionPort,
            PaymentEventRecoveryPort recoveryPort
    ) {
        this.paymentLookupPort = Objects.requireNonNull(paymentLookupPort);
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
        this.postingPreparationService =
                Objects.requireNonNull(postingPreparationService);
        this.finalizationService =
                Objects.requireNonNull(finalizationService);
        this.contextService = Objects.requireNonNull(contextService);
        this.executionPort = Objects.requireNonNull(executionPort);
        this.recoveryPort = Objects.requireNonNull(recoveryPort);
    }

    public PaymentWorkflowResult execute(
            PaymentId paymentId,
            PostingInstructionIdentity instruction,
            BankingRequestContext bankingContext,
            Instant requestedAt,
            PaymentPolicyBundle policies
    ) {
        Objects.requireNonNull(paymentId, "Payment ID");
        Objects.requireNonNull(instruction, "Posting instruction");
        Objects.requireNonNull(bankingContext, "Banking request context");
        Objects.requireNonNull(requestedAt, "Requested at");
        Objects.requireNonNull(policies, "Payment policies");

        Payment payment = requirePayment(paymentId);
        PaymentFinancialEventSnapshot snapshot =
                requireFinalizedSnapshot(paymentId);

        requireInstructionMatchesSnapshot(
                instruction,
                snapshot
        );

        if (payment.status() == PaymentStatus.POSTING_PENDING) {
            PostingInstructionIdentity persistedInstruction =
                    payment.toState()
                            .postingInstruction()
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "POSTING_PENDING Payment requires "
                                                    + "a persisted posting instruction"
                                    )
                            );

            if (!persistedInstruction.equals(instruction)) {
                throw PaymentDomainException.conflict(
                        "A different posting instruction is already authorized"
                );
            }

            return PaymentWorkflowResult.from(
                    payment,
                    false
            );
        }

        if (payment.status() != PaymentStatus.APPROVED_FOR_POSTING) {
            throw PaymentDomainException.invalidTransition(
                    payment.status(),
                    "executePaymentEvent"
            );
        }

        PaymentWorkflowResult authorization =
                postingPreparationService.authorizePaymentEventPosting(
                        paymentId,
                        instruction,
                        requestedAt
                );

        if (!authorization.stateChanged()) {
            return authorization;
        }

        PaymentEventExecutionPort.PaymentEventMappingContext mappingContext =
                contextService.resolve(
                        bankingContext,
                        requestedAt
                );

        PaymentEventExecutionPort.PaymentEventExecutionResult providerResult;
        try {
            providerResult = executionPort.execute(
                    new PaymentEventExecutionPort
                            .PaymentEventExecutionCommand(
                            snapshot,
                            mappingContext,
                            bankingContext,
                            new BankingIdempotencyKey(
                                    instruction
                                            .idempotencyKey()
                                            .value()
                            )
                    )
            );
        } catch (PaymentEventExecutionPort.PaymentEventOutcomeUnknownException unknown) {
            return recordUnknown(
                    paymentId,
                    instruction,
                    mappingContext,
                    policies
            );
        }

        return finalizationService.recordPaymentEventOutcome(
                paymentId,
                outcomeMapper.toSnapshot(
                        instruction,
                        providerResult,
                        mappingContext.accountingDate(),
                        PaymentEventObservationSource.DIRECT_RESPONSE
                ),
                failureFor(providerResult, FailureStage.POSTING),
                providerResult.observedAt(),
                policies
        );
    }

    private static void requireInstructionMatchesSnapshot(
            PostingInstructionIdentity instruction,
            PaymentFinancialEventSnapshot snapshot
    ) {
        if (!instruction.amount().equals(
                snapshot.requestedAmount()
        )) {
            throw PaymentDomainException.conflict(
                    "Posting instruction amount conflicts "
                            + "with finalized financial snapshot"
            );
        }

        if (!instruction.instructionFingerprint().equals(
                PaymentT0FinancialRequestFingerprint.from(snapshot)
        )) {
            throw PaymentDomainException.conflict(
                    "Posting instruction conflicts "
                            + "with finalized financial snapshot"
            );
        }
    }

    public PaymentWorkflowResult recover(
            PaymentId paymentId,
            BankingRequestContext bankingContext,
            Instant requestedAt,
            PaymentPolicyBundle policies
    ) {
        Objects.requireNonNull(paymentId, "Payment ID");
        Objects.requireNonNull(bankingContext, "Banking request context");
        Objects.requireNonNull(requestedAt, "Recovery request instant");
        Objects.requireNonNull(policies, "Payment policies");

        Payment payment = requirePayment(paymentId);
        if (payment.status() != PaymentStatus.POSTING_OUTCOME_UNKNOWN) {
            throw PaymentDomainException.invalidTransition(
                    payment.status(),
                    "recoverPaymentEvent"
            );
        }

        PostingInstructionIdentity instruction =
                payment.toState()
                        .postingInstruction()
                        .orElseThrow();

        PaymentFinancialEventSnapshot snapshot =
                requireFinalizedSnapshot(paymentId);

        PaymentEventRecoveryPort.PaymentEventRecoveryResult recovery =
                recoveryPort.recover(
                        new PaymentEventRecoveryPort
                                .PaymentEventRecoveryQuery(
                                payment.publicPaymentReference(),
                                bankingContext,
                                new BankingIdempotencyKey(
                                        instruction
                                                .idempotencyKey()
                                                .value()
                                )
                        )
                );

        if (recovery.status()
                == PaymentEventRecoveryPort
                        .PaymentEventRecoveryStatus.NOT_FOUND) {
            return PaymentWorkflowResult.from(
                    payment,
                    false
            );
        }

        PaymentEventExecutionPort.PaymentEventExecutionResult providerResult =
                Objects.requireNonNull(
                        recovery.result(),
                        "Recovery provider result"
                );

        PaymentEventObservationSource source =
                Objects.requireNonNull(
                        recovery.source(),
                        "Recovery observation source"
                );

        return finalizationService.resolvePaymentEventOutcome(
                paymentId,
                outcomeMapper.toSnapshot(
                        instruction,
                        providerResult,
                        payment.toState()
                                .paymentEventOutcomeEvidence()
                                .orElseThrow()
                                .accountingDate(),
                        source
                ),
                failureFor(
                        providerResult,
                        FailureStage.POSTING_RESOLUTION
                ),
                providerResult.observedAt(),
                policies
        );
    }

    private PaymentWorkflowResult recordUnknown(
            PaymentId paymentId,
            PostingInstructionIdentity instruction,
            PaymentEventExecutionPort.PaymentEventMappingContext mappingContext,
            PaymentPolicyBundle policies
    ) {
        PaymentEventExecutionPort.PaymentEventExecutionResult syntheticUnknown =
                new PaymentEventExecutionPort.PaymentEventExecutionResult(
                        requirePayment(paymentId)
                                .publicPaymentReference()
                                .value(),
                        PaymentEventOutcome.UNKNOWN,
                        java.util.Arrays.stream(
                                        com.sixpay.payment.domain.model
                                                .evidence
                                                .FundsControlCheckType
                                                .values()
                                )
                                .map(type ->
                                        new PaymentEventExecutionPort.PaymentEventExecutionCheck(
                                                type,
                                                com.sixpay.payment.domain.model.evidence.EvidenceCheckResult.UNKNOWN,
                                                null
                                        )
                                )
                                .toList(),
                        null,
                        null,
                        mappingContext.requestedAt()
                );

        return finalizationService.recordPaymentEventOutcome(
                paymentId,
                outcomeMapper.toSnapshot(
                        instruction,
                        syntheticUnknown,
                        mappingContext.accountingDate(),
                        PaymentEventObservationSource.DIRECT_RESPONSE
                ),
                unknownFailure(
                        FailureStage.POSTING,
                        mappingContext.requestedAt()
                ),
                mappingContext.requestedAt(),
                policies
        );
    }

    private Payment requirePayment(PaymentId paymentId) {
        return paymentLookupPort.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId)
                );
    }

    private PaymentFinancialEventSnapshot requireFinalizedSnapshot(
            PaymentId paymentId
    ) {
        PaymentFinancialEventSnapshot snapshot =
                snapshotRepository.findByPaymentId(paymentId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Finalized financial snapshot is required"
                                )
                        );

        if (snapshot.status()
                != FinancialSnapshotStatus.FINALIZED) {
            throw new IllegalStateException(
                    "Payment financial snapshot must be finalized"
            );
        }

        return snapshot;
    }

    private static PaymentFailure failureFor(
            PaymentEventExecutionPort.PaymentEventExecutionResult result,
            FailureStage stage
    ) {
        if (result.outcome() == PaymentEventOutcome.COMPLETED) {
            return null;
        }

        Instant observedAt = Objects.requireNonNull(
                result.observedAt(),
                "Observed instant"
        );

        if (result.outcome() == PaymentEventOutcome.REJECTED) {
            return new PaymentFailure(
                    FailureCode.of(
                            result.reasonCode() == null
                                    ? "POSTING_REJECTED"
                                    : result.reasonCode().value()
                    ),
                    FailureCategory.BUSINESS_REJECTION,
                    stage,
                    RetryDisposition.NOT_RETRYABLE,
                    "Core Banking rejected Payment event",
                    observedAt,
                    ExternalSystem.AMPLITUDE
            );
        }

        return unknownFailure(stage, observedAt);
    }

    private static PaymentFailure unknownFailure(
            FailureStage stage,
            Instant observedAt
    ) {
        return new PaymentFailure(
                FailureCode.of("POSTING_OUTCOME_UNKNOWN"),
                FailureCategory.UNCERTAIN_EXTERNAL_OUTCOME,
                stage,
                RetryDisposition.AUTHORITATIVE_LOOKUP_REQUIRED,
                "Authoritative Payment event lookup required",
                observedAt,
                ExternalSystem.AMPLITUDE
        );
    }
}
