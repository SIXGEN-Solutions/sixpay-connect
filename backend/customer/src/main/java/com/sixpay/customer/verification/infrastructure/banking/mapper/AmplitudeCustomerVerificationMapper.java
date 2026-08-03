package com.sixpay.customer.verification.infrastructure.banking.mapper;

import com.sixpay.customer.verification.application.port.out.BankingVerificationQuery;
import com.sixpay.customer.verification.application.port.out.BankingVerificationResponse;
import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationCheckResult;
import com.sixpay.customer.verification.domain.model.VerificationCheckType;
import com.sixpay.customer.verification.domain.model.VerificationEvidenceFingerprint;
import com.sixpay.customer.verification.domain.model.VerificationFailureCode;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationRequest;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationResponse;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeVerificationCheckResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AmplitudeCustomerVerificationMapper {

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
                query.bankingAccountAccessReference().value(),
                query.subject().identity().niu().value(),
                query.subject().identity().legalName(),
                query.financialInstitutionCode().value()
        );
    }

    public BankingVerificationResponse toInternalResponse(
            AmplitudeCustomerVerificationResponse response
    ) {
        Objects.requireNonNull(response, "response is required");

        Instant observedAt = Objects.requireNonNull(
                response.observedAt(),
                "Amplitude observedAt is required"
        );

        List<AmplitudeVerificationCheckResponse> externalChecks =
                normalizeChecks(response.checks());

        List<VerificationCheck> checks =
                externalChecks.stream()
                        .map(this::toInternalCheck)
                        .sorted(
                                Comparator.comparingInt(
                                        check -> check.type().ordinal()
                                )
                        )
                        .toList();

        Instant validUntil = response.validUntil() != null
                ? response.validUntil()
                : observedAt.plus(defaultEvidenceTtl);

        return BankingVerificationResponse.of(
                checks,
                fingerprint(response, externalChecks),
                observedAt,
                validUntil
        );
    }

    private List<AmplitudeVerificationCheckResponse> normalizeChecks(
            Map<String, String> rawChecks
    ) {
        if (rawChecks == null) {
            throw new CustomerVerificationDomainException(
                    "Amplitude checks are required"
            );
        }

        return rawChecks.entrySet().stream()
                .map(
                        entry ->
                                new AmplitudeVerificationCheckResponse(
                                        entry.getKey(),
                                        entry.getValue()
                                )
                )
                .toList();
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
        String canonical = response.code()
                + "|" + response.accountFound()
                + "|" + response.accountStatus()
                + "|" + response.accountHolder()
                + "|" + response.accountReferenceMasked()
                + "|" + response.currency()
                + "|" + response.availableBalance()
                + "|" + response.accountBalance()
                + "|" + response.canDebit()
                + "|" + response.result()
                + "|" + response.observedAt()
                + "|" + response.validUntil()
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

    private static String required(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new CustomerVerificationDomainException(
                    name + " is required"
            );
        }
        return value.strip();
    }
}
