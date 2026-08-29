package com.sixpay.security.infrastructure.administration;

import com.sixpay.security.application.model.SecurityAuditView;
import com.sixpay.security.application.model.SecurityIdentityView;
import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.model.SecurityUserSummary;
import com.sixpay.security.application.port.output.SecurityUserAdministrationPort;
import com.sixpay.security.domain.authentication.AuthenticationIdentityType;
import com.sixpay.security.domain.authentication.LocalAuthenticationAccountStatus;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserAccountJpaEntity;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserAccountSpringDataRepository;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserIdentityJpaEntity;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserIdentitySpringDataRepository;
import com.sixpay.security.infrastructure.authentication.persistence.LocalAuthenticationUserJpaEntity;
import com.sixpay.security.infrastructure.authentication.persistence.LocalAuthenticationUserSpringDataRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class JpaSecurityUserAdministrationAdapter
        implements SecurityUserAdministrationPort {

    private final SecurityUserAccountSpringDataRepository userRepository;
    private final SecurityUserIdentitySpringDataRepository identityRepository;
    private final LocalAuthenticationUserSpringDataRepository localRepository;
    private final SecurityAuditSpringDataRepository auditRepository;

    public JpaSecurityUserAdministrationAdapter(
            SecurityUserAccountSpringDataRepository userRepository,
            SecurityUserIdentitySpringDataRepository identityRepository,
            LocalAuthenticationUserSpringDataRepository localRepository,
            SecurityAuditSpringDataRepository auditRepository
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.identityRepository = Objects.requireNonNull(identityRepository);
        this.localRepository = Objects.requireNonNull(localRepository);
        this.auditRepository = Objects.requireNonNull(auditRepository);
    }

    @Override
    public SecurityUserDetail createUser(
            UUID userId,
            String username,
            String email,
            Set<String> roles,
            Set<String> permissions,
            boolean localAuthenticationEnabled,
            String bcryptHash
    ) {
        String normalizedUsername = normalize(username);
        rejectDuplicateUsername(normalizedUsername, null);
        rejectDuplicateEmail(email, null);

        Instant now = Instant.now();
        SecurityUserAccountJpaEntity account =
                SecurityUserAccountJpaEntity.create(
                        userId,
                        username,
                        email,
                        roles,
                        permissions,
                        now
                );
        userRepository.save(account);

        if (localAuthenticationEnabled) {
            if (bcryptHash == null || bcryptHash.isBlank()) {
                throw new IllegalArgumentException(
                        "Password hash is required when Local authentication is enabled"
                );
            }

            localRepository.save(
                    LocalAuthenticationUserJpaEntity.provisioned(
                            account,
                            bcryptHash,
                            now
                    )
            );

            identityRepository.save(
                    SecurityUserIdentityJpaEntity.linkedLocal(
                            account,
                            now
                    )
            );
        }

        return getUser(userId);
    }

    @Override
    public List<SecurityUserSummary> listUsers() {
        return userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(
                        SecurityUserAccountJpaEntity::getUsername
                ))
                .map(this::toSummary)
                .toList();
    }

    @Override
    public SecurityUserDetail getUser(UUID userId) {
        SecurityUserAccountJpaEntity account = requireUser(userId);

        List<SecurityUserIdentityJpaEntity> identities =
                identityRepository.findByUserAccount_IdOrderByCreatedAtAsc(
                        userId
                );

        boolean localEnabled =
                localRepository.findByUserAccount_Id(userId)
                        .map(local ->
                                local.getStatus()
                                        == LocalAuthenticationAccountStatus.ACTIVE
                        )
                        .orElse(false);

        List<SecurityIdentityView> identityViews =
                identities.stream()
                        .map(identity -> new SecurityIdentityView(
                                identity.getId(),
                                identity.getIdentityType(),
                                identity.getProvider(),
                                identity.getProviderSubject(),
                                "LINKED",
                                identity.getCreatedAt(),
                                identity.getUpdatedAt()
                        ))
                        .toList();

        List<SecurityAuditView> auditViews =
                auditRepository
                        .findTop20ByTargetUserIdOrderByOccurredAtDesc(userId)
                        .stream()
                        .map(event -> new SecurityAuditView(
                                event.getEventType(),
                                event.getActorSubject(),
                                event.getProvider(),
                                event.getDetail(),
                                event.getOccurredAt()
                        ))
                        .toList();

        return new SecurityUserDetail(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getStatus(),
                localEnabled,
                identities.stream().anyMatch(identity ->
                        identity.getIdentityType()
                                == AuthenticationIdentityType.OIDC
                ),
                account.getRoles(),
                account.getPermissions(),
                identityViews,
                auditViews
        );
    }

    @Override
    public void updateUser(
            UUID userId,
            String username,
            String email,
            Set<String> roles,
            Set<String> permissions
    ) {
        SecurityUserAccountJpaEntity account = requireUser(userId);

        rejectDuplicateUsername(normalize(username), userId);
        rejectDuplicateEmail(email, userId);

        Instant now = Instant.now();
        account.update(
                username,
                email,
                roles,
                permissions,
                now
        );
        userRepository.save(account);

        localRepository.findByUserAccount_Id(userId)
                .ifPresent(local -> {
                    local.rename(username, now);
                    localRepository.save(local);
                });
    }

    @Override
    public void enableUser(UUID userId) {
        SecurityUserAccountJpaEntity account = requireUser(userId);
        account.enable(Instant.now());
        userRepository.save(account);
    }

    @Override
    public void setLocalAuthenticationEnabled(
            UUID userId,
            boolean enabled
    ) {
        LocalAuthenticationUserJpaEntity local =
                localRepository.findByUserAccount_Id(userId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Local authentication is not provisioned for user"
                                )
                        );
        local.setEnabled(enabled, Instant.now());
        localRepository.save(local);
    }

    @Override
    public void resetLocalPassword(
            UUID userId,
            String bcryptHash
    ) {
        LocalAuthenticationUserJpaEntity local =
                localRepository.findByUserAccount_Id(userId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Local authentication is not provisioned for user"
                                )
                        );
        local.resetPassword(bcryptHash, Instant.now());
        localRepository.save(local);
    }

    @Override
    public void linkOidcIdentity(
            UUID userId,
            String provider,
            String providerSubject
    ) {
        if (provider == null
                || provider.isBlank()
                || providerSubject == null
                || providerSubject.isBlank()) {
            throw new IllegalArgumentException(
                    "OIDC provider and subject are required"
            );
        }

        if (identityRepository
                .existsByIdentityTypeAndProviderAndProviderSubject(
                        AuthenticationIdentityType.OIDC,
                        provider,
                        providerSubject
                )) {
            throw new IllegalStateException(
                    "OIDC identity is already linked"
            );
        }

        SecurityUserAccountJpaEntity account = requireUser(userId);
        identityRepository.save(
                SecurityUserIdentityJpaEntity.linkedOidc(
                        account,
                        provider.trim(),
                        providerSubject.trim(),
                        Instant.now()
                )
        );
    }

    @Override
    public void unlinkOidcIdentity(
            UUID userId,
            UUID identityId
    ) {
        SecurityUserIdentityJpaEntity identity =
                identityRepository.findById(identityId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Authentication identity not found"
                                )
                        );

        if (!identity.getUserAccount().getId().equals(userId)
                || identity.getIdentityType()
                != AuthenticationIdentityType.OIDC) {
            throw new IllegalArgumentException(
                    "Identity is not an OIDC identity of the requested user"
            );
        }

        identityRepository.delete(identity);
    }

    @Override
    public void disableUser(UUID userId) {
        SecurityUserAccountJpaEntity account = requireUser(userId);
        account.disable(Instant.now());
        userRepository.save(account);
    }

    @Override
    public void deleteUser(UUID userId) {
        SecurityUserAccountJpaEntity account = requireUser(userId);

        /*
         * security_local_users.user_id does not use ON DELETE CASCADE.
         * Delete the credential record first. Linked identities, roles and
         * permissions are DB-cascaded from the canonical account.
         */
        localRepository.findByUserAccount_Id(userId)
                .ifPresent(localRepository::delete);

        userRepository.delete(account);
    }

    private SecurityUserSummary toSummary(
            SecurityUserAccountJpaEntity account
    ) {
        UUID userId = account.getId();
        LocalAuthenticationUserJpaEntity local =
                localRepository.findByUserAccount_Id(userId)
                        .orElse(null);

        boolean localEnabled =
                local != null
                        && local.getStatus()
                        == LocalAuthenticationAccountStatus.ACTIVE;

        boolean oidcLinked =
                identityRepository
                        .findByUserAccount_IdOrderByCreatedAtAsc(userId)
                        .stream()
                        .anyMatch(identity ->
                                identity.getIdentityType()
                                        == AuthenticationIdentityType.OIDC
                        );

        Instant lastAuthenticationAt =
                local == null
                        ? null
                        : local.getLastAuthenticatedAt();

        return new SecurityUserSummary(
                userId,
                account.getUsername(),
                account.getEmail(),
                account.getStatus(),
                localEnabled,
                oidcLinked,
                lastAuthenticationAt
        );
    }

    private SecurityUserAccountJpaEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "SIXPAY user not found"
                        )
                );
    }

    private void rejectDuplicateUsername(
            String normalizedUsername,
            UUID currentUserId
    ) {
        boolean exists = currentUserId == null
                ? userRepository.existsByNormalizedUsername(
                        normalizedUsername
                )
                : userRepository.existsByNormalizedUsernameAndIdNot(
                        normalizedUsername,
                        currentUserId
                );

        if (exists) {
            throw new IllegalStateException(
                    "SIXPAY username is already input use"
            );
        }
    }

    private void rejectDuplicateEmail(
            String email,
            UUID currentUserId
    ) {
        if (email == null || email.isBlank()) {
            return;
        }

        boolean exists = currentUserId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(
                        email,
                        currentUserId
                );

        if (exists) {
            throw new IllegalStateException(
                    "SIXPAY email is already input use"
            );
        }
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
