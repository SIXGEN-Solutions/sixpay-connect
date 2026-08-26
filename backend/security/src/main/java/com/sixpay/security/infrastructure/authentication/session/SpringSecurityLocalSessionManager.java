package com.sixpay.security.infrastructure.authentication.session;

import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * @deprecated DA-8 replaced Local-specific session ownership with the
 * mechanism-neutral {@link SpringSecuritySessionManager}.
 */
@Deprecated(forRemoval = true)
public final class SpringSecurityLocalSessionManager
        extends SpringSecuritySessionManager {

    public SpringSecurityLocalSessionManager(
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository
    ) {
        super(securityContextRepository, csrfTokenRepository);
    }
}
