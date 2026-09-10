package com.sixpay.payment.infrastructure.messaging.internal;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.command.RecordTfjConfirmationCommand;
import com.sixpay.payment.application.port.input.PaymentReconciliationUseCase;
import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.payment.domain.policy.UniqueTfjMatchProof;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "sixpay.messaging",
        name = "transport",
        havingValue = "internal",
        matchIfMissing = true
)
public final class PaymentTfjFinalityIntegrationEventListener {

    static final String EVENT_TYPE =
            "AccountingTfjFinalityResolved";

    private final PaymentReconciliationUseCase reconciliationUseCase;
    private final PaymentPolicyBundle policies;
    private final ObjectMapper objectMapper;
    private final TimeProvider timeProvider;

    public PaymentTfjFinalityIntegrationEventListener(
            PaymentReconciliationUseCase reconciliationUseCase,
            PaymentPolicyBundle policies,
            ObjectMapper objectMapper,
            TimeProvider timeProvider
    ) {
        this.reconciliationUseCase =
                Objects.requireNonNull(reconciliationUseCase);
        this.policies = Objects.requireNonNull(policies);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    @EventListener
    public void onIntegrationEvent(
            IntegrationEventEnvelope event
    ) {
        if (!EVENT_TYPE.equals(event.eventType())
                || !"PAYMENT".equals(event.aggregateType())) {
            return;
        }

        TfjFinalityPayload payload =
                objectMapper.readValue(
                        event.payload(),
                        TfjFinalityPayload.class
                );

        TfjConfirmationId confirmationId =
                new TfjConfirmationId(
                        payload.confirmationId()
                );

        TfjFailureEvidence failureEvidence = null;
        PaymentFailure paymentFailure = null;

        if ("FAILED".equals(payload.tfjStatus())) {
            TfjRecoveryAction recoveryAction =
                    TfjRecoveryAction.valueOf(
                            payload.recoveryAction()
                    );
            FailureCode failureCode =
                    FailureCode.of(
                            payload.failureCode()
                    );

            failureEvidence =
                    new TfjFailureEvidence(
                            failureCode,
                            recoveryAction
                    );

            RetryDisposition disposition =
                    recoveryAction
                            == TfjRecoveryAction.REVERSAL_REQUIRED
                            ? RetryDisposition
                                    .RECOVERY_EVENT_REQUIRED
                            : RetryDisposition
                                    .OPERATOR_ACTION_REQUIRED;

            paymentFailure =
                    new PaymentFailure(
                            failureCode,
                            FailureCategory
                                    .TREASURY_RECONCILIATION_FAILURE,
                            FailureStage
                                    .END_OF_DAY_RECONCILIATION,
                            disposition,
                            "Authoritative TFJ failure reported by Amplitude",
                            payload.confirmedAt(),
                            ExternalSystem.AMPLITUDE
                    );
        }

        EndOfDayConfirmationSnapshot evidence =
                new EndOfDayConfirmationSnapshot(
                        confirmationId,
                        FinancialInstitutionCode.of(
                                payload.financialInstitutionCode()
                        ),
                        payload.businessDate(),
                        PublicPaymentReference.of(
                                payload.paymentReference()
                        ),
                        payload.bankPostingReference(),
                        payload.tfjBatchReference(),
                        TfjStatus.valueOf(
                                payload.tfjStatus()
                        ),
                        failureEvidence,
                        payload.confirmedAt(),
                        payload.matchedAt(),
                        new EvidenceMetadata(
                                ExternalSystem.AMPLITUDE,
                                CorrelationId.of(
                                        event.correlationId()
                                ),
                                EvidenceObservationChannel
                                        .valueOf(
                                                payload
                                                        .observationChannel()
                                        ),
                                EvidenceFingerprint.of(
                                        payload
                                                .evidenceFingerprint()
                                ),
                                payload.confirmedAt(),
                                payload.matchedAt()
                        )
                );

        reconciliationUseCase.reconcileTfj(
                new RecordTfjConfirmationCommand(
                        new PaymentId(
                                event.aggregateId()
                        ),
                        evidence,
                        new UniqueTfjMatchProof(
                                confirmationId,
                                true,
                                true,
                                true
                        ),
                        paymentFailure,
                        timeProvider.now(),
                        policies
                )
        );
    }

    record TfjFinalityPayload(
            UUID confirmationId,
            String financialInstitutionCode,
            LocalDate businessDate,
            String paymentReference,
            String bankPostingReference,
            String tfjBatchReference,
            String tfjStatus,
            Instant confirmedAt,
            Instant matchedAt,
            String observationChannel,
            String evidenceFingerprint,
            String failureCode,
            String failureDescription,
            String recoveryAction
    ) {
    }
}
