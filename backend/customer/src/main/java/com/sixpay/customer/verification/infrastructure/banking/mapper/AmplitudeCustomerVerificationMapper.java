package com.sixpay.customer.verification.infrastructure.banking.mapper;

import com.sixpay.customer.verification.application.port.output.BankingVerificationQuery;
import com.sixpay.customer.verification.application.port.output.BankingVerificationResponse;
import com.sixpay.customer.verification.application.port.output.VerifiedBankingAccount;
import com.sixpay.customer.verification.application.port.output.VerifiedBankingIdentity;
import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationCheckResult;
import com.sixpay.customer.verification.domain.model.VerificationCheckType;
import com.sixpay.customer.verification.domain.model.VerificationEvidenceFingerprint;
import com.sixpay.customer.verification.domain.model.VerificationFailureCode;
import com.sixpay.customer.verification.infrastructure.banking.dto.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class AmplitudeCustomerVerificationMapper {

    private static final List<String> REQUIRED_KYC_FIELDS = List.of(
            "niu",
            "legalName",
            "phoneNumber",
            "email"
    );

    private final Duration defaultEvidenceTtl;

    public AmplitudeCustomerVerificationMapper(
            Duration defaultEvidenceTtl
    ) {
        if (defaultEvidenceTtl == null
                || defaultEvidenceTtl.isZero()
                || defaultEvidenceTtl.isNegative()) {
            throw new IllegalArgumentException(
                    "defaultEvidenceTtl must be positive"
            );
        }
        this.defaultEvidenceTtl = defaultEvidenceTtl;
    }

    public AmplitudeCustomerVerificationRequest toExternalRequest(
            BankingVerificationQuery query
    ) {
        Objects.requireNonNull(query, "query is required");

        return new AmplitudeCustomerVerificationRequest(
                query.financialInstitutionCode().value(),
                new AmplitudeCustomerVerificationSubject(
                        null,
                        null,
                        query.subject().identity().niu().value(),
                        query.subject().identity().legalName(),
                        null,
                        null
                ),
                new AmplitudeAccountVerificationSubject(
                        query.bankingAccountAccessReference().value(),
                        null,
                        null
                ),
                REQUIRED_KYC_FIELDS,
                query.requestedAt()
        );
    }

    public BankingVerificationResponse toInternalResponse(
            AmplitudeCustomerVerificationResponse response
    ) {
        Objects.requireNonNull(response, "response is required");

        Instant observedAt = Objects.requireNonNull(
                response.verifiedAt(),
                "Amplitude verifiedAt is required"
        );

        List<AmplitudeVerificationCheckResponse> externalChecks =
                List.copyOf(
                        Objects.requireNonNull(
                                response.checks(),
                                "Amplitude checks are required"
                        )
                );

        List<VerificationCheck> checks =
                externalChecks.stream()
                        .map(this::toInternalCheck)
                        .sorted(
                                Comparator.comparingInt(
                                        check -> check.type().ordinal()
                                )
                        )
                        .toList();

        Instant validUntil =
                observedAt.plus(defaultEvidenceTtl);

        return BankingVerificationResponse.of(
                checks,
                fingerprint(response, externalChecks),
                observedAt,
                validUntil,
                response.customerReference(),
                response.accountReference(),
                toInternalIdentity(response.identity()),
                toInternalAccount(response.account())
        );
    }

    private VerificationCheck toInternalCheck(
            AmplitudeVerificationCheckResponse external
    ) {
        VerificationCheckType type;
        VerificationCheckResult result;

        try {
            type = VerificationCheckType.valueOf(
                    required(external.type(), "check type")
            );
            result = VerificationCheckResult.valueOf(
                    required(external.result(), "check result")
            );
        } catch (IllegalArgumentException exception) {
            throw new CustomerVerificationDomainException(
                    "Unsupported Amplitude verification check"
            );
        }

        return switch (result) {
            case PASS -> VerificationCheck.passed(type);
            case FAIL -> VerificationCheck.failed(
                    type,
                    businessFailureCode(type)
            );
            case UNKNOWN -> VerificationCheck.unknown(
                    type,
                    VerificationFailureCode.TECHNICAL_RESULT_UNKNOWN
            );
        };
    }

    private static VerifiedBankingIdentity toInternalIdentity(
            AmplitudeCustomerIdentityResponse identity
    ) {
        if (identity == null) {
            return null;
        }

        List<VerifiedBankingIdentity.KycField> kycFields =
                identity.kycFields() == null
                        ? List.of()
                        : identity.kycFields().stream()
                        .map(field ->
                                new VerifiedBankingIdentity.KycField(
                                        field.code(),
                                        field.value(),
                                        Boolean.TRUE.equals(field.present()),
                                        Boolean.TRUE.equals(field.verified()),
                                        field.verifiedAt()
                                )
                        )
                        .toList();

        return new VerifiedBankingIdentity(
                identity.customerReference(),
                identity.customerNumber(),
                identity.financialInstitutionCode(),
                identity.niu(),
                identity.legalName(),
                identity.phoneNumber(),
                identity.email(),
                identity.kycStatus(),
                kycFields,
                identity.kycLastUpdatedAt(),
                identity.retrievedAt()
        );
    }

    private static VerifiedBankingAccount toInternalAccount(
            AmplitudeBankAccountResponse account
    ) {
        if (account == null) {
            return null;
        }

        return new VerifiedBankingAccount(
                account.accountReference(),
                account.customerReference(),
                account.financialInstitutionCode(),
                account.maskedAccountIdentifier(),
                account.currency(),
                account.accountType(),
                account.status(),
                account.restrictions() == null
                        ? List.of()
                        : account.restrictions(),
                account.retrievedAt()
        );
    }

    private static VerificationFailureCode businessFailureCode(
            VerificationCheckType type
    ) {
        return switch (type) {
            case CUSTOMER_EXISTS ->
                    VerificationFailureCode.CUSTOMER_NOT_FOUND;
            case FINANCIAL_INSTITUTION_MATCHES ->
                    VerificationFailureCode.FINANCIAL_INSTITUTION_MISMATCH;
            case NIU_MATCHES ->
                    VerificationFailureCode.NIU_MISMATCH;
            case IDENTITY_MATCHES ->
                    VerificationFailureCode.IDENTITY_MISMATCH;
            case ACCOUNT_EXISTS ->
                    VerificationFailureCode.ACCOUNT_NOT_FOUND;
            case ACCOUNT_BELONGS_TO_CUSTOMER ->
                    VerificationFailureCode.ACCOUNT_CUSTOMER_MISMATCH;
            case ACCOUNT_IS_ACTIVE ->
                    VerificationFailureCode.ACCOUNT_INACTIVE;
            case ACCOUNT_NOT_BLOCKED ->
                    VerificationFailureCode.ACCOUNT_BLOCKED;
            case ACCOUNT_NOT_OPPOSED ->
                    VerificationFailureCode.ACCOUNT_OPPOSED;
            case REQUIRED_KYC_PRESENT ->
                    VerificationFailureCode.KYC_MISSING;
            case REQUIRED_KYC_VERIFIED ->
                    VerificationFailureCode.KYC_NOT_VERIFIED;
        };
    }

    private static VerificationEvidenceFingerprint fingerprint(
            AmplitudeCustomerVerificationResponse response,
            List<AmplitudeVerificationCheckResponse> checks
    ) {
        String canonical = response.verificationId()
                + "|" + response.verifiedAt()
                + "|" + response.source()
                + "|" + response.outcome()
                + "|" + response.customerReference()
                + "|" + response.accountReference()
                + "|" + canonicalIdentity(response.identity())
                + "|" + canonicalAccount(response.account())
                + "|"
                + checks.stream()
                        .sorted(
                                Comparator.comparing(
                                        AmplitudeVerificationCheckResponse::type
                                )
                        )
                        .map(
                                check -> check.type()
                                        + "="
                                        + check.result()
                                        + ":"
                                        + check.reasonCode()
                                        + ":"
                                        + check.checkedAt()
                        )
                        .reduce(
                                "",
                                (left, right) ->
                                        left.isEmpty()
                                                ? right
                                                : left + "," + right
                        );

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");
            String hash = HexFormat.of().formatHex(
                    digest.digest(
                            canonical.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    )
            );
            return VerificationEvidenceFingerprint.of(
                    "v1:sha256:" + hash
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }

    private static String canonicalIdentity(
            AmplitudeCustomerIdentityResponse identity
    ) {
        if (identity == null) {
            return "null";
        }

        return identity.customerReference()
                + "|" + identity.customerNumber()
                + "|" + identity.financialInstitutionCode()
                + "|" + identity.niu()
                + "|" + identity.legalName()
                + "|" + identity.phoneNumber()
                + "|" + identity.email()
                + "|" + identity.kycStatus()
                + "|" + identity.kycLastUpdatedAt()
                + "|" + identity.retrievedAt()
                + "|"
                + (identity.kycFields() == null
                ? ""
                : identity.kycFields().stream()
                        .sorted(
                                Comparator.comparing(
                                        AmplitudeKycFieldResponse::code
                                )
                        )
                        .map(field ->
                                field.code()
                                        + ":" + field.present()
                                        + ":" + field.verified()
                                        + ":" + field.verifiedAt()
                        )
                        .reduce(
                                "",
                                (left, right) ->
                                        left.isEmpty()
                                                ? right
                                                : left + "," + right
                        ));
    }

    private static String canonicalAccount(
            AmplitudeBankAccountResponse account
    ) {
        if (account == null) {
            return "null";
        }

        return account.accountReference()
                + "|" + account.customerReference()
                + "|" + account.financialInstitutionCode()
                + "|" + account.maskedAccountIdentifier()
                + "|" + account.currency()
                + "|" + account.accountType()
                + "|" + account.status()
                + "|" + account.retrievedAt()
                + "|"
                + (account.restrictions() == null
                ? ""
                : account.restrictions().stream()
                        .sorted()
                        .reduce(
                                "",
                                (left, right) ->
                                        left.isEmpty()
                                                ? right
                                                : left + "," + right
                        ));
    }

    private static String required(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new CustomerVerificationDomainException(
                    name + " is required"
            );
        }
        return value.strip().toUpperCase(java.util.Locale.ROOT);
    }
}
