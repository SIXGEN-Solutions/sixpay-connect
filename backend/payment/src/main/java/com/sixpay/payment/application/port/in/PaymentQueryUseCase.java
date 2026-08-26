package com.sixpay.payment.application.port.in;

import com.sixpay.payment.application.query.GetPaymentByExternalReferenceQuery;
import com.sixpay.payment.application.query.GetPaymentByIdQuery;
import com.sixpay.payment.application.query.GetPaymentByPublicReferenceQuery;
import com.sixpay.payment.application.view.PaymentView;

import java.util.Optional;

public interface PaymentQueryUseCase {

    Optional<PaymentView> findById(
            GetPaymentByIdQuery query
    );

    Optional<PaymentView> findByPublicReference(
            GetPaymentByPublicReferenceQuery query
    );

    Optional<PaymentView> findByExternalReference(
            GetPaymentByExternalReferenceQuery query
    );
}
