package com.sixpay.security.application.port.output;

import com.sixpay.security.application.model.DirectoryUserProfile;

public interface DirectoryUserLookupPort {

    DirectoryUserProfile lookupByUsername(
            String username
    );
}
