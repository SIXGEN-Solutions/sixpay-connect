package com.sixpay.security.api.dto;

public record AuthenticationCapabilitiesResponse(
        boolean localEnabled,
        boolean oidcEnabled,
        boolean ldapEnabled
) {
}
