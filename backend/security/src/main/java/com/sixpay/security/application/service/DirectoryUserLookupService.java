package com.sixpay.security.application.service;

import com.sixpay.security.application.model.DirectoryUserProfile;
import com.sixpay.security.application.port.input.DirectoryUserLookupQuery;
import com.sixpay.security.application.port.input.DirectoryUserLookupUseCase;
import com.sixpay.security.application.port.output.DirectoryUserLookupPort;

import java.util.Objects;

public final class DirectoryUserLookupService
        implements DirectoryUserLookupUseCase {

    private final DirectoryUserLookupPort lookupPort;

    public DirectoryUserLookupService(
            DirectoryUserLookupPort lookupPort
    ) {
        this.lookupPort = Objects.requireNonNull(
                lookupPort,
                "Directory user lookup port must not be null"
        );
    }

    @Override
    public DirectoryUserProfile lookup(
            DirectoryUserLookupQuery query
    ) {
        Objects.requireNonNull(
                query,
                "Directory user lookup query must not be null"
        );
        return lookupPort.lookupByUsername(query.username());
    }
}
