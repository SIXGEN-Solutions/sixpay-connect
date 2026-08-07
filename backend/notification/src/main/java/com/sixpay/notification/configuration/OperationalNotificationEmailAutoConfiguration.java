package com.sixpay.notification.configuration;

import com.sixpay.notification.application.port.output.AdminEmailAddressResolver;
import com.sixpay.notification.application.port.output.OperationalNotificationDeliveryGateway;
import com.sixpay.notification.application.port.output.SixPayAdminRecipientResolver;
import com.sixpay.notification.domain.policy.OperationalNotificationTemplateCatalog;
import com.sixpay.notification.infrastructure.operational.email.ConfiguredSixPayAdminRecipientResolver;
import com.sixpay.notification.infrastructure.operational.email.OperationalEmailTemplateRenderer;
import com.sixpay.notification.infrastructure.operational.email.OperationalNotificationEmailProperties;
import com.sixpay.notification.infrastructure.operational.email.OperationalSmtpNotificationDeliveryGateway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;

@AutoConfiguration(
        before = OperationalNotificationApplicationAutoConfiguration.class
)
@EnableConfigurationProperties(
        OperationalNotificationEmailProperties.class
)
@ConditionalOnProperty(
        prefix = OperationalNotificationEmailProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class OperationalNotificationEmailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean({
            SixPayAdminRecipientResolver.class,
            AdminEmailAddressResolver.class
    })
    ConfiguredSixPayAdminRecipientResolver
    configuredSixPayAdminRecipientResolver(
            OperationalNotificationEmailProperties properties
    ) {
        return new ConfiguredSixPayAdminRecipientResolver(
                properties
        );
    }

    @Bean
    @ConditionalOnMissingBean
    OperationalEmailTemplateRenderer
    operationalEmailTemplateRenderer(
            OperationalNotificationTemplateCatalog catalog
    ) {
        return new OperationalEmailTemplateRenderer(
                catalog
        );
    }

    @Bean
    @ConditionalOnClass(JavaMailSender.class)
    @ConditionalOnBean({
            JavaMailSender.class,
            AdminEmailAddressResolver.class,
            OperationalEmailTemplateRenderer.class
    })
    @ConditionalOnMissingBean(
            OperationalNotificationDeliveryGateway.class
    )
    OperationalNotificationDeliveryGateway
    operationalNotificationDeliveryGateway(
            JavaMailSender mailSender,
            AdminEmailAddressResolver addressResolver,
            OperationalEmailTemplateRenderer renderer,
            OperationalNotificationEmailProperties properties
    ) {
        return new OperationalSmtpNotificationDeliveryGateway(
                mailSender,
                addressResolver,
                renderer,
                properties
        );
    }
}
