package com.sixpay.payment.infrastructure.customer.mapper;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.port.output.CustomerVerificationEvidenceMapper;
import com.sixpay.payment.application.port.output.CustomerVerificationResponse;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.evidence.BankingVerificationCheckEvidence;
import com.sixpay.payment.domain.model.evidence.BankingVerificationCheckType;
import com.sixpay.payment.domain.model.evidence.BankingVerificationId;
import com.sixpay.payment.domain.model.evidence.BankingVerificationOutcome;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import com.sixpay.payment.domain.model.evidence.EvidenceCheckResult;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.payment.domain.model.evidence.EvidenceMetadata;
import com.sixpay.payment.domain.model.evidence.EvidenceObservationChannel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
public final class CustomerVerificationPaymentMapper
        implements CustomerVerificationEvidenceMapper {

    @Override
    public BankingVerificationSnapshot toSnapshot(
            CustomerVerificationResponse response,
            CorrelationId correlationId,
            Instant acceptedAt
    ) {
        Objects.requireNonNull(response, "response is required");
        Objects.requireNonNull(correlationId, "correlationId is required");
        Objects.requireNonNull(acceptedAt, "acceptedAt is required");

        return new BankingVerificationSnapshot(
                new BankingVerificationId(response.verificationId()),
                mapOutcome(response.outcome()),
                response.accountBindingFingerprint(),
                response.checks().stream()
                        .map(check ->
                                mapCheck(check, response.observedAt())
                        )
                        .toList(),
                new EvidenceMetadata(
                        ExternalSystem.AMPLITUDE,
                        correlationId,
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        EvidenceFingerprint.of(
                                response.evidenceFingerprint()
                        ),
                        response.observedAt(),
                        acceptedAt
                )
        );
    }

    static BankingVerificationOutcome mapOutcome(
            CustomerVerificationResponse.Outcome outcome
    ) {
        Objects.requireNonNull(outcome, "outcome is required");

        return switch (outcome) {
            case VERIFIED -> BankingVerificationOutcome.VERIFIED;
            case REJECTED -> BankingVerificationOutcome.REJECTED;
            case INDETERMINATE ->
                    BankingVerificationOutcome.INDETERMINATE;
        };
    }

    static BankingVerificationCheckEvidence mapCheck(
            CustomerVerificationResponse.Check check,
            Instant checkedAt
    ) {
        Objects.requireNonNull(check, "check is required");
        Objects.requireNonNull(checkedAt, "checkedAt is required");

        return new BankingVerificationCheckEvidence(
                mapCheckType(check.type()),
                mapCheckResult(check.result()),
                mapFailureCode(check.result(), check.failureCode()),
                checkedAt
        );
    }

    static BankingVerificationCheckType mapCheckType(
            CustomerVerificationResponse.CheckType type
    ) {
        Objects.requireNonNull(type, "check type is required");

        return switch (type) {
            case CUSTOMER_EXISTS ->
                    BankingVerificationCheckType.CUSTOMER_EXISTS;
            case FINANCIAL_INSTITUTION_MATCHES ->
                    BankingVerificationCheckType
                            .FINANCIAL_INSTITUTION_MATCHES;
            case NIU_MATCHES ->
                    BankingVerificationCheckType.NIU_MATCHES;
            case IDENTITY_MATCHES ->
                    BankingVerificationCheckType.IDENTITY_MATCHES;
            case ACCOUNT_EXISTS ->
                    BankingVerificationCheckType.ACCOUNT_EXISTS;
            case ACCOUNT_BELONGS_TO_CUSTOMER ->
                    BankingVerificationCheckType
                            .ACCOUNT_BELONGS_TO_CUSTOMER;
            case ACCOUNT_IS_ACTIVE ->
                    BankingVerificationCheckType.ACCOUNT_IS_ACTIVE;
            case ACCOUNT_NOT_BLOCKED ->
                    BankingVerificationCheckType.ACCOUNT_NOT_BLOCKED;
            case ACCOUNT_NOT_OPPOSED ->
                    BankingVerificationCheckType.ACCOUNT_NOT_OPPOSED;
            case REQUIRED_KYC_PRESENT ->
                    BankingVerificationCheckType.REQUIRED_KYC_PRESENT;
            case REQUIRED_KYC_VERIFIED ->
                    BankingVerificationCheckType.REQUIRED_KYC_VERIFIED;
        };
    }

    static EvidenceCheckResult mapCheckResult(
            CustomerVerificationResponse.CheckResult result
    ) {
        Objects.requireNonNull(result, "check result is required");

        return switch (result) {
            case PASS -> EvidenceCheckResult.PASS;
            case FAIL -> EvidenceCheckResult.FAIL;
            case UNKNOWN -> EvidenceCheckResult.UNKNOWN;
        };
    }

    private static FailureCode mapFailureCode(
            CustomerVerificationResponse.CheckResult result,
            String failureCode
    ) {
        if (result == CustomerVerificationResponse.CheckResult.PASS) {
            return null;
        }
        if (failureCode == null || failureCode.isBlank()) {
            return null;
        }
        return FailureCode.of(failureCode);
    }
}
