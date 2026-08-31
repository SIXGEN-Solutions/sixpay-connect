package com.sixpay.customer.verification.infrastructure.banking.error;

import java.util.List;

public record AmplitudeErrorResponse(
        String type,
        String title,
        Integer status,
        String detail,
        String instance,
        String code,
        String correlationId,
        List<InvalidParameter> invalidParams
) {
    public static AmplitudeErrorResponse unknown(
            int status,
            String correlationId
    ) {
        return new AmplitudeErrorResponse(
                "about:blank",
                "Core Banking error",
                status,
                "Core Banking returned an unreadable error response",
                null,
                "AMPLITUDE_INTERNAL_ERROR",
                correlationId,
                List.of()
        );
    }

    @Override
    public String toString() {
        return "AmplitudeErrorResponse[type=" + type
                + ", title=" + title
                + ", status=" + status
                + ", code=" + code
                + ", correlationId=" + correlationId
                + "]";
    }

    public record InvalidParameter(
            String name,
            String reason
    ) { }
}
