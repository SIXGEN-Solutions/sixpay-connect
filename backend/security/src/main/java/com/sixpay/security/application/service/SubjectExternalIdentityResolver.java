package com.sixpay.security.application.service;

import com.sixpay.security.application.port.out.FindLinkedIdentityPort;

/**
 * @deprecated DA-5/DA-6 require persisted identity linking and SIXPAY-owned
 * authorization. Subject-only resolution is no longer allowed.
 */
@Deprecated(forRemoval = true)
public final class SubjectExternalIdentityResolver
        extends LinkedExternalIdentityResolver {

    public SubjectExternalIdentityResolver(
            FindLinkedIdentityPort findLinkedIdentityPort
    ) {
        super(findLinkedIdentityPort);
    }
}
