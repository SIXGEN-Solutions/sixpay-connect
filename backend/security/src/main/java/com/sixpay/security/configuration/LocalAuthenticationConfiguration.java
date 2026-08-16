package com.sixpay.security.configuration;

import com.sixpay.common.time.SystemTimeProvider;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.security.api.controller.LocalAuthenticationController;
import com.sixpay.security.api.controller.LocalPasswordController;
import com.sixpay.security.api.error.LocalAuthenticationExceptionHandler;
import com.sixpay.security.api.error.LocalPasswordChangeExceptionHandler;
import com.sixpay.security.application.port.in.AuthenticateLocalUserUseCase;
import com.sixpay.security.application.port.in.ChangeLocalPasswordUseCase;
import com.sixpay.security.application.port.in.LogoutUseCase;
import com.sixpay.security.application.port.out.AuthenticationAuditPort;
import com.sixpay.security.application.port.out.ChangeLocalCredentialPort;
import com.sixpay.security.application.port.out.LoadAuthenticationUserPort;
import com.sixpay.security.application.port.out.PasswordHistoryPort;
import com.sixpay.security.application.port.out.PasswordVerificationPort;
import com.sixpay.security.application.port.out.SaveAuthenticationUserStatePort;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.application.service.LocalAuthenticationService;
import com.sixpay.security.application.service.LocalLogoutService;
import com.sixpay.security.application.service.LocalPasswordChangeService;
import com.sixpay.security.domain.authentication.PasswordPolicy;
import com.sixpay.security.infrastructure.authentication.audit.AuthenticationAuditSpringDataRepository;
import com.sixpay.security.infrastructure.authentication.audit.JpaAuthenticationAuditAdapter;
import com.sixpay.security.infrastructure.authentication.password.BCryptPasswordVerificationAdapter;
import com.sixpay.security.infrastructure.authentication.persistence.JpaLocalAuthenticationUserAdapter;
import com.sixpay.security.infrastructure.authentication.persistence.LocalAuthenticationUserSpringDataRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@Import({
        LocalAuthenticationController.class,
        LocalPasswordController.class,
        LocalAuthenticationExceptionHandler.class,
        LocalPasswordChangeExceptionHandler.class
})
@ConditionalOnProperty(
        prefix = "sixpay.security.authentication.local",
        name = "enabled",
        havingValue = "true"
)
public class LocalAuthenticationConfiguration {

    @Bean
    @ConditionalOnMissingBean(TimeProvider.class)
    TimeProvider localAuthenticationTimeProvider() {
        return new SystemTimeProvider();
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    PasswordEncoder localPasswordEncoder(
            AuthenticationCapabilitiesProperties properties
    ) {
        return new BCryptPasswordEncoder(
                properties.local()
                        .bcryptStrength()
        );
    }

    @Bean
    @ConditionalOnMissingBean(
            PasswordVerificationPort.class
    )
    PasswordVerificationPort passwordVerificationPort(
            PasswordEncoder passwordEncoder
    ) {
        return new BCryptPasswordVerificationAdapter(
                passwordEncoder
        );
    }

    @Bean
    @ConditionalOnMissingBean(
            AuthenticationAuditPort.class
    )
    AuthenticationAuditPort authenticationAuditPort(
            AuthenticationAuditSpringDataRepository repository,
            SecurityAuditPort securityAuditPort
    ) {
        return new JpaAuthenticationAuditAdapter(
                repository,
                securityAuditPort
        );
    }

    @Bean
    JpaLocalAuthenticationUserAdapter
    localAuthenticationUserAdapter(
            LocalAuthenticationUserSpringDataRepository repository,
            TimeProvider timeProvider
    ) {
        return new JpaLocalAuthenticationUserAdapter(
                repository,
                timeProvider
        );
    }

    @Bean
    @ConditionalOnMissingBean(
            LoadAuthenticationUserPort.class
    )
    LoadAuthenticationUserPort
    loadAuthenticationUserPort(
            JpaLocalAuthenticationUserAdapter adapter
    ) {
        return adapter;
    }

    @Bean
    @ConditionalOnMissingBean(
            SaveAuthenticationUserStatePort.class
    )
    SaveAuthenticationUserStatePort
    saveAuthenticationUserStatePort(
            JpaLocalAuthenticationUserAdapter adapter
    ) {
        return adapter;
    }

    @Bean
    @ConditionalOnMissingBean(
            ChangeLocalCredentialPort.class
    )
    ChangeLocalCredentialPort
    changeLocalCredentialPort(
            JpaLocalAuthenticationUserAdapter adapter
    ) {
        return adapter;
    }

    @Bean
    @ConditionalOnMissingBean(
            AuthenticateLocalUserUseCase.class
    )
    AuthenticateLocalUserUseCase
    authenticateLocalUserUseCase(
            LoadAuthenticationUserPort loadAuthenticationUserPort,
            SaveAuthenticationUserStatePort saveAuthenticationUserStatePort,
            PasswordVerificationPort passwordVerificationPort,
            AuthenticationAuditPort authenticationAuditPort,
            TimeProvider timeProvider,
            AuthenticationCapabilitiesProperties properties
    ) {
        return new LocalAuthenticationService(
                loadAuthenticationUserPort,
                saveAuthenticationUserStatePort,
                passwordVerificationPort,
                authenticationAuditPort,
                timeProvider,
                properties.local()
                        .maximumFailedAttempts(),
                properties.local()
                        .lockDuration()
        );
    }

    @Bean
    @ConditionalOnMissingBean(
            ChangeLocalPasswordUseCase.class
    )
    ChangeLocalPasswordUseCase
    changeLocalPasswordUseCase(
            PasswordHistoryPort passwordHistoryPort,
            ChangeLocalCredentialPort credentialPort,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            SecurityAuditPort securityAuditPort,
            TimeProvider timeProvider
    ) {
        return new LocalPasswordChangeService(
                passwordHistoryPort,
                credentialPort,
                passwordEncoder,
                passwordPolicy,
                securityAuditPort,
                timeProvider
        );
    }

    @Bean
    @ConditionalOnMissingBean(
            LogoutUseCase.class
    )
    LogoutUseCase logoutUseCase(
            AuthenticationAuditPort authenticationAuditPort,
            TimeProvider timeProvider
    ) {
        return new LocalLogoutService(
                authenticationAuditPort,
                timeProvider
        );
    }
}
