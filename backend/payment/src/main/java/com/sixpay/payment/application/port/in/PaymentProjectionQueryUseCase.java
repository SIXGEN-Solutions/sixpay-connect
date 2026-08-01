package com.sixpay.payment.application.port.in;

import com.sixpay.payment.application.query.SearchPaymentProjectionsQuery;
import com.sixpay.payment.application.view.PaymentProjectionViews;

import java.util.Optional;
import java.util.UUID;

public interface PaymentProjectionQueryUseCase {
    PaymentProjectionViews.SearchPage search(SearchPaymentProjectionsQuery query);
    Optional<PaymentProjectionViews.Detail> findById(UUID paymentId);
}
