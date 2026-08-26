package com.sixpay.security.application.model;

import com.sixpay.security.domain.authentication.SixpayUserAccountStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record SecurityUserDetail(
        UUID id,
        String username,
        String email,
        SixpayUserAccountStatus status,
        boolean localEnabled,
        boolean oidcLinked,
        Set<String> roles,
        Set<String> permissions,
        List<SecurityIdentityView> identities,
        List<SecurityAuditView> recentAuthenticationEvents
) {
}
