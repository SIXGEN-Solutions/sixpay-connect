package com.sixpay.security.domain.authentication;

import com.sixpay.common.validation.Preconditions;

/**
 * Provider-neutral external identity established by an authentication protocol.
 *
 * <p>The issuer identifies the trust domain. The subject is the stable
 * provider-side identity key. Neither value is exposed to business modules as
 * a provider-specific type.</p>
 */
public record ExternalIdentity(
        String issuer,
        String subject,
        String username
) {

    public ExternalIdentity {
        issuer = Preconditions.requireNonBlank(
                issuer,
                "External identity issuer must not be blank"
        );
        subject = Preconditions.requireNonBlank(
                subject,
                "External identity subject must not be blank"
        );
        username = Preconditions.requireNonBlank(
                username,
                "External identity username must not be blank"
        );
    }
}
