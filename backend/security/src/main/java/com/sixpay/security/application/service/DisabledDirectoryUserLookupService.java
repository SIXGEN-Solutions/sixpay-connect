package com.sixpay.security.application.service;

import com.sixpay.security.application.model.DirectoryUserProfile;
import com.sixpay.security.application.port.input.DirectoryUnavailableException;
import com.sixpay.security.application.port.input.DirectoryUserLookupQuery;
import com.sixpay.security.application.port.input.DirectoryUserLookupUseCase;

/** Fail-closed directory lookup for deployments without LDAP. */
public final class DisabledDirectoryUserLookupService implements DirectoryUserLookupUseCase {
    @Override
    public DirectoryUserProfile lookup(DirectoryUserLookupQuery query) {
        throw new DirectoryUnavailableException(
                new IllegalStateException("LDAP directory capability is disabled")
        );
    }
}
