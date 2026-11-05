package com.sixpay.payment.configuration;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTreasuryApprovalPropertiesTest {
    @Test
    void missingConfigurationApprovesNobody() {
        assertThat(new PaymentTreasuryApprovalProperties(null).approvedVersions())
                .isEmpty();
    }

    @Test
    void mapsAnyConfiguredInstitutionWithoutJavaAllowlist() {
        var props = new PaymentTreasuryApprovalProperties(
                Map.of("ANOTHER_BANK", Set.of("v7")));
        assertThat(props.approvedVersions())
                .containsEntry(FinancialInstitutionCode.of("ANOTHER_BANK"), Set.of("v7"));
    }

    @Test
    void blankVersionsAreRejectedRatherThanImplicitlyApproved() {
        var props = new PaymentTreasuryApprovalProperties(Map.of("REGIONAL", Set.of("")));
        assertThatThrownBy(props::approvedVersions)
                .isInstanceOf(IllegalArgumentException.class);
    }
}
