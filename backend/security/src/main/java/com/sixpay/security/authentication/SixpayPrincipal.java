package com.sixpay.security.authentication;

import java.util.Set;

/**
 * Canonical, framework-independent authenticated identity exposed by the
 * SIXPAY security boundary.
 *
 * <p>DA-5 makes {@link #subject()} the canonical SIXPAY user-account subject
 * after Local/OIDC identity resolution. Authentication-mechanism and
 * provider-specific subjects stay inside the security identity-linking
 * boundary.</p>
 */
public interface SixpayPrincipal {

    /**
     * Canonical SIXPAY authenticated subject.
     *
     * @return canonical subject
     */
    String subject();

    String username();

    Set<String> roles();

    Set<String> permissions();

    Set<String> authorities();

    boolean hasAuthority(String authority);
}
