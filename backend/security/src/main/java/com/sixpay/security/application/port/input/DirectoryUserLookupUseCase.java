package com.sixpay.security.application.port.input;

import com.sixpay.security.application.model.DirectoryUserProfile;

@FunctionalInterface
public interface DirectoryUserLookupUseCase {

    DirectoryUserProfile lookup(
            DirectoryUserLookupQuery query
    );
}
