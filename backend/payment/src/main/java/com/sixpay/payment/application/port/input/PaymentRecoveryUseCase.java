package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.view.PaymentRecoveryView;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Optional;

/**
 * Read-only Payment recovery use case.
 */
public interface PaymentRecoveryUseCase {
    Optional<PaymentRecoveryView> findByPaymentReference(
            PublicPaymentReference paymentReference
    );
}
