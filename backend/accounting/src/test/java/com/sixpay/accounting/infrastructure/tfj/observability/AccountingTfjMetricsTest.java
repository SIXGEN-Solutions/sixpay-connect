package com.sixpay.accounting.infrastructure.tfj.observability;

import com.sixpay.accounting.domain.model.TfjReceiptStatus;
import com.sixpay.accounting.domain.model.TfjStatus;
import com.sixpay.accounting.infrastructure.tfj.persistence.AccountingTfjConfirmationRepositoryAdapter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountingTfjMetricsTest {

    @Test
    void recordsLowCardinalityTfjMetricsAndBacklogGauge() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AccountingTfjConfirmationRepositoryAdapter repository =
                mock(AccountingTfjConfirmationRepositoryAdapter.class);
        when(repository.countPendingFinalityPublication()).thenReturn(3L);

        AccountingTfjMetrics metrics =
                new AccountingTfjMetrics(registry, repository);

        metrics.recordIngestion(
                TfjStatus.INTEGRATED,
                TfjReceiptStatus.ACCEPTED_FOR_MATCHING
        );
        metrics.recordConflict();
        metrics.recordFinalityPublication("SUCCESS", 1);

        assertThat(
                registry.get(AccountingTfjMetrics.INGESTION_COUNTER)
                        .tag("tfj_status", "INTEGRATED")
                        .tag("receipt_status", "ACCEPTED_FOR_MATCHING")
                        .counter()
                        .count()
        ).isEqualTo(1.0);

        assertThat(
                registry.get(AccountingTfjMetrics.CONFLICT_COUNTER)
                        .counter()
                        .count()
        ).isEqualTo(1.0);

        assertThat(
                registry.get(AccountingTfjMetrics.FINALITY_PUBLICATION_COUNTER)
                        .tag("outcome", "SUCCESS")
                        .counter()
                        .count()
        ).isEqualTo(1.0);

        assertThat(
                registry.get(AccountingTfjMetrics.FINALITY_PENDING_GAUGE)
                        .gauge()
                        .value()
        ).isEqualTo(3.0);
    }
}
