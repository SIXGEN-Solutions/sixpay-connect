package com.sixpay.security.application.port.output;

import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.ExternalIdentity;

/**
 * Resolves a trusted external identity to the canonical SIXPAY principal.
 *
 * <p>Authorization is loaded from SIXPAY-owned user data. External provider
 * claims are never passed as business authorities through this contract.</p>
 */
@FunctionalInterface
public interface ExternalIdentityResolver {

    AuthenticatedUser resolve(
            ExternalIdentity externalIdentity
    );
}
