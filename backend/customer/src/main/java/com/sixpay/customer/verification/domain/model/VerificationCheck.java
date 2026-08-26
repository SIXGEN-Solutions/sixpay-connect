package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;
import java.util.Optional;

public record VerificationCheck(
        VerificationCheckType type,
        VerificationCheckResult result,
        VerificationFailureCode failureCode
) implements ValueObject {

    public VerificationCheck {
        type = Objects.requireNonNull(type, "type is required");
        result = Objects.requireNonNull(result, "result is required");
        switch (result) {
            case PASS -> requireNoReason(failureCode);
            case FAIL -> requireBusinessReason(type, failureCode);
            case UNKNOWN -> requireOptionalTechnicalReason(failureCode);
        }
    }

    public static VerificationCheck passed(VerificationCheckType type) {
        return new VerificationCheck(type, VerificationCheckResult.PASS, null);
    }
    public static VerificationCheck failed(VerificationCheckType type, VerificationFailureCode code) {
        return new VerificationCheck(type, VerificationCheckResult.FAIL, code);
    }
    public static VerificationCheck unknown(VerificationCheckType type, VerificationFailureCode code) {
        return new VerificationCheck(type, VerificationCheckResult.UNKNOWN, code);
    }
    public static VerificationCheck unknown(VerificationCheckType type) { return unknown(type, null); }
    public Optional<VerificationFailureCode> failureCodeOptional() { return Optional.ofNullable(failureCode); }

    private static void requireNoReason(VerificationFailureCode code) {
        if (code != null) throw new CustomerVerificationDomainException("PASS verification check must not contain a reason code");
    }
    private static void requireBusinessReason(VerificationCheckType type, VerificationFailureCode code) {
        if (code == null || !code.isBusinessRejection())
            throw new CustomerVerificationDomainException("FAIL verification check requires a business rejection code");
        if (!code.appliesTo(type))
            throw new CustomerVerificationDomainException("Business rejection code " + code + " does not apply to check " + type);
    }
    private static void requireOptionalTechnicalReason(VerificationFailureCode code) {
        if (code != null && !code.isTechnicalIndetermination())
            throw new CustomerVerificationDomainException("UNKNOWN verification check accepts only a technical indetermination code");
    }
}
