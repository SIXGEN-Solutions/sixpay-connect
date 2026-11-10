package com.sixpay.bootstrap.security;

import com.sixpay.security.application.port.input.BootstrapLdapAdministratorUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LdapAdministratorBootstrapActivationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LdapAdministratorBootstrapRunner.class)
            .withBean(BootstrapLdapAdministratorUseCase.class,
                    () -> mock(BootstrapLdapAdministratorUseCase.class))
            .withPropertyValues(
                    "sixpay.security.bootstrap.ldap-admin.user-id=" + UUID.randomUUID(),
                    "sixpay.security.bootstrap.ldap-admin.username=sixpay.testuser",
                    "sixpay.security.bootstrap.ldap-admin.trust-domain=sixpay-test-ldap",
                    "sixpay.security.bootstrap.ldap-admin.stable-subject=stable-test-subject"
            );

    @Test
    void disabledByDefaultInProduction() {
        contextRunner.withInitializer(context ->
                        context.getEnvironment().setActiveProfiles("production"))
                .run(context -> assertThat(context)
                        .doesNotHaveBean(LdapAdministratorBootstrapRunner.class));
    }

    @Test
    void enabledInProduction() {
        contextRunner.withInitializer(context ->
                        context.getEnvironment().setActiveProfiles("production"))
                .withPropertyValues("sixpay.security.bootstrap.ldap-admin.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(LdapAdministratorBootstrapRunner.class));
    }

    @Test
    void enabledInPreproduction() {
        contextRunner.withInitializer(context ->
                        context.getEnvironment().setActiveProfiles("preproduction"))
                .withPropertyValues("sixpay.security.bootstrap.ldap-admin.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(LdapAdministratorBootstrapRunner.class));
    }

    @Test
    void disabledInPreproduction() {
        contextRunner.withInitializer(context ->
                        context.getEnvironment().setActiveProfiles("preproduction"))
                .withPropertyValues("sixpay.security.bootstrap.ldap-admin.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(LdapAdministratorBootstrapRunner.class));
    }
}
