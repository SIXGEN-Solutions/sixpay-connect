package com.sixpay.security.infrastructure.authentication.ldap;

import com.sixpay.security.application.exception.LdapAuthenticationFailedException;
import com.sixpay.security.application.port.input.LdapAuthenticationCommand;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveDirectoryLdapAuthenticationDiagnosticTest {

    @Test
    void authenticatesAgainstConfiguredPreproductionDirectory() {
        String ldapUrl = requiredEnvironment("SIXPAY_LDAP_URLS");
        String baseDn = requiredEnvironment("SIXPAY_LDAP_BASE_DN");
        String userSearchBase = requiredEnvironment("SIXPAY_LDAP_USER_SEARCH_BASE");
        String serviceAccountDn = requiredEnvironment("SIXPAY_LDAP_SERVICE_ACCOUNT_DN");
        String serviceAccountPassword = requiredEnvironment("SIXPAY_LDAP_SERVICE_ACCOUNT_PASSWORD");
        String trustDomain = requiredEnvironment("SIXPAY_LDAP_TRUST_DOMAIN");
        String username = requiredEnvironment("SIXPAY_LDAP_DIAGNOSTIC_USERNAME");
        String password = requiredEnvironment("SIXPAY_LDAP_DIAGNOSTIC_PASSWORD");

        AuthenticationCapabilitiesProperties.Ldap properties =
                new AuthenticationCapabilitiesProperties.Ldap(
                        true, List.of(ldapUrl), baseDn, userSearchBase,
                        "(sAMAccountName={0})", "sAMAccountName", "sAMAccountName",
                        "objectGUID", trustDomain, serviceAccountDn,
                        serviceAccountPassword, Duration.ofSeconds(3),
                        Duration.ofSeconds(5), Duration.ofSeconds(10)
                );

        ActiveDirectoryLdapAuthenticationAdapter adapter =
                new ActiveDirectoryLdapAuthenticationAdapter(properties);

        try {
            var result = adapter.authenticate(new LdapAuthenticationCommand(username, password));
            assertThat(result.externalIdentity().issuer()).isEqualTo(trustDomain);
            System.out.println(
                    "LDAP_DIAGNOSTIC_SUCCESS identityType=" + result.identityType()
            );
            assertThat(result.externalIdentity().subject()).isNotBlank();
            System.out.println("LDAP_DIAGNOSTIC_SUCCESS identityType=" + result.identityType());
        } catch (LdapAuthenticationFailedException exception) {
            printSafeCauseChain(exception);
            throw exception;
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required diagnostic environment variable: " + name);
        }
        return value;
    }

    private static void printSafeCauseChain(Throwable failure) {
        System.err.println("LDAP_DIAGNOSTIC_FAILURE");
        Throwable current = failure;
        int depth = 0;
        while (current != null && depth < 12) {
            System.err.println("cause[" + depth + "]=" + current.getClass().getName());
            current = current.getCause();
            depth++;
        }
    }
}
