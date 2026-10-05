package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.view.TresorPayPaymentRecoveryView;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Optional;

/**
 * Read-only TRESOR PAY recovery use case.
 */
public interface TresorPayPaymentRecoveryUseCase {
    Optional<TresorPayPaymentRecoveryView> findByPaymentReference(
            PublicPaymentReference paymentReference
    );
}
