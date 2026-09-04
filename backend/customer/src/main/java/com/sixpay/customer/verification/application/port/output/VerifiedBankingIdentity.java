package com.sixpay.customer.verification.application.port.output;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable Customer-owned projection of the canonical banking identity
 * returned by the authoritative Core Banking system after Customer Verification.
 */
public record VerifiedBankingIdentity(
        String customerReference,
        String customerNumber,
        String financialInstitutionCode,
        String niu,
        String legalName,
        String phoneNumber,
        String email,
        String kycStatus,
        List<KycField> kycFields,
        Instant kycLastUpdatedAt,
        Instant retrievedAt
) {

    public VerifiedBankingIdentity {
        customerReference = required(customerReference, "customerReference");
        customerNumber = required(customerNumber, "customerNumber");
        financialInstitutionCode = required(
                financialInstitutionCode,
                "financialInstitutionCode"
        );
        niu = required(niu, "niu");
        legalName = required(legalName, "legalName");
        phoneNumber = required(phoneNumber, "phoneNumber");
        email = required(email, "email");
        kycStatus = required(kycStatus, "kycStatus");
        kycFields = List.copyOf(
                Objects.requireNonNull(kycFields, "kycFields are required")
        );
        retrievedAt = Objects.requireNonNull(
                retrievedAt,
                "retrievedAt is required"
        );
    }

    public Optional<Instant> kycLastUpdatedAtOptional() {
        return Optional.ofNullable(kycLastUpdatedAt);
    }

    @Override
    public String toString() {
        return "VerifiedBankingIdentity[customerReference=[PROTECTED]"
                + ", customerNumber=[PROTECTED]"
                + ", financialInstitutionCode=" + financialInstitutionCode
                + ", niu=[PROTECTED]"
                + ", legalName=[PROTECTED]"
                + ", phoneNumber=[PROTECTED]"
                + ", email=[PROTECTED]"
                + ", kycStatus=" + kycStatus
                + ", kycFields=" + kycFields.stream()
                        .map(KycField::withoutValue)
                        .toList()
                + ", kycLastUpdatedAt=" + kycLastUpdatedAt
                + ", retrievedAt=" + retrievedAt
                + "]";
    }

    public record KycField(
            String code,
            Object value,
            boolean present,
            boolean verified,
            Instant verifiedAt
    ) {
        public KycField {
            code = required(code, "kyc field code");
        }

        KycField withoutValue() {
            return new KycField(
                    code,
                    null,
                    present,
                    verified,
                    verifiedAt
            );
        }

        @Override
        public String toString() {
            return "KycField[code=" + code
                    + ", value=[PROTECTED]"
                    + ", present=" + present
                    + ", verified=" + verified
                    + ", verifiedAt=" + verifiedAt
                    + "]";
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
