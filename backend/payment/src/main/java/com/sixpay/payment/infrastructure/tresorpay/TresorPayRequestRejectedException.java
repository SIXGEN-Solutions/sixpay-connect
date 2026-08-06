package com.sixpay.payment.infrastructure.tresorpay;

import org.springframework.http.HttpStatus;

public final class TresorPayRequestRejectedException extends RuntimeException {
    private final HttpStatus status;
    private final TresorPayErrorCode code;
    private final Integer retryAfterSeconds;

    public TresorPayRequestRejectedException(
            HttpStatus status,
            TresorPayErrorCode code,
            String safeMessage
    ) {
        this(status, code, safeMessage, null);
    }

    public TresorPayRequestRejectedException(
            HttpStatus status,
            TresorPayErrorCode code,
            String safeMessage,
            Integer retryAfterSeconds
    ) {
        super(safeMessage);
        this.status = status;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus status() { return status; }
    public TresorPayErrorCode code() { return code; }
    public Integer retryAfterSeconds() { return retryAfterSeconds; }
}
