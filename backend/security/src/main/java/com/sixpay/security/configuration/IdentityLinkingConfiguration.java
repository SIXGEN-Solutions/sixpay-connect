package com.sixpay.security.configuration;

import com.sixpay.security.application.port.output.ExternalIdentityResolver;
import com.sixpay.security.application.port.output.FindLinkedIdentityPort;
import com.sixpay.security.application.service.LinkedExternalIdentityResolver;
import com.sixpay.security.infrastructure.authentication.identity.JpaLinkedIdentityAdapter;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserIdentitySpringDataRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdentityLinkingConfiguration {

    @Bean
    @ConditionalOnBean(SecurityUserIdentitySpringDataRepository.class)
    JpaLinkedIdentityAdapter linkedIdentityAdapter(
            SecurityUserIdentitySpringDataRepository repository
    ) {
        return new JpaLinkedIdentityAdapter(repository);
    }

    @Bean
    @ConditionalOnBean(JpaLinkedIdentityAdapter.class)
    @ConditionalOnMissingBean(FindLinkedIdentityPort.class)
    FindLinkedIdentityPort findLinkedIdentityPort(
            JpaLinkedIdentityAdapter adapter
    ) {
        return adapter;
    }

    @Bean
    @ConditionalOnBean(FindLinkedIdentityPort.class)
    @ConditionalOnMissingBean(ExternalIdentityResolver.class)
    ExternalIdentityResolver externalIdentityResolver(
            FindLinkedIdentityPort findLinkedIdentityPort
    ) {
        return new LinkedExternalIdentityResolver(
                findLinkedIdentityPort
        );
    }
}
