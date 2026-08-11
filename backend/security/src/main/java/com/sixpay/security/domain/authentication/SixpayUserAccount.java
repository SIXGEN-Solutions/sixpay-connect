package com.sixpay.security.domain.authentication;

import com.sixpay.common.validation.Preconditions;

import java.util.UUID;

public record SixpayUserAccount(
        UUID id,
        String username,
        String email,
        SixpayUserAccountStatus status
) {
    public SixpayUserAccount {
        id = Preconditions.requireNonNull(id, "SIXPAY user id must not be null");
        username = Preconditions.requireNonBlank(username, "SIXPAY username must not be blank");
        status = Preconditions.requireNonNull(status, "SIXPAY user status must not be null");
        email = email == null || email.isBlank() ? null : email.trim();
    }

    public boolean active() {
        return status == SixpayUserAccountStatus.ACTIVE;
    }

    public String canonicalSubject() {
        return id.toString();
    }
}
