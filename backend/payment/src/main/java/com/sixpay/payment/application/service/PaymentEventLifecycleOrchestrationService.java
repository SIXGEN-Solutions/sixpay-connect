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

        PaymentEventExecutionPort.PaymentEventMappingContext mappingContext =
                contextService.resolve(
                        bankingContext,
                        requestedAt
                );

        Payment payment = requirePayment(paymentId);

        if (payment.status() == PaymentStatus.APPROVED_FOR_POSTING) {
            postingPreparationService.authorizePaymentEventPosting(
                    paymentId,
                    instruction,
                    mappingContext.requestedAt()
            );
        } else if (payment.status() != PaymentStatus.POSTING_PENDING) {
            throw PaymentDomainException.invalidTransition(
                    payment.status(),
                    "executePaymentEvent"
            );
        }

        PaymentFinancialEventSnapshot snapshot =
                requireFinalizedSnapshot(paymentId);

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
