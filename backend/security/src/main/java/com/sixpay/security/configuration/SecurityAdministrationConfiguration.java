package com.sixpay.security.configuration;

import com.sixpay.security.application.port.input.SecurityUserAdministrationUseCase;
import com.sixpay.security.application.port.output.PasswordHistoryPort;
import com.sixpay.security.application.port.output.SecurityAuditPort;
import com.sixpay.security.application.port.output.SecurityUserAdministrationPort;
import com.sixpay.security.application.service.SecurityUserAdministrationService;
import com.sixpay.security.domain.authentication.PasswordPolicy;
import com.sixpay.security.infrastructure.administration.JpaSecurityAuditAdapter;
import com.sixpay.security.infrastructure.administration.JpaSecurityUserAdministrationAdapter;
import com.sixpay.security.infrastructure.administration.SecurityAuditJpaEntity;
import com.sixpay.security.infrastructure.administration.SecurityAuditSpringDataRepository;
import com.sixpay.security.infrastructure.authentication.audit.AuthenticationAuditJpaEntity;
import com.sixpay.security.infrastructure.authentication.audit.AuthenticationAuditSpringDataRepository;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserAccountJpaEntity;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserAccountSpringDataRepository;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserIdentityJpaEntity;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserIdentitySpringDataRepository;
import com.sixpay.security.infrastructure.authentication.persistence.JpaPasswordHistoryAdapter;
import com.sixpay.security.infrastructure.authentication.persistence.LocalAuthenticationUserJpaEntity;
import com.sixpay.security.infrastructure.authentication.persistence.LocalAuthenticationUserSpringDataRepository;
import com.sixpay.security.infrastructure.authentication.persistence.PasswordHistoryJpaEntity;
import com.sixpay.security.infrastructure.authentication.persistence.PasswordHistorySpringDataRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PasswordPolicyProperties.class)
@EntityScan(basePackageClasses = {
        SecurityUserAccountJpaEntity.class, SecurityUserIdentityJpaEntity.class,
        LocalAuthenticationUserJpaEntity.class, PasswordHistoryJpaEntity.class,
        AuthenticationAuditJpaEntity.class, SecurityAuditJpaEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        SecurityUserAccountSpringDataRepository.class, SecurityUserIdentitySpringDataRepository.class,
        LocalAuthenticationUserSpringDataRepository.class, PasswordHistorySpringDataRepository.class,
        AuthenticationAuditSpringDataRepository.class, SecurityAuditSpringDataRepository.class
})
public class SecurityAdministrationConfiguration {
    @Bean @ConditionalOnMissingBean(SecurityAuditPort.class)
    SecurityAuditPort securityAuditPort(SecurityAuditSpringDataRepository repository) {
        return new JpaSecurityAuditAdapter(repository);
    }

    @Bean @ConditionalOnMissingBean(PasswordPolicy.class)
    PasswordPolicy passwordPolicy(PasswordPolicyProperties properties) { return properties.toDomain(); }

    @Bean @ConditionalOnMissingBean(JpaPasswordHistoryAdapter.class)
    JpaPasswordHistoryAdapter passwordHistoryAdapter(
            LocalAuthenticationUserSpringDataRepository localRepository,
            PasswordHistorySpringDataRepository historyRepository
    ) { return new JpaPasswordHistoryAdapter(localRepository, historyRepository); }

    @Bean @ConditionalOnMissingBean(PasswordHistoryPort.class)
    PasswordHistoryPort passwordHistoryPort(JpaPasswordHistoryAdapter adapter) { return adapter; }

    @Bean @ConditionalOnMissingBean(JpaSecurityUserAdministrationAdapter.class)
    JpaSecurityUserAdministrationAdapter securityUserAdministrationAdapter(
            SecurityUserAccountSpringDataRepository userRepository,
            SecurityUserIdentitySpringDataRepository identityRepository,
            LocalAuthenticationUserSpringDataRepository localRepository,
            SecurityAuditSpringDataRepository auditRepository
    ) { return new JpaSecurityUserAdministrationAdapter(userRepository, identityRepository, localRepository, auditRepository); }

    @Bean @ConditionalOnMissingBean(SecurityUserAdministrationPort.class)
    SecurityUserAdministrationPort securityUserAdministrationPort(JpaSecurityUserAdministrationAdapter adapter) { return adapter; }

    @Bean @ConditionalOnMissingBean(SecurityUserAdministrationUseCase.class)
    SecurityUserAdministrationUseCase securityUserAdministrationUseCase(
            SecurityUserAdministrationPort administrationPort,
            SecurityAuditPort auditPort,
            ObjectProvider<PasswordEncoder> encoderProvider,
            PasswordPolicy passwordPolicy,
            PasswordHistoryPort passwordHistoryPort
    ) {
        PasswordEncoder encoder = encoderProvider.getIfAvailable(() -> new BCryptPasswordEncoder(12));
        return new SecurityUserAdministrationService(
                administrationPort, auditPort, encoder, passwordPolicy, passwordHistoryPort
        );
    }
}
