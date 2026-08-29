package com.sixpay.bootstrap.integration.customer;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.verification.application.exception.BankingVerificationTimeoutException;
import com.sixpay.customer.verification.application.exception.BankingVerificationUnavailableException;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerCommand;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerResult;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.customer.verification.application.port.output.BankingAccountAccessReference;
import com.sixpay.customer.verification.domain.model.AccountBindingFingerprint;
import com.sixpay.customer.verification.domain.model.CustomerIdentity;
import com.sixpay.customer.verification.domain.model.CustomerNiu;
import com.sixpay.customer.verification.domain.model.CustomerVerificationContext;
import com.sixpay.customer.verification.domain.model.CustomerVerificationId;
import com.sixpay.customer.verification.domain.model.CustomerVerificationSubject;
import com.sixpay.customer.verification.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.application.port.output.CustomerVerificationPort;
import com.sixpay.payment.application.port.output.CustomerVerificationRequest;
import com.sixpay.payment.application.port.output.CustomerVerificationResponse;
import com.sixpay.payment.application.port.output.CustomerVerificationTechnicalException;

import java.util.Objects;

/**
 * Composition-layer adapter between Payment and Customer Verification.
 */
public final class CustomerVerificationModuleAdapter
        implements CustomerVerificationPort {

    private final VerifyCustomerUseCase verifyCustomerUseCase;

    public CustomerVerificationModuleAdapter(
            VerifyCustomerUseCase verifyCustomerUseCase
    ) {
        this.verifyCustomerUseCase = Objects.requireNonNull(
                verifyCustomerUseCase,
                "verifyCustomerUseCase is required"
        );
    }

    @Override
    public CustomerVerificationResponse verify(
            CustomerVerificationRequest request
    ) {
        Objects.requireNonNull(request, "request is required");

        try {
            VerifyCustomerResult result = verifyCustomerUseCase.verify(
                    toCustomerCommand(request)
            );
            return toPaymentResponse(result);
        } catch (BankingVerificationTimeoutException exception) {
            throw technicalFailure(
                    request,
                    CustomerVerificationTechnicalException.ErrorType.TIMEOUT,
                    "Customer verification timed output",
                    exception
            );
        } catch (BankingVerificationUnavailableException exception) {
            throw technicalFailure(
                    request,
                    CustomerVerificationTechnicalException.ErrorType.UNAVAILABLE,
                    "Customer verification is unavailable",
                    exception
            );
        }
    }

    private static CustomerVerificationTechnicalException technicalFailure(
            CustomerVerificationRequest request,
            CustomerVerificationTechnicalException.ErrorType errorType,
            String message,
            Throwable cause
    ) {
        return new CustomerVerificationTechnicalException(
                request.verificationId(),
                errorType,
                message,
                cause
        );
    }

    static VerifyCustomerCommand toCustomerCommand(
            CustomerVerificationRequest request
    ) {
        return new VerifyCustomerCommand(
                new CustomerVerificationId(request.verificationId()),
                CustomerVerificationSubject.of(
                        CustomerIdentity.of(
                                CustomerNiu.of(request.customerNiu()),
                                request.customerLegalName()
                        )
                ),
                FinancialInstitutionCode.of(
                        request.financialInstitutionCode()
                ),
                AccountBindingFingerprint.of(
                        request.accountBindingFingerprint()
                ),
                BankingAccountAccessReference.of(
                        request.integrationAccountToken()
                ),
                CustomerVerificationContext.of(
                        CorrelationId.of(request.correlationId()),
                        request.causationId()
                ),
                request.requestedAt()
        );
    }

    static CustomerVerificationResponse toPaymentResponse(
            VerifyCustomerResult result
    ) {
        return new CustomerVerificationResponse(
                result.verificationId().value(),
                CustomerVerificationResponse.Outcome.valueOf(
                        result.outcome().name()
                ),
                result.checks().stream()
                        .map(check ->
                                new CustomerVerificationResponse.Check(
                                        CustomerVerificationResponse.CheckType
                                                .valueOf(check.type().name()),
                                        CustomerVerificationResponse.CheckResult
                                                .valueOf(check.result().name()),
                                        check.failureCodeOptional()
                                                .map(Enum::name)
                                                .orElse(null)
                                )
                        )
                        .toList(),
                result.evidenceFingerprint().value(),
                result.accountBindingFingerprint().value(),
                result.observedAt(),
                result.validUntil(),
                result.completedAt()
        );
    }
}
