package com.sixpay.security.application.port.output;

import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.model.SecurityUserSummary;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SecurityUserAdministrationPort {

    SecurityUserDetail createUser(
            UUID userId,
            String username,
            String email,
            Set<String> roles,
            Set<String> permissions,
            boolean localAuthenticationEnabled,
            String bcryptHash
    );

    List<SecurityUserSummary> listUsers();

    SecurityUserDetail getUser(UUID userId);

    void updateUser(
            UUID userId,
            String username,
            String email,
            Set<String> roles,
            Set<String> permissions
    );

    void enableUser(UUID userId);

    void setLocalAuthenticationEnabled(
            UUID userId,
            boolean enabled
    );

    void resetLocalPassword(
            UUID userId,
            String bcryptHash
    );

    void linkOidcIdentity(
            UUID userId,
            String provider,
            String providerSubject
    );

    void unlinkOidcIdentity(
            UUID userId,
            UUID identityId
    );

    void disableUser(UUID userId);

    void deleteUser(UUID userId);
}
