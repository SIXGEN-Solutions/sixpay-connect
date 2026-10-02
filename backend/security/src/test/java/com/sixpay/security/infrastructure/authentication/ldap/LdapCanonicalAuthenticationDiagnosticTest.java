package com.sixpay.security.infrastructure.authentication.ldap;

import com.sixpay.security.application.exception.ExternalIdentityNotLinkedException;
import com.sixpay.security.application.exception.LdapAuthenticationFailedException;
import com.sixpay.security.application.exception.SixpayUserDisabledException;
import com.sixpay.security.application.port.input.LdapAuthenticationCommand;
import com.sixpay.security.application.service.LdapCanonicalAuthenticationService;
import com.sixpay.security.application.service.LinkedExternalIdentityResolver;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import com.sixpay.security.infrastructure.authentication.identity.JpaLinkedIdentityAdapter;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserIdentitySpringDataRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LdapCanonicalAuthenticationDiagnosticTest {

    @Test
    void authenticatesAndResolvesCanonicalSixpayUser() {
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
                        serviceAccountPassword, Duration.ofSeconds(10),
                        Duration.ofSeconds(20), Duration.ofSeconds(60)
                );

        ActiveDirectoryLdapAuthenticationAdapter ldapAdapter =
                new ActiveDirectoryLdapAuthenticationAdapter(properties);

        var ldapResult = authenticateLdap(
                ldapAdapter,
                new LdapAuthenticationCommand(username, password)
        );

        System.out.println(
                "LDAP_CANONICAL_DIAGNOSTIC_LDAP_SUCCESS"
                        + " identityType=" + ldapResult.identityType()
                        + " provider=" + ldapResult.externalIdentity().issuer()
                        + " subject=" + ldapResult.externalIdentity().subject()
                        + " username=" + ldapResult.externalIdentity().username()
        );

        verifyPersistedIdentity(
                ldapResult.identityType().name(),
                ldapResult.externalIdentity().issuer(),
                ldapResult.externalIdentity().subject()
        );
    }


    private static void verifyPersistedIdentity(
            String identityType,
            String provider,
            String providerSubject
    ) {
        String jdbcUrl = requiredEnvironment("SIXPAY_DIAGNOSTIC_DB_URL");
        String dbUsername = requiredEnvironment("SIXPAY_DIAGNOSTIC_DB_USERNAME");
        String dbPassword = requiredEnvironment("SIXPAY_DIAGNOSTIC_DB_PASSWORD");

        String sql = """
                select a.id, a.username, a.status
                from sixpay.security_user_identities i
                join sixpay.security_user_accounts a on a.id = i.user_id
                where i.identity_type = ?
                  and i.provider = ?
                  and i.provider_subject = ?
                """;

        try (java.sql.Connection connection =
                     java.sql.DriverManager.getConnection(
                             jdbcUrl, dbUsername, dbPassword
                     );
             java.sql.PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, identityType);
            statement.setString(2, provider);
            statement.setString(3, providerSubject);

            try (java.sql.ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new AssertionError(
                            "LDAP_CANONICAL_DIAGNOSTIC_RESOLUTION_FAILURE"
                                    + " reason=EXTERNAL_IDENTITY_NOT_LINKED"
                                    + " identityType=" + identityType
                                    + " provider=" + provider
                                    + " subject=" + providerSubject
                    );
                }

                String id = result.getString("id");
                String username = result.getString("username");
                String status = result.getString("status");

                if (result.next()) {
                    throw new AssertionError(
                            "LDAP_CANONICAL_DIAGNOSTIC_RESOLUTION_FAILURE"
                                    + " reason=MULTIPLE_IDENTITY_LINKS"
                    );
                }

                System.out.println(
                        "LDAP_CANONICAL_DIAGNOSTIC_RESOLUTION_SUCCESS"
                                + " userId=" + id
                                + " username=" + username
                                + " status=" + status
                );

                assertThat(status).isEqualTo("ACTIVE");
            }
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException(
                    "LDAP_CANONICAL_DIAGNOSTIC_DATABASE_FAILURE: "
                            + exception.getClass().getName(),
                    exception
            );
        }
    }

    private static com.sixpay.security.domain.authentication.LdapAuthenticationResult authenticateLdap(
            ActiveDirectoryLdapAuthenticationAdapter adapter,
            LdapAuthenticationCommand command
    ) {
        try {
            return adapter.authenticate(command);
        } catch (LdapAuthenticationFailedException exception) {
            System.err.println("LDAP_CANONICAL_DIAGNOSTIC_LDAP_FAILURE");
            printSafeCauseChain(exception);
            throw exception;
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required diagnostic environment variable: " + name
            );
        }
        return value;
    }

    private static void printSafeCauseChain(Throwable failure) {
        Throwable current = failure;
        int depth = 0;
        while (current != null && depth < 12) {
            System.err.println(
                    "cause[" + depth + "]=" + current.getClass().getName()
            );
            current = current.getCause();
            depth++;
        }
    }
}
