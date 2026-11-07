package com.sixpay.payment.application.port.output.partner;

import java.util.Optional;

/**
 * Payment-owned outbound port for canonical Partner identity resolution.
 *
 * <p>Payment never depends on Partner repositories, JPA entities or
 * infrastructure adapters. Runtime composition is provided outside Payment.</p>
 */
public interface PartnerIdentityResolutionPort {

    Optional<ResolvedPartnerIdentity> resolveAuthenticatedPartner(
            String authenticatedSubject
    );

    Optional<ResolvedPartnerIdentity> findByPartnerIdentifier(
            String partnerIdentifier
    );
}
