package com.sixpay.partner.application.port.output;

public interface PartnerOperationMetrics {

    void succeeded(Operation operation);

    void replayed(Operation operation);

    void rejected(Rejection rejection);

    enum Operation {
        CREATE,
        DECIDE,
        SUSPEND,
        REACTIVATE,
        CONFIGURE_THRESHOLD
    }

    enum Rejection {
        NOT_FOUND,
        DOMAIN_RULE,
        INVALID_REQUEST
    }
}
