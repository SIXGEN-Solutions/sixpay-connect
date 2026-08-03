package com.sixpay.payment.application.port.output.callback;

/**
 * Sends one already committed Payment status notification to its partner.
 */
public interface PaymentStatusCallbackTransportPort {

    void send(PaymentStatusCallbackDelivery delivery);
}
