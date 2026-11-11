package com.sixpay.reporting.configuration;

import com.sixpay.reporting.application.port.output.AuditExportArtifactStore;
import com.sixpay.reporting.application.port.output.AuditExportGeneratorPort;
import com.sixpay.reporting.application.port.output.AuditExportJobStore;
import com.sixpay.reporting.application.port.output.PaymentAuditReadPort;
import com.sixpay.reporting.application.service.PaymentAuditQueryService;
import com.sixpay.reporting.infrastructure.export.AsyncPaymentAuditExportWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReportingAuditExportConditionalConfigurationTest {

    private ApplicationContextRunner context() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        return new ApplicationContextRunner()
                .withUserConfiguration(
                        ReportingConfiguration.class,
                        ReportingAuditExportConfiguration.class
                )
                .withBean(PaymentAuditReadPort.class, () -> mock(PaymentAuditReadPort.class))
                .withBean(AuditExportJobStore.class, () -> mock(AuditExportJobStore.class))
                .withBean(AuditExportGeneratorPort.class, () -> mock(AuditExportGeneratorPort.class))
                .withBean(AuditExportArtifactStore.class, () -> mock(AuditExportArtifactStore.class))
                .withPropertyValues("sixpay.reporting.audit-query.cursor-hmac-key=" + key);
    }

    @Test
    void disabledRequiresNoExportUriAndPreservesAuditQuery() {
        context()
                .withPropertyValues("sixpay.reporting.audit-export.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(PaymentAuditQueryService.class);
                    assertThat(ctx).doesNotHaveBean(ReportingAuditExportProperties.class);
                    assertThat(ctx).doesNotHaveBean(AsyncPaymentAuditExportWorker.class);
                });
    }

    @Test
    void enabledRequiresValidUri() {
        context()
                .withPropertyValues("sixpay.reporting.audit-export.enabled=true")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void enabledWithUriWiresExportWorker() {
        context()
                .withPropertyValues(
                        "sixpay.reporting.audit-export.enabled=true",
                        "sixpay.reporting.audit-export.storage-directory=target/audit-exports-test",
                        "sixpay.reporting.audit-export.retrieval-base-uri=https://example.invalid/audit-exports/"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(PaymentAuditQueryService.class);
                    assertThat(ctx).hasSingleBean(ReportingAuditExportProperties.class);
                    assertThat(ctx).hasSingleBean(AsyncPaymentAuditExportWorker.class);
                });
    }
}
