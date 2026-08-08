package com.sixpay.reporting.configuration;

import com.sixpay.reporting.application.port.output.*;
import com.sixpay.reporting.application.service.PaymentAuditExportService;
import com.sixpay.reporting.application.service.PaymentAuditQueryService;
import com.sixpay.reporting.infrastructure.export.AsyncPaymentAuditExportWorker;
import com.sixpay.reporting.infrastructure.query.HmacAuditCursorCodec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties({
        ReportingAuditQueryProperties.class,
        ReportingAuditExportProperties.class
})
public class ReportingConfiguration {

    @Bean
    Clock reportingAuditClock() {
        return Clock.systemUTC();
    }

    @Bean
    AuditCursorCodec reportingAuditCursorCodec(
            ReportingAuditQueryProperties properties
    ) {
        return new HmacAuditCursorCodec(
                properties.decodedKey()
        );
    }

    @Bean
    PaymentAuditQueryService paymentAuditQueryService(
            PaymentAuditReadPort readPort,
            AuditCursorCodec cursorCodec
    ) {
        return new PaymentAuditQueryService(
                readPort,
                cursorCodec
        );
    }

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
