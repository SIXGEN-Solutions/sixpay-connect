package com.sixpay.payment.configuration;

import com.sixpay.payment.domain.model.evidence.BankingVerificationCheckType;
import com.sixpay.payment.domain.model.evidence.FundsControlCheckType;
import com.sixpay.payment.domain.policy.EvidenceCategory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMvpPolicyConfigurationTest {

    @Test
    void exposesVersionedMvpReferenceProfiles() {
        var bundle =
                new PaymentMvpPolicyConfiguration()
                        .paymentMvpPolicyBundle();

        assertThat(
                bundle.bankingVerificationPolicyProfile()
                        .metadata()
                        .profileId()
        ).isEqualTo("payment-mvp");

        assertThat(
                bundle.bankingVerificationPolicyProfile()
                        .metadata()
                        .profileVersion()
        ).isEqualTo("v1");

        assertThat(
                bundle.bankingVerificationPolicyProfile()
                        .mandatoryChecks()
        ).containsExactlyInAnyOrder(
                BankingVerificationCheckType.values()
        );

        assertThat(
                bundle.fundsControlPolicyProfile().mandatoryChecks()
        ).isEqualTo(Set.of(FundsControlCheckType.values()));

        assertThat(
                bundle.evidenceTemporalProfile()
                        .maximumAge(EvidenceCategory.BANKING_VERIFICATION)
        ).isEqualTo(Duration.ofMinutes(5));

        assertThat(
                bundle.evidenceTemporalProfile()
                        .maximumAge(EvidenceCategory.FUNDS_CONTROL)
        ).isEqualTo(Duration.ofMinutes(2));

        assertThat(
                bundle.postingAuthorizationPolicyProfile()
                        .requireFreshFundsEvidence()
        ).isTrue();

        assertThat(
                bundle.postingAuthorizationPolicyProfile()
                        .requireResolvedTreasuryAccount()
        ).isTrue();
    }
}
