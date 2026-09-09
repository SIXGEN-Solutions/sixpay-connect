package com.sixpay.accounting.infrastructure.tfj.observability;

import com.sixpay.accounting.domain.model.TfjReceiptStatus;
import com.sixpay.accounting.domain.model.TfjStatus;
import com.sixpay.accounting.infrastructure.tfj.persistence.AccountingTfjConfirmationRepositoryAdapter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Low-cardinality observability for Accounting TFJ processing.
 *
 * <p>Payment references, bank references, confirmation identifiers and
 * correlation identifiers are deliberately excluded from metric tags.</p>
 */
@Component
@ConditionalOnBean({
        MeterRegistry.class,
        AccountingTfjConfirmationRepositoryAdapter.class
})
public final class AccountingTfjMetrics {

    static final String INGESTION_COUNTER =
            "sixpay.accounting.tfj.ingestion";
    static final String CONFLICT_COUNTER =
            "sixpay.accounting.tfj.conflicts";
    static final String FINALITY_PUBLICATION_COUNTER =
            "sixpay.accounting.tfj.finality.publication";
    static final String FINALITY_PENDING_GAUGE =
            "sixpay.accounting.tfj.finality.pending";

    private final MeterRegistry registry;

    public AccountingTfjMetrics(
            MeterRegistry registry,
            AccountingTfjConfirmationRepositoryAdapter repository
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(repository, "repository");

        Gauge.builder(
                        FINALITY_PENDING_GAUGE,
                        repository,
                        AccountingTfjConfirmationRepositoryAdapter
                                ::countPendingFinalityPublication
                )
                .description(
                        "Matched terminal TFJ confirmations awaiting Payment finality publication"
                )
                .register(registry);
    }

    public void recordIngestion(
            TfjStatus status,
            TfjReceiptStatus receiptStatus
    ) {
        Counter.builder(INGESTION_COUNTER)
                .description("Accounting TFJ ingestion outcomes")
                .tag("tfj_status", Objects.requireNonNull(status).name())
                .tag(
                        "receipt_status",
                        Objects.requireNonNull(receiptStatus).name()
                )
                .register(registry)
                .increment();
    }

    public void recordConflict() {
        Counter.builder(CONFLICT_COUNTER)
                .description("Conflicting TFJ replay attempts")
                .register(registry)
                .increment();
    }

    public void recordFinalityPublication(
            String outcome,
            double count
    ) {
        if (outcome == null || outcome.isBlank() || outcome.length() > 64) {
            throw new IllegalArgumentException(
                    "finality publication metric outcome is invalid"
            );
        }
        if (count <= 0) {
            return;
        }

        Counter.builder(FINALITY_PUBLICATION_COUNTER)
                .description("Accounting to Payment TFJ finality publication outcomes")
                .tag("outcome", outcome)
                .register(registry)
                .increment(count);
    }
}
