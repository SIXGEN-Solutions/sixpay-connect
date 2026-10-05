package com.sixpay.security.application.port.input;

import com.sixpay.security.application.model.DirectoryAccountStatus;

public final class LdapUserNotProvisionableException extends RuntimeException {
    private final DirectoryAccountStatus accountStatus;

    public LdapUserNotProvisionableException(DirectoryAccountStatus accountStatus) {
        super("Directory user is not provisionable: " + accountStatus);
        this.accountStatus = accountStatus;
    }

    public DirectoryAccountStatus accountStatus() {
        return accountStatus;
    }
}
