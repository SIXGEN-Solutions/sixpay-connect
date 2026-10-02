package com.sixpay.bootstrap.security;

import com.sixpay.security.application.port.input.BootstrapLdapAdministratorCommand;
import com.sixpay.security.application.port.input.BootstrapLdapAdministratorUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Pre-production composition hook for the one-shot Security bootstrap.
 * No Security repository, JPA entity or infrastructure adapter is consumed here.
 */
@Component
@Profile("preproduction")
@ConditionalOnProperty(
        prefix = "sixpay.security.bootstrap.ldap-admin",
        name = "enabled",
        havingValue = "true"
)
public final class PreproductionLdapAdministratorBootstrap
        implements ApplicationRunner {

    static final String ACTOR = "preproduction-ldap-admin-bootstrap";

    private final BootstrapLdapAdministratorUseCase useCase;
    private final UUID userId;
    private final String username;
    private final String email;
    private final String trustDomain;
    private final String stableSubject;

    public PreproductionLdapAdministratorBootstrap(
            BootstrapLdapAdministratorUseCase useCase,
            @Value("${sixpay.security.bootstrap.ldap-admin.user-id}") UUID userId,
            @Value("${sixpay.security.bootstrap.ldap-admin.username}") String username,
            @Value("${sixpay.security.bootstrap.ldap-admin.email:}") String email,
            @Value("${sixpay.security.bootstrap.ldap-admin.trust-domain}") String trustDomain,
            @Value("${sixpay.security.bootstrap.ldap-admin.stable-subject}") String stableSubject
    ) {
        this.useCase = useCase;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.trustDomain = trustDomain;
        this.stableSubject = stableSubject;
    }

    @Override
    public void run(ApplicationArguments args) {
        useCase.bootstrap(
                new BootstrapLdapAdministratorCommand(
                        userId,
                        username,
                        email,
                        trustDomain,
                        stableSubject,
                        ACTOR
                )
        );
    }
}
