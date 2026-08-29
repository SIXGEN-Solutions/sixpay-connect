package com.sixpay.notification.configuration;

import com.sixpay.notification.application.port.output.PartnerNotificationSender;
import com.sixpay.notification.infrastructure.email.EmailPartnerNotificationSender;
import com.sixpay.notification.infrastructure.email.LoggingPartnerNotificationSender;
import com.sixpay.notification.infrastructure.email.NotificationEmailProperties;
import com.sixpay.notification.infrastructure.email.PartnerEmailTemplateRenderer;
import com.sixpay.notification.infrastructure.email.smtp.SmtpEmailGateway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;

@AutoConfiguration(before = NotificationApplicationAutoConfiguration.class)
@EnableConfigurationProperties(NotificationEmailProperties.class)
public class NotificationEmailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    PartnerEmailTemplateRenderer partnerEmailTemplateRenderer() {
        return new PartnerEmailTemplateRenderer();
    }

    @Bean
    @ConditionalOnMissingBean(PartnerNotificationSender.class)
    @ConditionalOnProperty(
            prefix = "sixpay.notification.email",
            name = "mode",
            havingValue = "logging",
            matchIfMissing = true
    )
    LoggingPartnerNotificationSender loggingPartnerNotificationSender(
            PartnerEmailTemplateRenderer renderer,
            NotificationEmailProperties properties
    ) {
        return new LoggingPartnerNotificationSender(renderer, properties);
    }

    @Bean
    @ConditionalOnClass(JavaMailSender.class)
    @ConditionalOnBean(JavaMailSender.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "sixpay.notification.email",
            name = "mode",
            havingValue = "smtp"
    )
    SmtpEmailGateway smtpEmailGateway(JavaMailSender mailSender) {
        return new SmtpEmailGateway(mailSender);
    }

    @Bean
    @ConditionalOnBean(SmtpEmailGateway.class)
    @ConditionalOnMissingBean(PartnerNotificationSender.class)
    @ConditionalOnProperty(
            prefix = "sixpay.notification.email",
            name = "mode",
            havingValue = "smtp"
    )
    EmailPartnerNotificationSender emailPartnerNotificationSender(
            PartnerEmailTemplateRenderer renderer,
            SmtpEmailGateway gateway,
            NotificationEmailProperties properties
    ) {
        return new EmailPartnerNotificationSender(
                renderer,
                gateway,
                properties
        );
    }
}
