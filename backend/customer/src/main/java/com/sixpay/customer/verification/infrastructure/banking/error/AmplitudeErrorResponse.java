package com.sixpay.customer.verification.infrastructure.banking.error;

public record AmplitudeErrorResponse(
        String type,
        String title,
        Integer status,
        String code,
        String detail,
        String correlationId,
        Boolean retryable
) {
    public static AmplitudeErrorResponse unknown(
            int status,
            String correlationId
    ) {
        return new AmplitudeErrorResponse(
                "about:blank",
                "Core Banking error",
                status,
                "CORE_BANKING_UNCLASSIFIED_ERROR",
                "Core Banking returned an unreadable error response",
                correlationId,
                null
        );
    }

    @Override
    public String toString() {
        return "AmplitudeErrorResponse[type=" + type
                + ", title=" + title
                + ", status=" + status
                + ", code=" + code
                + ", correlationId=" + correlationId
                + ", retryable=" + retryable
                + "]";
    }
}
