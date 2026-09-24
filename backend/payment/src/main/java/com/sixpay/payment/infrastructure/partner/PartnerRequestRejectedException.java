package com.sixpay.payment.infrastructure.partner;

import org.springframework.http.HttpStatus;

public final class PartnerRequestRejectedException extends RuntimeException {
    private final HttpStatus status;
    private final PartnerRequestErrorCode code;
    private final Integer retryAfterSeconds;

    public PartnerRequestRejectedException(
            HttpStatus status,
            PartnerRequestErrorCode code,
            String safeMessage
    ) {
        this(status, code, safeMessage, null);
    }

    public PartnerRequestRejectedException(
            HttpStatus status,
            PartnerRequestErrorCode code,
            String safeMessage,
            Integer retryAfterSeconds
    ) {
        super(safeMessage);
        this.status = status;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus status() { return status; }
    public PartnerRequestErrorCode code() { return code; }
    public Integer retryAfterSeconds() { return retryAfterSeconds; }
}
