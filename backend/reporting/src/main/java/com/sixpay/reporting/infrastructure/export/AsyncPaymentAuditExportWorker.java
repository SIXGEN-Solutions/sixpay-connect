package com.sixpay.reporting.infrastructure.export;

import com.sixpay.reporting.application.port.output.*;
import com.sixpay.reporting.application.query.*;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public final class AsyncPaymentAuditExportWorker
        implements AuditExportDispatchPort {

    private final AuditExportJobStore jobStore;
    private final AuditExportGeneratorPort generator;
    private final AuditExportArtifactStore artifactStore;
    private final ExecutorService executor;

    public AsyncPaymentAuditExportWorker(
            AuditExportJobStore jobStore,
            AuditExportGeneratorPort generator,
            AuditExportArtifactStore artifactStore,
            ExecutorService executor
    ) {
        this.jobStore = Objects.requireNonNull(jobStore);
        this.generator = Objects.requireNonNull(generator);
        this.artifactStore = Objects.requireNonNull(artifactStore);
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public void dispatch(UUID exportId) {
        executor.submit(() -> process(exportId));
    }

    @Scheduled(
            fixedDelayString =
                    "${sixpay.reporting.audit-export.recovery-delay-ms:5000}"
    )
    public void recoverAcceptedJobs() {
        for (UUID exportId : jobStore.findAccepted(20)) {
            dispatch(exportId);
        }
    }

    private void process(UUID exportId) {
        jobStore.claim(exportId).ifPresent(job -> {
            try {
                GeneratedAuditExport generated =
                        generator.generate(job);

                StoredAuditExportArtifact stored =
                        artifactStore.store(
                                job,
                                generated
                        );

                jobStore.complete(
                        exportId,
                        generated.recordCount(),
                        generated.checksum(),
                        stored.retrievalUri()
                );
            } catch (RuntimeException exception) {
                jobStore.fail(
                        exportId,
                        "AUDIT_EXPORT_GENERATION_FAILED"
                );
            }
        });
    }
}
