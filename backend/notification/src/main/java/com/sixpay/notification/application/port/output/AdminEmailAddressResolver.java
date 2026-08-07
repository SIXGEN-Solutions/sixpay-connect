package com.sixpay.notification.application.port.output;

public interface AdminEmailAddressResolver {

    String resolveEmail(
            String recipientReference
    );
}
