package com.sixpay.security.configuration;

import com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.application.port.out.SecurityUserAdministrationPort;
import com.sixpay.security.application.service.SecurityUserAdministrationService;
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
import com.sixpay.security.infrastructure.authentication.persistence.LocalAuthenticationUserJpaEntity;
import com.sixpay.security.infrastructure.authentication.persistence.LocalAuthenticationUserSpringDataRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = {
        SecurityUserAccountJpaEntity.class,
        SecurityUserIdentityJpaEntity.class,
        LocalAuthenticationUserJpaEntity.class,
        AuthenticationAuditJpaEntity.class,
        SecurityAuditJpaEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        SecurityUserAccountSpringDataRepository.class,
        SecurityUserIdentitySpringDataRepository.class,
        LocalAuthenticationUserSpringDataRepository.class,
        AuthenticationAuditSpringDataRepository.class,
        SecurityAuditSpringDataRepository.class
})
public class SecurityAdministrationConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityAuditPort.class)
    SecurityAuditPort securityAuditPort(
            SecurityAuditSpringDataRepository repository
    ) {
        return new JpaSecurityAuditAdapter(repository);
    }

    @Bean
    @ConditionalOnMissingBean(JpaSecurityUserAdministrationAdapter.class)
    JpaSecurityUserAdministrationAdapter securityUserAdministrationAdapter(
            SecurityUserAccountSpringDataRepository userRepository,
            SecurityUserIdentitySpringDataRepository identityRepository,
            LocalAuthenticationUserSpringDataRepository localRepository,
            SecurityAuditSpringDataRepository auditRepository
    ) {
        return new JpaSecurityUserAdministrationAdapter(
                userRepository,
                identityRepository,
                localRepository,
                auditRepository
        );
    }

    @Bean
    @ConditionalOnMissingBean(SecurityUserAdministrationPort.class)
    SecurityUserAdministrationPort securityUserAdministrationPort(
            JpaSecurityUserAdministrationAdapter adapter
    ) {
        return adapter;
    }

    @Bean
    @ConditionalOnMissingBean(SecurityUserAdministrationUseCase.class)
    SecurityUserAdministrationUseCase securityUserAdministrationUseCase(
            SecurityUserAdministrationPort administrationPort,
            SecurityAuditPort auditPort,
            ObjectProvider<PasswordEncoder> encoderProvider
    ) {
        PasswordEncoder encoder =
                encoderProvider.getIfAvailable(
                        () -> new BCryptPasswordEncoder(12)
                );

        return new SecurityUserAdministrationService(
                administrationPort,
                auditPort,
                encoder
        );
    }
}
