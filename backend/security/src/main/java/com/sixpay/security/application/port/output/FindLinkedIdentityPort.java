package com.sixpay.security.application.port.output;

import com.sixpay.security.domain.authentication.AuthenticationIdentityType;
import com.sixpay.security.domain.authentication.LinkedUserIdentity;

import java.util.Optional;

@FunctionalInterface
public interface FindLinkedIdentityPort {

    Optional<LinkedUserIdentity> findLinkedIdentity(
            AuthenticationIdentityType identityType,
            String provider,
            String providerSubject
    );
}
