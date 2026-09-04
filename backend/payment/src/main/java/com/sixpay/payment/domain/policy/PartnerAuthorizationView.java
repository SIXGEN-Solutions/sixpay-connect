package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.ClaimType;

import java.util.Objects;
import java.util.Set;

public record PartnerAuthorizationView(
        boolean active,
        Set<ClaimType> authorizedClaimTypes
) {
    public PartnerAuthorizationView {
        authorizedClaimTypes = Set.copyOf(
                Objects.requireNonNull(
                        authorizedClaimTypes,
                        "Authorized claim types"
                )
        );
    }

    public boolean authorizes(ClaimType claimType) {
        return authorizedClaimTypes.contains(
                Objects.requireNonNull(claimType, "Claim type")
        );
    }
}
