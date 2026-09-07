package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.PaymentEventExecutionPort;
import com.sixpay.payment.application.port.output.banking.PaymentEventRecoveryPort;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.payment.domain.model.evidence.PostingIdempotencyKey;
import com.sixpay.payment.domain.model.evidence.PostingInstructionId;
import com.sixpay.payment.domain.model.financial.FinancialSnapshotStatus;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import com.sixpay.payment.domain.repository.PaymentFinancialSnapshotRepository;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventLifecycleExecutionGuardTest {

    @Mock
    private PaymentLookupPort paymentLookupPort;
    @Mock
    private PaymentFinancialSnapshotRepository snapshotRepository;
    @Mock
    private PaymentPostingPreparationService postingPreparationService;
    @Mock
    private PaymentFinalizationService finalizationService;
    @Mock
    private PaymentEventContextService contextService;
    @Mock
    private PaymentEventExecutionPort executionPort;
    @Mock
    private PaymentEventRecoveryPort recoveryPort;
    @Mock
    private PaymentPolicyBundle policies;

    @Test
    void samePaymentSameInstructionSameSnapshotNeverExecutesTwice() {
        Fixture fixture = fixture();

        when(fixture.payment.status())
                .thenReturn(PaymentStatus.POSTING_PENDING);
        when(fixture.payment.id()).thenReturn(fixture.paymentId);
        when(fixture.payment.publicPaymentReference())
                .thenReturn(fixture.reference);
        when(fixture.payment.businessVersion()).thenReturn(2L);
        when(fixture.payment.toState()).thenReturn(fixture.state);

        when(fixture.state.postingInstruction())
                .thenReturn(Optional.of(fixture.instruction));

        PaymentWorkflowResult result = service().execute(
                fixture.paymentId,
                fixture.instruction,
                fixture.context,
                fixture.requestedAt,
                policies
        );

        assertThat(result.stateChanged()).isFalse();
        assertThat(result.status())
                .isEqualTo(PaymentStatus.POSTING_PENDING);

        verifyNoInteractions(
                postingPreparationService,
                contextService,
                executionPort,
                finalizationService,
                recoveryPort
        );
    }

    @Test
    void staleConcurrentAuthorizationLoserDoesNotResolveContextOrPost() {
        Fixture fixture = fixture();

        when(fixture.payment.status())
                .thenReturn(PaymentStatus.APPROVED_FOR_POSTING);

        when(postingPreparationService.authorizePaymentEventPosting(
                fixture.paymentId,
                fixture.instruction,
                fixture.requestedAt
        )).thenReturn(
                new PaymentWorkflowResult(
                        fixture.paymentId,
                        fixture.reference,
                        PaymentStatus.POSTING_PENDING,
                        2L,
                        false
                )
        );

        PaymentWorkflowResult result = service().execute(
                fixture.paymentId,
                fixture.instruction,
                fixture.context,
                fixture.requestedAt,
                policies
        );

        assertThat(result.stateChanged()).isFalse();

        verifyNoInteractions(
                contextService,
                executionPort,
                finalizationService,
                recoveryPort
        );
    }

    @Test
    void sameKeyDifferentFinancialRequestIsConflictBeforeAnyBankCall() {
        Fixture fixture = fixture();

        PostingInstructionIdentity conflicting =
                new PostingInstructionIdentity(
                        fixture.instruction.instructionId(),
                        fixture.instruction.idempotencyKey(),
                        fixture.instruction.amount(),
                        fixture.instruction.accountBindingFingerprint(),
                        EvidenceFingerprint.of(
                                "v1:sha256:"
                                        + "f".repeat(64)
                        )
                );

        assertThatThrownBy(() ->
                service().execute(
                        fixture.paymentId,
                        conflicting,
                        fixture.context,
                        fixture.requestedAt,
                        policies
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("financial snapshot");

        verifyNoInteractions(
                postingPreparationService,
                contextService,
                executionPort,
                finalizationService,
                recoveryPort
        );
    }

    private PaymentEventLifecycleOrchestrationService service() {
        return new PaymentEventLifecycleOrchestrationService(
                paymentLookupPort,
                snapshotRepository,
                postingPreparationService,
                finalizationService,
                contextService,
                executionPort,
                recoveryPort
        );
    }

    private Fixture fixture() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        PublicPaymentReference reference =
                PublicPaymentReference.of(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                );
        Money amount =
                Money.of(new BigDecimal("1000.00"), "XAF");
        Instant requestedAt =
                Instant.parse("2026-09-07T05:00:00Z");

        Payment payment = mock(Payment.class);
        PaymentState state = mock(PaymentState.class);
        PaymentFinancialEventSnapshot snapshot =
                mock(PaymentFinancialEventSnapshot.class);

        when(paymentLookupPort.findById(paymentId))
                .thenReturn(Optional.of(payment));
        when(snapshotRepository.findByPaymentId(paymentId))
                .thenReturn(Optional.of(snapshot));

        when(snapshot.status())
                .thenReturn(FinancialSnapshotStatus.FINALIZED);
        when(snapshot.publicPaymentReference()).thenReturn(reference);
        when(snapshot.requestedAmount()).thenReturn(amount);
        when(snapshot.debtorAccountReference())
                .thenReturn("vault:debtor:0001");
        when(snapshot.creditorAccountReference())
                .thenReturn("vault:creditor:0001");

        EvidenceFingerprint fingerprint =
                PaymentT0FinancialRequestFingerprint.from(snapshot);

        PostingInstructionIdentity instruction =
                new PostingInstructionIdentity(
                        new PostingInstructionId(UUID.randomUUID()),
                        new PostingIdempotencyKey(
                                "t0:" + UUID.randomUUID()
                        ),
                        amount,
                        "v1:" + "a".repeat(64),
                        fingerprint
                );

        BankingRequestContext context =
                new BankingRequestContext(
                        CorrelationId.of(
                                UUID.randomUUID().toString()
                        ),
                        FinancialInstitutionCode.of("LRB")
                );

        return new Fixture(
                paymentId,
                reference,
                payment,
                state,
                instruction,
                context,
                requestedAt
        );
    }

    private record Fixture(
            PaymentId paymentId,
            PublicPaymentReference reference,
            Payment payment,
            PaymentState state,
            PostingInstructionIdentity instruction,
            BankingRequestContext context,
            Instant requestedAt
    ) {
    }
}
