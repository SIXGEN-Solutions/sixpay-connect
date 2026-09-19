package com.sixpay.partner.application.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerIdentityTest {
    @Test
    void parsesCanonicalPartnerUuid() {
        var id = PartnerIdentity.from("11111111-2222-3333-4444-555555555555");
        assertThat(id.toString()).isEqualTo("11111111-2222-3333-4444-555555555555");
    }

    @Test
    void rejectsProviderLoginAsCanonicalIdentity() {
        assertThatThrownBy(() -> PartnerIdentity.from("TRESOR_PAY"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
