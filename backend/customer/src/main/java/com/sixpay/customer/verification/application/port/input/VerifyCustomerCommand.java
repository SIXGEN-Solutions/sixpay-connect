package com.sixpay.customer.verification.application.port.input;

import com.sixpay.customer.verification.application.port.output.BankingAccountAccessReference;
import com.sixpay.customer.verification.application.port.output.BankingVerificationQuery;
import com.sixpay.customer.verification.domain.model.AccountBindingFingerprint;
import com.sixpay.customer.verification.domain.model.CustomerVerificationContext;
import com.sixpay.customer.verification.domain.model.CustomerVerificationId;
import com.sixpay.customer.verification.domain.model.CustomerVerificationSubject;
import com.sixpay.customer.verification.domain.model.FinancialInstitutionCode;

import java.time.Instant;
import java.util.Objects;

public record VerifyCustomerCommand(
        CustomerVerificationId verificationId,
        CustomerVerificationSubject subject,
        FinancialInstitutionCode financialInstitutionCode,
        AccountBindingFingerprint accountBindingFingerprint,
        BankingAccountAccessReference bankingAccountAccessReference,
        CustomerVerificationContext context,
        Instant requestedAt
) {
    public VerifyCustomerCommand {
        verificationId = Objects.requireNonNull(verificationId, "verificationId is required");
        subject = Objects.requireNonNull(subject, "subject is required");
        financialInstitutionCode = Objects.requireNonNull(financialInstitutionCode, "financialInstitutionCode is required");
        accountBindingFingerprint = Objects.requireNonNull(accountBindingFingerprint, "accountBindingFingerprint is required");
        bankingAccountAccessReference = Objects.requireNonNull(bankingAccountAccessReference, "bankingAccountAccessReference is required");
        context = Objects.requireNonNull(context, "context is required");
        requestedAt = Objects.requireNonNull(requestedAt, "requestedAt is required");
    }

    public BankingVerificationQuery toBankingQuery() {
        return new BankingVerificationQuery(
                verificationId,
                subject,
                financialInstitutionCode,
                accountBindingFingerprint,
                bankingAccountAccessReference,
                context,
                requestedAt
        );
    }

    @Override
    public String toString() {
        return "VerifyCustomerCommand[verificationId=" + verificationId
                + ", subject=[PROTECTED]"
                + ", financialInstitutionCode=" + financialInstitutionCode
                + ", accountBindingFingerprint=[PROTECTED]"
                + ", bankingAccountAccessReference=[PROTECTED]"
                + ", correlationId=" + context.correlationId()
                + ", requestedAt=" + requestedAt + "]";
    }
}
