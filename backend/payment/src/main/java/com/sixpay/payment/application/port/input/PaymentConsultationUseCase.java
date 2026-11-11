package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.view.PaymentConsultationView;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Optional;

/**
 * Read-only partner Payment consultation use case.
 */
public interface PaymentConsultationUseCase {
    Optional<PaymentConsultationView> findByPaymentReference(
            PublicPaymentReference paymentReference
    );
}
