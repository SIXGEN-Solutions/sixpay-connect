package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.partner.PartnerIdentityResolutionPort;
import com.sixpay.payment.application.port.output.partner.ResolvedPartnerIdentity;

import java.util.Objects;

/**
 * Aligns trusted Security identity with the Partner business identity declared
 * by the Payment request.
 */
public final class PartnerIdentityAlignmentService {

    private final PartnerIdentityResolutionPort resolutionPort;

    public PartnerIdentityAlignmentService(
            PartnerIdentityResolutionPort resolutionPort
    ) {
        this.resolutionPort = Objects.requireNonNull(
                resolutionPort,
                "Partner identity resolution port"
        );
    }

    public ResolvedPartnerIdentity requireConsistentPartner(
            String authenticatedSubject,
            String declaredPartnerIdentifier
    ) {
        String subject = requireText(
                authenticatedSubject,
                "authenticatedSubject"
        );
        String declared = requireText(
                declaredPartnerIdentifier,
                "declaredPartnerIdentifier"
        );

        ResolvedPartnerIdentity authenticated =
                resolutionPort.resolveAuthenticatedPartner(subject)
                        .orElseThrow(() ->
                                new PartnerIdentityResolutionException(
                                        PartnerIdentityResolutionFailure
                                                .AUTHENTICATED_PARTNER_NOT_RESOLVED,
                                        "Authenticated principal is not linked "
                                                + "to a Partner"
                                )
                        );

        if (!authenticated.acceptsNewTransactions()) {
            throw new PartnerIdentityResolutionException(
                    PartnerIdentityResolutionFailure
                            .PARTNER_NOT_AUTHORIZED,
                    "Authenticated Partner is not authorized "
                            + "for new transactions"
            );
        }

        ResolvedPartnerIdentity declaredIdentity =
                resolutionPort.findByPartnerIdentifier(declared)
                        .orElseThrow(() ->
                                new PartnerIdentityResolutionException(
                                        PartnerIdentityResolutionFailure
                                                .DECLARED_PARTNER_NOT_FOUND,
                                        "Declared Partner identifier "
                                                + "is not registered"
                                )
                        );

        if (!authenticated.partnerId()
                .equals(declaredIdentity.partnerId())) {
            throw new PartnerIdentityResolutionException(
                    PartnerIdentityResolutionFailure
                            .PARTNER_IDENTITY_MISMATCH,
                    "Declared Partner does not match "
                            + "authenticated Partner"
            );
        }

        return authenticated;
    }

    private static String requireText(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required"
            );
        }
        return value.strip();
    }
}
