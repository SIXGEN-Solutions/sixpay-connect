package com.sixpay.bootstrap.integration.partner;

import com.sixpay.partner.application.port.input.PartnerIdentityQueryUseCase;
import com.sixpay.partner.application.port.input.PartnerQueryUseCase;
import com.sixpay.partner.domain.model.PartnerId;
import com.sixpay.partner.domain.model.PartnerStatus;
import com.sixpay.payment.application.port.PartnerAuthorizationPort;
import com.sixpay.payment.domain.model.ClaimType;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.policy.PartnerAuthorizationView;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Composition adapter: business permissions remain owned by Partner. */
public final class PaymentPartnerAuthorizationModuleAdapter implements PartnerAuthorizationPort {
    private final PartnerIdentityQueryUseCase identities;
    private final PartnerQueryUseCase partners;

    public PaymentPartnerAuthorizationModuleAdapter(PartnerIdentityQueryUseCase identities,
                                                     PartnerQueryUseCase partners) {
        this.identities = Objects.requireNonNull(identities);
        this.partners = Objects.requireNonNull(partners);
    }

    @Override
    public PartnerAuthorizationView resolve(PaymentInitiationContext context) {
        if (context == null || context.applicationId() == null
                || context.applicationId().isBlank()) return null;
        return identities.findByPartnerIdentifier(context.applicationId())
                .map(identity -> {
                    var partner = partners.findById(new PartnerId(identity.partnerId()));
                    if (!identity.partnerIdentifier().equals(partner.partnerIdentifier())) {
                        throw new IllegalStateException("Inconsistent Partner identity");
                    }
                    Set<ClaimType> allowed = Arrays.stream(ClaimType.values())
                            .filter(type -> partner.authorizedTransactionTypes().contains(type.name()))
                            .collect(Collectors.toUnmodifiableSet());
                    return new PartnerAuthorizationView(partner.status() == PartnerStatus.ACTIVE, allowed);
                }).orElse(null);
    }
}
