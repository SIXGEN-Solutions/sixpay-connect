package com.sixpay.reporting.configuration;

import com.sixpay.reporting.application.port.output.*;
import com.sixpay.reporting.application.service.PaymentAuditQueryService;
import com.sixpay.reporting.infrastructure.query.HmacAuditCursorCodec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(ReportingAuditQueryProperties.class)
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
}
