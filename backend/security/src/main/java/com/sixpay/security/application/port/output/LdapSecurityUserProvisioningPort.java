package com.sixpay.security.application.port.output;

import com.sixpay.security.application.model.DirectoryUserProfile;
import com.sixpay.security.application.model.SecurityUserDetail;

import java.util.Set;
import java.util.UUID;

public interface LdapSecurityUserProvisioningPort {

    boolean ldapIdentityExists(String trustDomain, String stableSubject);

    boolean usernameExists(String username);

    SecurityUserDetail createCanonicalLdapUser(
            UUID userId,
            DirectoryUserProfile directoryUser,
            Set<String> roles,
            Set<String> permissions,
            String actorSubject
    );
}
