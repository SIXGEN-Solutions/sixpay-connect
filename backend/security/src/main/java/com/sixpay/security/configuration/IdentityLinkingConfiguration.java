package com.sixpay.security.configuration;

import com.sixpay.security.application.port.output.ExternalIdentityResolver;
import com.sixpay.security.application.port.output.FindLinkedIdentityPort;
import com.sixpay.security.application.service.LinkedExternalIdentityResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdentityLinkingConfiguration {

    @Bean
    @ConditionalOnMissingBean(ExternalIdentityResolver.class)
    ExternalIdentityResolver externalIdentityResolver(
            FindLinkedIdentityPort findLinkedIdentityPort
    ) {
        return new LinkedExternalIdentityResolver(
                findLinkedIdentityPort
        );
    }
}
