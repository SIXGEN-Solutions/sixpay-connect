package com.sixpay.security.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provider-neutral authentication capability configuration.
 *
 * <p>DA-2 introduces independent Local and OIDC capabilities. The properties
 * are intentionally configuration-only: Local/OIDC filter-chain composition
 * is implemented by later authentication lots.</p>
 */
@ConfigurationProperties("sixpay.security.authentication")
public record AuthenticationCapabilitiesProperties(
        Local local,
        Oidc oidc
) {

    public AuthenticationCapabilitiesProperties {
        local = local == null
                ? new Local(false)
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
            boolean enabled
    ) {
    }

    public record Oidc(
            boolean enabled,
            String registrationId
    ) {
    }
}
