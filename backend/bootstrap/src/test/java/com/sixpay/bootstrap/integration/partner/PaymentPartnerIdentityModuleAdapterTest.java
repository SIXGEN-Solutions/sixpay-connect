package com.sixpay.bootstrap.integration.partner;

import com.sixpay.partner.application.port.input.PartnerIdentityQueryUseCase;
import com.sixpay.partner.application.view.PartnerIdentityView;
import com.sixpay.partner.domain.model.PartnerStatus;
import com.sixpay.security.application.model.PartnerMachineIdentityView;
import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;
import com.sixpay.security.authentication.AuthenticatedMachineIdentity;
import com.sixpay.security.authentication.CurrentMachineIdentityProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPartnerIdentityModuleAdapterTest {

    private static final UUID PARTNER_ID =
            UUID.fromString("8ec6a427-406f-4f93-b271-cbc819a4c1dd");

    @Test
    void resolvesAuthenticatedMachineToRegisteredActivePartner() {
        CurrentMachineIdentityProvider current =
                () -> Optional.of(
                        new AuthenticatedMachineIdentity("client-001")
                );

        PartnerMachineIdentityQueryUseCase machineQuery =
                subject -> Optional.of(
                        new PartnerMachineIdentityView(
                                subject,
                                "PARTNER_001"
                        )
                );

        PartnerIdentityQueryUseCase partnerQuery =
                identifier -> Optional.of(
                        new PartnerIdentityView(
                                PARTNER_ID,
                                identifier,
                                PartnerStatus.ACTIVE
                        )
                );

        var adapter = new PaymentPartnerIdentityModuleAdapter(
                current,
                machineQuery,
                partnerQuery
        );

        assertThat(adapter.resolveAuthenticatedPartner("client-001"))
                .get()
                .satisfies(identity -> {
                    assertThat(identity.partnerId()).isEqualTo(PARTNER_ID);
                    assertThat(identity.partnerIdentifier())
                            .isEqualTo("PARTNER_001");
                    assertThat(identity.acceptsNewTransactions()).isTrue();
                });
    }

    @Test
    void rejectsSubjectDifferentFromTrustedMachineIdentity() {
        var adapter = new PaymentPartnerIdentityModuleAdapter(
                () -> Optional.of(
                        new AuthenticatedMachineIdentity("client-001")
                ),
                subject -> Optional.of(
                        new PartnerMachineIdentityView(subject, "PARTNER_001")
                ),
                identifier -> Optional.of(
                        new PartnerIdentityView(
                                PARTNER_ID,
                                identifier,
                                PartnerStatus.ACTIVE
                        )
                )
        );

        assertThat(
                adapter.resolveAuthenticatedPartner("client-other")
        ).isEmpty();
    }
}
