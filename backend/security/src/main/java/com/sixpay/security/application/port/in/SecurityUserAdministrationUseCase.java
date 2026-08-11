package com.sixpay.security.application.port.in;

import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.model.SecurityUserSummary;

import java.util.List;
import java.util.UUID;

public interface SecurityUserAdministrationUseCase {

    SecurityUserDetail createUser(CreateSecurityUserCommand command);

    List<SecurityUserSummary> listUsers();

    SecurityUserDetail getUser(UUID userId);

    SecurityUserDetail updateUser(UpdateSecurityUserCommand command);

    SecurityUserDetail enableUser(
            UUID userId,
            String actorSubject
    );

    SecurityUserDetail setLocalAuthenticationEnabled(
            UUID userId,
            boolean enabled,
            String actorSubject
    );

    SecurityUserDetail resetLocalPassword(
            UUID userId,
            String newPassword,
            String actorSubject
    );

    SecurityUserDetail linkOidcIdentity(
            UUID userId,
            String provider,
            String providerSubject,
            String actorSubject
    );

    SecurityUserDetail unlinkOidcIdentity(
            UUID userId,
            UUID identityId,
            String actorSubject
    );

    SecurityUserDetail disableUser(
            UUID userId,
            String actorSubject
    );

    void deleteUser(
            UUID userId,
            String actorSubject
    );
}
