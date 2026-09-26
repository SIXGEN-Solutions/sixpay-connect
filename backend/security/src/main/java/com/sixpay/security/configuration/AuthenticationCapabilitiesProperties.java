package com.sixpay.security.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("sixpay.security.authentication")
public record AuthenticationCapabilitiesProperties(
        Local local,
        Oidc oidc,
        Ldap ldap
) {

    public AuthenticationCapabilitiesProperties {
        local = local == null
                ? new Local(false, 5, Duration.ofMinutes(15), 12)
                : local;

        oidc = oidc == null
                ? new Oidc(false, null)
                : oidc;

        ldap = ldap == null
                ? Ldap.disabled()
                : ldap;
    }

    public boolean localEnabled() {
        return local.enabled();
    }

    public boolean oidcEnabled() {
        return oidc.enabled();
    }

    public boolean ldapEnabled() {
        return ldap.enabled();
    }

    public boolean hybridEnabled() {
        int enabledCount = 0;
        enabledCount += localEnabled() ? 1 : 0;
        enabledCount += oidcEnabled() ? 1 : 0;
        enabledCount += ldapEnabled() ? 1 : 0;
        return enabledCount > 1;
    }

    public record Local(
            boolean enabled,
            int maximumFailedAttempts,
            Duration lockDuration,
            int bcryptStrength
    ) {
        private static final int DEFAULT_MAXIMUM_FAILED_ATTEMPTS = 5;

        private static final Duration DEFAULT_LOCK_DURATION =
                Duration.ofMinutes(15);

        private static final int DEFAULT_BCRYPT_STRENGTH = 12;

        public Local {
            maximumFailedAttempts =
                    maximumFailedAttempts > 0
                            ? maximumFailedAttempts
                            : DEFAULT_MAXIMUM_FAILED_ATTEMPTS;

            lockDuration =
                    lockDuration != null
                            && !lockDuration.isZero()
                            && !lockDuration.isNegative()
                            ? lockDuration
                            : DEFAULT_LOCK_DURATION;

            bcryptStrength =
                    bcryptStrength >= 10 && bcryptStrength <= 16
                            ? bcryptStrength
                            : DEFAULT_BCRYPT_STRENGTH;
        }
    }

    public record Oidc(
            boolean enabled,
            String registrationId
    ) {
    }

    public record Ldap(
            boolean enabled,
            java.util.List<String> urls,
            String baseDn,
            String userSearchBase,
            String userSearchFilter,
            String loginAttribute,
            String usernameAttribute,
            String subjectAttribute,
            String trustDomain,
            String serviceAccountDn,
            String serviceAccountPassword,
            Duration connectTimeout,
            Duration readTimeout,
            Duration authenticationTimeout
    ) {
        private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
        private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);
        private static final Duration DEFAULT_AUTHENTICATION_TIMEOUT = Duration.ofSeconds(10);

        public Ldap {
            urls = urls == null ? java.util.List.of() : java.util.List.copyOf(urls);
            userSearchFilter = defaultIfBlank(userSearchFilter, "(sAMAccountName={0})");
            loginAttribute = defaultIfBlank(loginAttribute, "sAMAccountName");
            usernameAttribute = defaultIfBlank(usernameAttribute, "sAMAccountName");
            subjectAttribute = defaultIfBlank(subjectAttribute, "objectGUID");
            trustDomain = defaultIfBlank(trustDomain, "regionale-ldap");
            connectTimeout = positiveOrDefault(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
            readTimeout = positiveOrDefault(readTimeout, DEFAULT_READ_TIMEOUT);
            authenticationTimeout = positiveOrDefault(authenticationTimeout, DEFAULT_AUTHENTICATION_TIMEOUT);

            if (enabled) {
                requireConfigured(urls, "LDAP urls must not be empty when LDAP is enabled");
                requireConfigured(baseDn, "LDAP base DN must not be blank when LDAP is enabled");
                requireConfigured(userSearchBase, "LDAP user search base must not be blank when LDAP is enabled");
                requireConfigured(serviceAccountDn, "LDAP service account DN must not be blank when LDAP is enabled");
                requireConfigured(serviceAccountPassword, "LDAP service account password must not be blank when LDAP is enabled");

                if (urls.stream().anyMatch(url -> url == null || !url.startsWith("ldaps://"))) {
                    throw new IllegalArgumentException(
                            "LDAP URLs must use ldaps:// when LDAP authentication is enabled"
                    );
                }
            }
        }

        static Ldap disabled() {
            return new Ldap(
                    false,
                    java.util.List.of(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        private static Duration positiveOrDefault(Duration candidate, Duration defaultValue) {
            return candidate != null && !candidate.isZero() && !candidate.isNegative()
                    ? candidate
                    : defaultValue;
        }

        private static String defaultIfBlank(String candidate, String defaultValue) {
            return candidate == null || candidate.isBlank()
                    ? defaultValue
                    : candidate.trim();
        }

        private static void requireConfigured(Object candidate, String message) {
            if (candidate == null
                    || candidate instanceof String value && value.isBlank()
                    || candidate instanceof java.util.Collection<?> collection && collection.isEmpty()) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}