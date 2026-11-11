package com.sixpay.reporting.configuration;

import com.sixpay.reporting.application.port.output.*;
import com.sixpay.reporting.application.service.PaymentAuditExportService;
import com.sixpay.reporting.infrastructure.export.AsyncPaymentAuditExportWorker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Exports seulement : Audit Query et l'enregistrement des audits restent actifs. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ReportingAuditExportProperties.class)
@ConditionalOnProperty(
        prefix = "sixpay.reporting.audit-export",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ReportingAuditExportConfiguration {

    @Bean(destroyMethod = "close")
    ExecutorService reportingAuditExportExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    AsyncPaymentAuditExportWorker paymentAuditExportWorker(
            AuditExportJobStore jobStore,
            AuditExportGeneratorPort generator,
            AuditExportArtifactStore artifactStore,
            ExecutorService reportingAuditExportExecutor
    ) {
        return new AsyncPaymentAuditExportWorker(
                jobStore,
                generator,
                artifactStore,
                reportingAuditExportExecutor
        );
    }

    @Bean
    PaymentAuditExportService paymentAuditExportService(
            AuditExportJobStore jobStore,
            AsyncPaymentAuditExportWorker worker,
            @Qualifier("reportingAuditClock")
            Clock reportingAuditClock,
            ReportingAuditExportProperties properties
    ) {
        return new PaymentAuditExportService(
                jobStore,
                worker,
                reportingAuditClock,
                properties.retention()
        );
    }
}
