package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.partner.PartnerIdentityResolutionPort;
import com.sixpay.payment.application.port.output.partner.ResolvedPartnerIdentity;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerIdentityAlignmentServiceTest {

    private static final UUID PARTNER_ID =
            UUID.fromString("8ec6a427-406f-4f93-b271-cbc819a4c1dd");

    @Test
    void acceptsWhenAuthenticatedMachinePartnerMatchesDeclaredAppId() {
        var identity = new ResolvedPartnerIdentity(
                PARTNER_ID,
                "PARTNER_001",
                true
        );

        PartnerIdentityResolutionPort port =
                new FixedPort(identity, identity);

        assertThat(
                new PartnerIdentityAlignmentService(port)
                        .requireConsistentPartner(
                                "client-001",
                                "PARTNER_001"
                        )
        ).isEqualTo(identity);
    }

    @Test
    void rejectsWhenDeclaredAppIdBelongsToAnotherPartner() {
        var authenticated = new ResolvedPartnerIdentity(
                PARTNER_ID,
                "PARTNER_001",
                true
        );
        var declared = new ResolvedPartnerIdentity(
                UUID.fromString("d3ac5544-f376-4308-9efc-291027c0ae76"),
                "PARTNER_002",
                true
        );

        assertThatThrownBy(() ->
                new PartnerIdentityAlignmentService(
                        new FixedPort(authenticated, declared)
                ).requireConsistentPartner(
                        "client-001",
                        "PARTNER_002"
                )
        )
                .isInstanceOf(PartnerIdentityResolutionException.class)
                .extracting("failure")
                .isEqualTo(
                        PartnerIdentityResolutionFailure
                                .PARTNER_IDENTITY_MISMATCH
                );
    }

    private record FixedPort(
            ResolvedPartnerIdentity authenticated,
            ResolvedPartnerIdentity declared
    ) implements PartnerIdentityResolutionPort {

        @Override
        public Optional<ResolvedPartnerIdentity> resolveAuthenticatedPartner(
                String authenticatedSubject
        ) {
            return Optional.ofNullable(authenticated);
        }

        @Override
        public Optional<ResolvedPartnerIdentity> findByPartnerIdentifier(
                String partnerIdentifier
        ) {
            return Optional.ofNullable(declared);
        }
    }
}
