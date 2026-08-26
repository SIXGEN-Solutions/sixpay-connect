package com.sixpay.customer.verification.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Optional;

public enum VerificationFailureCode implements ValueObject {
    CUSTOMER_NOT_FOUND(Category.BUSINESS_REJECTION, VerificationCheckType.CUSTOMER_EXISTS),
    FINANCIAL_INSTITUTION_MISMATCH(Category.BUSINESS_REJECTION, VerificationCheckType.FINANCIAL_INSTITUTION_MATCHES),
    NIU_MISMATCH(Category.BUSINESS_REJECTION, VerificationCheckType.NIU_MATCHES),
    IDENTITY_MISMATCH(Category.BUSINESS_REJECTION, VerificationCheckType.IDENTITY_MATCHES),
    ACCOUNT_NOT_FOUND(Category.BUSINESS_REJECTION, VerificationCheckType.ACCOUNT_EXISTS),
    ACCOUNT_CUSTOMER_MISMATCH(Category.BUSINESS_REJECTION, VerificationCheckType.ACCOUNT_BELONGS_TO_CUSTOMER),
    ACCOUNT_INACTIVE(Category.BUSINESS_REJECTION, VerificationCheckType.ACCOUNT_IS_ACTIVE),
    ACCOUNT_BLOCKED(Category.BUSINESS_REJECTION, VerificationCheckType.ACCOUNT_NOT_BLOCKED),
    ACCOUNT_OPPOSED(Category.BUSINESS_REJECTION, VerificationCheckType.ACCOUNT_NOT_OPPOSED),
    KYC_MISSING(Category.BUSINESS_REJECTION, VerificationCheckType.REQUIRED_KYC_PRESENT),
    KYC_NOT_VERIFIED(Category.BUSINESS_REJECTION, VerificationCheckType.REQUIRED_KYC_VERIFIED),
    BANKING_SYSTEM_UNAVAILABLE(Category.TECHNICAL_INDETERMINATION),
    BANKING_RESPONSE_TIMEOUT(Category.TECHNICAL_INDETERMINATION),
    BANKING_RESPONSE_INCOMPLETE(Category.TECHNICAL_INDETERMINATION),
    BANKING_RESPONSE_INVALID(Category.TECHNICAL_INDETERMINATION),
    CHECK_NOT_SUPPORTED(Category.TECHNICAL_INDETERMINATION),
    EVIDENCE_NOT_FRESH(Category.TECHNICAL_INDETERMINATION),
    TECHNICAL_RESULT_UNKNOWN(Category.TECHNICAL_INDETERMINATION);

    private final Category category;
    private final VerificationCheckType applicableCheckType;

    VerificationFailureCode(Category category) { this(category, null); }
    VerificationFailureCode(Category category, VerificationCheckType applicableCheckType) {
        this.category = category;
        this.applicableCheckType = applicableCheckType;
    }
    public Category category() { return category; }
    public boolean isBusinessRejection() { return category == Category.BUSINESS_REJECTION; }
    public boolean isTechnicalIndetermination() { return category == Category.TECHNICAL_INDETERMINATION; }
    public boolean appliesTo(VerificationCheckType checkType) { return applicableCheckType == null || applicableCheckType == checkType; }
    public Optional<VerificationCheckType> applicableCheckType() { return Optional.ofNullable(applicableCheckType); }

    public enum Category { BUSINESS_REJECTION, TECHNICAL_INDETERMINATION }
}
