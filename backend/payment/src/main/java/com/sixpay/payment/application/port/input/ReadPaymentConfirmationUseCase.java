package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.query.ReadPaymentConfirmationQuery;
import com.sixpay.payment.application.view.PaymentConfirmationView;

public interface ReadPaymentConfirmationUseCase {

    PaymentConfirmationView read(
            ReadPaymentConfirmationQuery query
    );
}
