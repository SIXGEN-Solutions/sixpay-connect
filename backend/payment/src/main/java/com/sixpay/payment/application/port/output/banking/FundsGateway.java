package com.sixpay.payment.application.port.output.banking;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.FundsControlSnapshot;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.util.Objects;

public interface FundsGateway {

    FundsControlSnapshot check(FundsCheckRequest request);

    record FundsCheckRequest(
            PaymentId paymentId,
            BankingRequestContext context,
            String debtorAccountReference,
            Money amount
    ) {
        public FundsCheckRequest {
            paymentId = Objects.requireNonNull(paymentId, "Payment ID");
            context = Objects.requireNonNull(context, "Banking request context");
            if (debtorAccountReference == null
                    || debtorAccountReference.isBlank()
                    || debtorAccountReference.length() > 100) {
                throw new IllegalArgumentException(
                        "Debtor account reference must be non-blank "
                                + "and at most 100 characters"
                );
            }
            amount = Objects.requireNonNull(amount, "Payment amount");
            if (!amount.isPositive()) {
                throw new IllegalArgumentException(
                        "Payment amount must be positive"
                );
            }
        }
    }
}
