package com.sixpay.security.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("sixpay.security.authentication")
public record AuthenticationCapabilitiesProperties(
        Local local,
        Oidc oidc
) {

    public AuthenticationCapabilitiesProperties {
        local = local == null
                ? new Local(false, 5, Duration.ofMinutes(15), 12)
                : local;

        oidc = oidc == null
                ? new Oidc(false, null)
                : oidc;
    }

    public boolean localEnabled() {
        return local.enabled();
    }

    public boolean oidcEnabled() {
        return oidc.enabled();
    }

    public boolean hybridEnabled() {
        return localEnabled() && oidcEnabled();
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
}