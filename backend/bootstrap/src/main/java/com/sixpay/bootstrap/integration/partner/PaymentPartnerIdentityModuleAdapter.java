package com.sixpay.bootstrap.integration.partner;

import com.sixpay.partner.application.port.input.PartnerIdentityQueryUseCase;
import com.sixpay.payment.application.port.output.partner.PartnerIdentityResolutionPort;
import com.sixpay.payment.application.port.output.partner.ResolvedPartnerIdentity;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;

import java.util.Objects;
import java.util.Optional;

/**
 * Bootstrap composition adapter between Payment, Partner and Security public
 * surfaces.
 *
 * <p>No business decision is implemented here. Partner owns canonical Partner
 * resolution; Security owns authenticated identity; Payment owns consistency
 * checks.</p>
 */
public final class PaymentPartnerIdentityModuleAdapter
        implements PartnerIdentityResolutionPort {

    private final CurrentUserProvider currentUserProvider;
    private final PartnerIdentityQueryUseCase partnerIdentityQuery;

    public PaymentPartnerIdentityModuleAdapter(
            CurrentUserProvider currentUserProvider,
            PartnerIdentityQueryUseCase partnerIdentityQuery
    ) {
        this.currentUserProvider = Objects.requireNonNull(
                currentUserProvider,
                "Current user provider"
        );
        this.partnerIdentityQuery = Objects.requireNonNull(
                partnerIdentityQuery,
                "Partner identity query use case"
        );
    }

    @Override
    public Optional<ResolvedPartnerIdentity> resolveAuthenticatedPartner(
            String authenticatedSubject
    ) {
        AuthenticatedUser current =
                currentUserProvider.requireCurrentUser();

        if (!current.subject().equals(authenticatedSubject)) {
            return Optional.empty();
        }

        /*
         * The concrete Security-principal -> Partner business mapping is not
         * invented in INIT-2. A reviewed Partner/Security linking surface is
         * still required before this adapter can return a resolved Partner.
         */
        return Optional.empty();
    }

    @Override
    public Optional<ResolvedPartnerIdentity> findByPartnerIdentifier(
            String partnerIdentifier
    ) {
        return partnerIdentityQuery
                .findByPartnerIdentifier(partnerIdentifier)
                .map(view -> new ResolvedPartnerIdentity(
                        view.partnerId(),
                        view.partnerIdentifier(),
                        view.acceptsNewTransactions()
                ));
    }
}
