package com.sixpay.bootstrap.integration.partner;

import com.sixpay.partner.application.port.input.PartnerIdentityQueryUseCase;
import com.sixpay.payment.application.port.output.partner.PartnerIdentityResolutionPort;
import com.sixpay.payment.application.port.output.partner.ResolvedPartnerIdentity;
import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;
import com.sixpay.security.authentication.CurrentMachineIdentityProvider;

import java.util.Objects;
import java.util.Optional;

/**
 * Composition adapter connecting Security machine identity to Partner business
 * identity without leaking Security or Partner infrastructure into Payment.
 */
public final class PaymentPartnerIdentityModuleAdapter
        implements PartnerIdentityResolutionPort {

    private final CurrentMachineIdentityProvider machineIdentityProvider;
    private final PartnerMachineIdentityQueryUseCase machineIdentityQuery;
    private final PartnerIdentityQueryUseCase partnerIdentityQuery;

    public PaymentPartnerIdentityModuleAdapter(
            CurrentMachineIdentityProvider machineIdentityProvider,
            PartnerMachineIdentityQueryUseCase machineIdentityQuery,
            PartnerIdentityQueryUseCase partnerIdentityQuery
    ) {
        this.machineIdentityProvider = Objects.requireNonNull(machineIdentityProvider);
        this.machineIdentityQuery = Objects.requireNonNull(machineIdentityQuery);
        this.partnerIdentityQuery = Objects.requireNonNull(partnerIdentityQuery);
    }

    @Override
    public Optional<ResolvedPartnerIdentity> resolveAuthenticatedPartner(
            String authenticatedSubject
    ) {
        String trustedSubject = machineIdentityProvider
                .requireCurrentMachineIdentity()
                .subject();

        if (authenticatedSubject == null
                || !trustedSubject.equals(authenticatedSubject.strip())) {
            return Optional.empty();
        }

        return machineIdentityQuery
                .findByMachineSubject(trustedSubject)
                .flatMap(link ->
                        partnerIdentityQuery
                                .findByPartnerIdentifier(
                                        link.partnerIdentifier()
                                )
                )
                .map(view -> new ResolvedPartnerIdentity(
                        view.partnerId(),
                        view.partnerIdentifier(),
                        view.acceptsNewTransactions()
                ));
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
