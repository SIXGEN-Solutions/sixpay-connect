package com.sixpay.payment.configuration;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.evidence.BankingVerificationCheckType;
import com.sixpay.payment.domain.model.evidence.FundsControlCheckType;
import com.sixpay.payment.domain.policy.EvidenceCategory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMvpPolicyConfigurationTest {

    @Test
    void exposesVersionedMvpReferenceProfiles() {
        var bundle =
                new PaymentMvpPolicyConfiguration()
                        .paymentMvpPolicyBundle(
                                new PaymentTreasuryApprovalProperties(Map.of()));

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

        // The same nine baseline checks are mandatory in the Payment
        // transport response; KYC checks are conditional in Customer.
        assertThat(
                bundle.bankingVerificationPolicyProfile()
                        .mandatoryChecks()
        ).containsExactlyInAnyOrder(
                BankingVerificationCheckType.CUSTOMER_EXISTS,
                BankingVerificationCheckType.FINANCIAL_INSTITUTION_MATCHES,
                BankingVerificationCheckType.NIU_MATCHES,
                BankingVerificationCheckType.IDENTITY_MATCHES,
                BankingVerificationCheckType.ACCOUNT_EXISTS,
                BankingVerificationCheckType.ACCOUNT_BELONGS_TO_CUSTOMER,
                BankingVerificationCheckType.ACCOUNT_IS_ACTIVE,
                BankingVerificationCheckType.ACCOUNT_NOT_BLOCKED,
                BankingVerificationCheckType.ACCOUNT_NOT_OPPOSED
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
    @Test
    void treasuryPolicyDeniesEveryInstitutionWithoutExplicitDeploymentApproval() {
        var bundle = new PaymentMvpPolicyConfiguration().paymentMvpPolicyBundle(
                new PaymentTreasuryApprovalProperties(Map.of()));
        assertThat(bundle.treasuryResolutionPolicyProfile()
                .isApproved(FinancialInstitutionCode.of("REGIONAL"), "v1"))
                .isFalse();
    }

    @Test
    void treasuryPolicyApprovesOnlyConfiguredInstitutionAndVersion() {
        var bundle = new PaymentMvpPolicyConfiguration().paymentMvpPolicyBundle(
                new PaymentTreasuryApprovalProperties(Map.of("REGIONAL", Set.of("v1"))));
        assertThat(bundle.treasuryResolutionPolicyProfile()
                .isApproved(FinancialInstitutionCode.of("REGIONAL"), "v1"))
                .isTrue();
        assertThat(bundle.treasuryResolutionPolicyProfile()
                .isApproved(FinancialInstitutionCode.of("OTHER_BANK"), "v1"))
                .isFalse();
        assertThat(bundle.treasuryResolutionPolicyProfile()
                .isApproved(FinancialInstitutionCode.of("REGIONAL"), "v2"))
                .isFalse();
    }
}
