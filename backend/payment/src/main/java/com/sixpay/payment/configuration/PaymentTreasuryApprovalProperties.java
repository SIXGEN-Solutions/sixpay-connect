package com.sixpay.payment.configuration;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deployment-owned allowlist. Missing configuration means no approved institution.
 * Account references and an institution's presence in Treasury accounts do NOT
 * grant approval. No institution or policy version is hardcoded in Java.
 */
@ConfigurationProperties(prefix = "sixpay.payment.treasury.approval")
public record PaymentTreasuryApprovalProperties(
        Map<String, Set<String>> approvedVersionsByInstitution
) {
    public PaymentTreasuryApprovalProperties {
        approvedVersionsByInstitution = approvedVersionsByInstitution == null
                ? Map.of()
                : Map.copyOf(approvedVersionsByInstitution);
    }

    public Map<FinancialInstitutionCode, Set<String>> approvedVersions() {
        return approvedVersionsByInstitution.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        entry -> FinancialInstitutionCode.of(entry.getKey()),
                        entry -> {
                            Set<String> versions = Objects.requireNonNull(
                                    entry.getValue(), "Approved versions");
                            if (versions.isEmpty() || versions.stream()
                                    .anyMatch(v -> v == null || v.isBlank())) {
                                throw new IllegalArgumentException(
                                        "Approved Treasury versions must be nonempty");
                            }
                            return Set.copyOf(versions);
                        }
                ));
    }
}
