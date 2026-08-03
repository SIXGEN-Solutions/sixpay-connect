package com.sixpay.payment.application.port.output.security;

import com.sixpay.payment.application.query.SearchPaymentProjectionsQuery;
import com.sixpay.payment.application.security.PaymentVisibilityScope;
import com.sixpay.payment.application.view.PaymentProjectionViews;
import com.sixpay.payment.domain.model.PaymentId;

import java.util.Optional;

/**
 * Reads masked Payment projections without loading the Payment Aggregate Root.
 */
public interface PaymentProjectionReadPort {

    PaymentProjectionViews.SearchPage search(
            SearchPaymentProjectionsQuery query,
            PaymentVisibilityScope visibility
    );

    Optional<PaymentProjectionViews.Detail> findById(
            PaymentId paymentId
    );
}
