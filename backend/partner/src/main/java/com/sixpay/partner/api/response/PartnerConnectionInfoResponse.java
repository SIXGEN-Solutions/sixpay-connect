package com.sixpay.partner.api.response;

import java.util.Set;

public record PartnerConnectionInfoResponse(
        String apiBasePath,
        Set<String> supportedAuthenticationMethods,
        boolean newTransactionsAllowed
) {

    public static PartnerConnectionInfoResponse forStatus(
            com.sixpay.partner.domain.model.PartnerStatus status
    ) {
        return new PartnerConnectionInfoResponse(
                "/api/v1",
                Set.of("MTLS", "API_KEY"),
                status == com.sixpay.partner.domain.model.PartnerStatus.ACTIVE
        );
    }
}
