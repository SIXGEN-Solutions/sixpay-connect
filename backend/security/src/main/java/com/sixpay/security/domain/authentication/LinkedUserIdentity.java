package com.sixpay.security.domain.authentication;

import com.sixpay.common.validation.Preconditions;

public record LinkedUserIdentity(
        SixpayUserAccount userAccount,
        UserIdentity identity
) {
    public LinkedUserIdentity {
        userAccount = Preconditions.requireNonNull(userAccount, "Linked SIXPAY user must not be null");
        identity = Preconditions.requireNonNull(identity, "Linked authentication identity must not be null");

        if (!userAccount.id().equals(identity.userId())) {
            throw new IllegalArgumentException(
                    "Authentication identity must reference the linked SIXPAY user"
            );
        }
    }
}
