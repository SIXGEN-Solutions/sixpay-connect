package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.AccountingBatchQueryPort;
import com.sixpay.accounting.application.port.output.AccountingCandidateProjectionRepository;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchTracking;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.policy.AccountingCutoffMode;
import com.sixpay.accounting.domain.policy.AccountingCutoffPolicy;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;
import com.sixpay.accounting.domain.repository.AccountingBatchTrackingRepository;
import com.sixpay.common.context.CorrelationId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountingT1ManualExecutionServiceTest {

    @Test
    void executesManualT1ThenSubmitsTheBatch() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        Instant now = Instant.parse("2026-09-10T22:00:00Z");

        AccountingCutoffPolicy cutoff =
                mock(AccountingCutoffPolicy.class);
        AccountingCandidateProjectionRepository projections =
                mock(AccountingCandidateProjectionRepository.class);
        AccountingBatchQueryPort query =
                mock(AccountingBatchQueryPort.class);
        AccountingT1OrchestrationService orchestration =
                mock(AccountingT1OrchestrationService.class);
        AccountingBatchReconciliationService reconciliation =
                mock(AccountingBatchReconciliationService.class);
        AccountingBatchTrackingRepository trackingRepository =
                mock(AccountingBatchTrackingRepository.class);

        AccountingSelectionWindow window =
                mock(AccountingSelectionWindow.class);

        when(cutoff.resolve(
                now,
                AccountingCutoffMode.MANUAL,
                Optional.of(date)
        )).thenReturn(window);

        AccountingCandidateProjection candidate =
                mock(AccountingCandidateProjection.class);
        when(candidate.financialInstitutionCode())
                .thenReturn("LAREGIONALE");
        when(projections.findUnbatchedForVerification(window))
                .thenReturn(List.of(candidate));

        AccountingBatch batch = mock(AccountingBatch.class);
        AccountingBatchId batchId = mock(AccountingBatchId.class);
        when(batch.batchId()).thenReturn(batchId);

        CorrelationId correlation =
                new CorrelationId(
                        "11111111-2222-3333-4444-555555555555"
                );

        when(orchestration.execute(
                now,
                AccountingCutoffMode.MANUAL,
                Optional.of(date),
                "LAREGIONALE",
                correlation
        )).thenReturn(batch);

        AccountingBatchTracking tracking =
                mock(AccountingBatchTracking.class);
        when(reconciliation.submitOrReconcile(
                eq(batchId),
                any()
        )).thenReturn(tracking);
        when(query.findById(batchId))
                .thenReturn(Optional.of(batch));

        AccountingT1ManualExecutionService service =
                new AccountingT1ManualExecutionService(
                        Clock.fixed(now, ZoneOffset.UTC),
                        cutoff,
                        projections,
                        query,
                        orchestration,
                        reconciliation,
                        trackingRepository
                );

        var result = service.execute(date, correlation);

        assertSame(batch, result.batch());
        assertSame(tracking, result.tracking());

        verify(orchestration).execute(
                now,
                AccountingCutoffMode.MANUAL,
                Optional.of(date),
                "LAREGIONALE",
                correlation
        );
        verify(reconciliation).submitOrReconcile(
                eq(batchId),
                any()
        );
    }
}
