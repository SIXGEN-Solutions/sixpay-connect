package com.sixpay.security.configuration;

import com.sixpay.security.application.port.output.ExternalIdentityResolver;
import com.sixpay.security.application.port.output.FindLinkedIdentityPort;
import com.sixpay.security.application.service.LinkedExternalIdentityResolver;
import com.sixpay.security.infrastructure.authentication.identity.JpaLinkedIdentityAdapter;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserIdentitySpringDataRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdentityLinkingConfiguration {

    @Bean
    @ConditionalOnMissingBean(FindLinkedIdentityPort.class)
    FindLinkedIdentityPort findLinkedIdentityPort(
            SecurityUserIdentitySpringDataRepository repository
    ) {
        return new JpaLinkedIdentityAdapter(repository);
    }

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
