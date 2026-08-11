package com.sixpay.security.authentication;

import java.util.Set;

/**
 * Canonical, framework-independent authenticated identity exposed by the
 * SIXPAY security boundary.
 *
 * <p>This contract deliberately contains no authentication-mechanism or
 * Identity-Provider specific concept. Local credentials and OIDC identities
 * must converge to this same representation before authorization and business
 * access are evaluated.</p>
 *
 * <p>The internal SIXPAY user identifier is intentionally not part of DA-1.
 * The current authoritative implementation has no canonical external-identity
 * to SIXPAY-user linking model yet. That identifier must be introduced when
 * identity linking is implemented, rather than being fabricated from an OIDC
 * subject.</p>
 */
public interface SixpayPrincipal {

    /**
     * Stable subject supplied by the authenticated security context.
     *
     * @return authenticated subject
     */
    String subject();

    /**
     * Human-readable username associated with the authenticated identity.
     *
     * @return username
     */
    String username();

    /**
     * SIXPAY role names without the Spring Security {@code ROLE_} prefix.
     *
     * @return immutable role names
     */
    Set<String> roles();

    /**
     * Granted non-role authorities/permissions.
     *
     * <p>Existing SIXPAY scopes such as {@code SCOPE_payment.read} are exposed
     * through this set without coupling the principal contract to a specific
     * authorization vocabulary.</p>
     *
     * @return immutable permissions
     */
    Set<String> permissions();

    /**
     * Complete granted-authority set retained as the compatibility boundary
     * for existing SIXPAY authorization policies.
     *
     * @return immutable authorities
     */
    Set<String> authorities();

    /**
     * Tests whether the principal owns the supplied authority.
     *
     * @param authority authority to test
     * @return {@code true} when granted
     */
    boolean hasAuthority(String authority);
}
