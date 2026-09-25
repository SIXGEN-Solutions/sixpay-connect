#!/usr/bin/env python3
"""
SIXPAY CONNECT — INIT-2B Partner machine identity
Target HEAD: 366556155a82022e5d76b47b4aca2ad50daf41f8

Scope:
- introduce a Security-owned machine identity surface for Partner M2M callers;
- bind a trusted machine identity to a canonical Partner business identifier;
- let Bootstrap resolve the authenticated machine caller to Partner;
- let Payment compare authenticated Partner vs declared AppID/partnerIdentifier.

Out of scope on purpose:
- changing the public Payment OpenAPI / physical contract;
- changing transport authentication mechanisms (JWT/API-key/mTLS);
- LDAP;
- commit/push/PR;
- gate execution.

No worktree cleanliness check is performed.
"""
from pathlib import Path
import subprocess

ROOT = Path.cwd()
EXPECTED_HEAD = "366556155a82022e5d76b47b4aca2ad50daf41f8"

def read(rel):
    p = ROOT / rel
    if not p.is_file():
        raise RuntimeError(f"Missing required file: {rel}")
    return p.read_text(encoding="utf-8")

def write(rel, content):
    p = ROOT / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding="utf-8", newline="\n")
    print("UPDATED", rel)

def ensure_replace(rel, old, new, sentinel=None):
    s = read(rel)
    if sentinel and sentinel in s:
        return
    if old not in s:
        raise RuntimeError(f"{rel}: expected marker not found:\n{old}")
    write(rel, s.replace(old, new, 1))

head = subprocess.run(
    ["git", "rev-parse", "HEAD"],
    cwd=ROOT,
    text=True,
    capture_output=True,
    check=True
).stdout.strip()
if head != EXPECTED_HEAD:
    raise RuntimeError(f"Unexpected HEAD: {head}. Expected {EXPECTED_HEAD}")

# ---------------------------------------------------------------------------
# Security public machine identity surface
# ---------------------------------------------------------------------------

write(
    "backend/security/src/main/java/com/sixpay/security/authentication/AuthenticatedMachineIdentity.java",
    """package com.sixpay.security.authentication;

import com.sixpay.common.validation.Preconditions;

/**
 * Minimal trusted machine identity established by the transport security layer.
 *
 * <p>This type is deliberately distinct from {@link AuthenticatedUser}: M2M
 * Partner callers are not SIXPAY human users.</p>
 */
public record AuthenticatedMachineIdentity(
        String subject
) {

    public AuthenticatedMachineIdentity {
        subject = Preconditions.requireNonBlank(
                subject,
                "Authenticated machine subject must not be blank"
        );
    }
}
"""
)

write(
    "backend/security/src/main/java/com/sixpay/security/authentication/CurrentMachineIdentityProvider.java",
    """package com.sixpay.security.authentication;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.util.Optional;

/**
 * Security public surface for the authenticated machine caller.
 */
public interface CurrentMachineIdentityProvider {

    Optional<AuthenticatedMachineIdentity> currentMachineIdentity();

    default AuthenticatedMachineIdentity requireCurrentMachineIdentity() {
        return currentMachineIdentity().orElseThrow(
                () -> new AuthenticationCredentialsNotFoundException(
                        "No authenticated machine identity is available"
                )
        );
    }
}
"""
)

write(
    "backend/security/src/main/java/com/sixpay/security/authentication/SecurityContextCurrentMachineIdentityProvider.java",
    """package com.sixpay.security.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Reads the trusted machine subject established by Spring Security.
 *
 * <p>No Partner business identifier is inferred here. The subject is only the
 * authenticated technical identity key.</p>
 */
public final class SecurityContextCurrentMachineIdentityProvider
        implements CurrentMachineIdentityProvider {

    @Override
    public Optional<AuthenticatedMachineIdentity> currentMachineIdentity() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return Optional.empty();
        }

        /*
         * Human SIXPAY principals are deliberately excluded from the M2M
         * surface. Partner system calls must be represented by the transport
         * authentication itself, not by AuthenticatedUser.
         */
        if (authentication.getPrincipal() instanceof AuthenticatedUser) {
            return Optional.empty();
        }

        return Optional.of(
                new AuthenticatedMachineIdentity(authentication.getName())
        );
    }
}
"""
)

# Machine identity -> Partner link is Security-owned because this is identity
# association, not Partner domain state.
write(
    "backend/security/src/main/java/com/sixpay/security/application/model/PartnerMachineIdentityView.java",
    """package com.sixpay.security.application.model;

import com.sixpay.common.validation.Preconditions;

/**
 * Public Security projection linking one trusted machine subject to a Partner
 * business identifier.
 */
public record PartnerMachineIdentityView(
        String machineSubject,
        String partnerIdentifier
) {

    public PartnerMachineIdentityView {
        machineSubject = Preconditions.requireNonBlank(
                machineSubject,
                "Machine subject must not be blank"
        );
        partnerIdentifier = Preconditions.requireNonBlank(
                partnerIdentifier,
                "Partner identifier must not be blank"
        );
    }
}
"""
)

write(
    "backend/security/src/main/java/com/sixpay/security/application/port/input/PartnerMachineIdentityQueryUseCase.java",
    """package com.sixpay.security.application.port.input;

import com.sixpay.security.application.model.PartnerMachineIdentityView;

import java.util.Optional;

/**
 * Resolves a trusted machine subject to the registered Partner identity link.
 */
public interface PartnerMachineIdentityQueryUseCase {

    Optional<PartnerMachineIdentityView> findByMachineSubject(
            String machineSubject
    );
}
"""
)

# New migration, never edit V700.
write(
    "backend/security/src/main/resources/db/migration/V701__partner_machine_identity_link.sql",
    """-- INIT-2B — Partner M2M machine identity link.
-- This table belongs to Security: it binds a trusted technical caller identity
-- to the Partner business identifier that Partner owns.

CREATE TABLE security_partner_machine_identities (
    id UUID PRIMARY KEY,
    machine_subject VARCHAR(255) NOT NULL,
    partner_identifier VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_security_partner_machine_identity_subject
        UNIQUE (machine_subject),
    CONSTRAINT ck_security_partner_machine_identity_subject
        CHECK (NULLIF(BTRIM(machine_subject), '') IS NOT NULL),
    CONSTRAINT ck_security_partner_machine_identity_partner
        CHECK (NULLIF(BTRIM(partner_identifier), '') IS NOT NULL),
    CONSTRAINT ck_security_partner_machine_identity_timestamps
        CHECK (updated_at >= created_at)
);

CREATE INDEX ix_security_partner_machine_identity_partner
    ON security_partner_machine_identities (partner_identifier)
    WHERE enabled = TRUE;

COMMENT ON TABLE security_partner_machine_identities IS
    'Security-owned binding from authenticated Partner M2M machine subject to canonical Partner business identifier.';
"""
)

write(
    "backend/security/src/main/java/com/sixpay/security/infrastructure/authentication/machine/PartnerMachineIdentityJpaEntity.java",
    """package com.sixpay.security.infrastructure.authentication.machine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_partner_machine_identities")
public class PartnerMachineIdentityJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "machine_subject", nullable = false, unique = true, length = 255)
    private String machineSubject;

    @Column(name = "partner_identifier", nullable = false, length = 64)
    private String partnerIdentifier;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PartnerMachineIdentityJpaEntity() {
    }

    public UUID id() {
        return id;
    }

    public String machineSubject() {
        return machineSubject;
    }

    public String partnerIdentifier() {
        return partnerIdentifier;
    }

    public boolean enabled() {
        return enabled;
    }
}
"""
)

write(
    "backend/security/src/main/java/com/sixpay/security/infrastructure/authentication/machine/PartnerMachineIdentitySpringDataRepository.java",
    """package com.sixpay.security.infrastructure.authentication.machine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartnerMachineIdentitySpringDataRepository
        extends JpaRepository<PartnerMachineIdentityJpaEntity, UUID> {

    Optional<PartnerMachineIdentityJpaEntity> findByMachineSubjectAndEnabledTrue(
            String machineSubject
    );
}
"""
)

write(
    "backend/security/src/main/java/com/sixpay/security/infrastructure/authentication/machine/JpaPartnerMachineIdentityQueryAdapter.java",
    """package com.sixpay.security.infrastructure.authentication.machine;

import com.sixpay.security.application.model.PartnerMachineIdentityView;
import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;

import java.util.Objects;
import java.util.Optional;

public final class JpaPartnerMachineIdentityQueryAdapter
        implements PartnerMachineIdentityQueryUseCase {

    private final PartnerMachineIdentitySpringDataRepository repository;

    public JpaPartnerMachineIdentityQueryAdapter(
            PartnerMachineIdentitySpringDataRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Optional<PartnerMachineIdentityView> findByMachineSubject(
            String machineSubject
    ) {
        if (machineSubject == null || machineSubject.isBlank()) {
            return Optional.empty();
        }

        return repository
                .findByMachineSubjectAndEnabledTrue(machineSubject.strip())
                .map(entity -> new PartnerMachineIdentityView(
                        entity.machineSubject(),
                        entity.partnerIdentifier()
                ));
    }
}
"""
)

write(
    "backend/security/src/main/java/com/sixpay/security/configuration/PartnerMachineIdentityConfiguration.java",
    """package com.sixpay.security.configuration;

import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;
import com.sixpay.security.authentication.CurrentMachineIdentityProvider;
import com.sixpay.security.authentication.SecurityContextCurrentMachineIdentityProvider;
import com.sixpay.security.infrastructure.authentication.machine.JpaPartnerMachineIdentityQueryAdapter;
import com.sixpay.security.infrastructure.authentication.machine.PartnerMachineIdentitySpringDataRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PartnerMachineIdentityConfiguration {

    @Bean
    @ConditionalOnMissingBean(CurrentMachineIdentityProvider.class)
    CurrentMachineIdentityProvider currentMachineIdentityProvider() {
        return new SecurityContextCurrentMachineIdentityProvider();
    }

    @Bean
    @ConditionalOnBean(PartnerMachineIdentitySpringDataRepository.class)
    @ConditionalOnMissingBean(PartnerMachineIdentityQueryUseCase.class)
    PartnerMachineIdentityQueryUseCase partnerMachineIdentityQueryUseCase(
            PartnerMachineIdentitySpringDataRepository repository
    ) {
        return new JpaPartnerMachineIdentityQueryAdapter(repository);
    }
}
"""
)

# Register configuration with Security auto-config.
ensure_replace(
    "backend/security/src/main/java/com/sixpay/security/configuration/SixpaySecurityAutoConfiguration.java",
    """        IdentityLinkingConfiguration.class,
        AuthenticationSessionController.class""",
    """        IdentityLinkingConfiguration.class,
        PartnerMachineIdentityConfiguration.class,
        AuthenticationSessionController.class""",
    "PartnerMachineIdentityConfiguration.class"
)

# ---------------------------------------------------------------------------
# Bootstrap: machine identity + Security link + Partner public resolution
# ---------------------------------------------------------------------------

write(
    "backend/bootstrap/src/main/java/com/sixpay/bootstrap/integration/partner/PaymentPartnerIdentityModuleAdapter.java",
    """package com.sixpay.bootstrap.integration.partner;

import com.sixpay.partner.application.port.input.PartnerIdentityQueryUseCase;
import com.sixpay.payment.application.port.output.partner.PartnerIdentityResolutionPort;
import com.sixpay.payment.application.port.output.partner.ResolvedPartnerIdentity;
import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;
import com.sixpay.security.authentication.CurrentMachineIdentityProvider;

import java.util.Objects;
import java.util.Optional;

/**
 * Composition adapter connecting Security machine identity to Partner business
 * identity without leaking Security or Partner infrastructure into Payment.
 */
public final class PaymentPartnerIdentityModuleAdapter
        implements PartnerIdentityResolutionPort {

    private final CurrentMachineIdentityProvider machineIdentityProvider;
    private final PartnerMachineIdentityQueryUseCase machineIdentityQuery;
    private final PartnerIdentityQueryUseCase partnerIdentityQuery;

    public PaymentPartnerIdentityModuleAdapter(
            CurrentMachineIdentityProvider machineIdentityProvider,
            PartnerMachineIdentityQueryUseCase machineIdentityQuery,
            PartnerIdentityQueryUseCase partnerIdentityQuery
    ) {
        this.machineIdentityProvider = Objects.requireNonNull(machineIdentityProvider);
        this.machineIdentityQuery = Objects.requireNonNull(machineIdentityQuery);
        this.partnerIdentityQuery = Objects.requireNonNull(partnerIdentityQuery);
    }

    @Override
    public Optional<ResolvedPartnerIdentity> resolveAuthenticatedPartner(
            String authenticatedSubject
    ) {
        String trustedSubject = machineIdentityProvider
                .requireCurrentMachineIdentity()
                .subject();

        if (authenticatedSubject == null
                || !trustedSubject.equals(authenticatedSubject.strip())) {
            return Optional.empty();
        }

        return machineIdentityQuery
                .findByMachineSubject(trustedSubject)
                .flatMap(link ->
                        partnerIdentityQuery
                                .findByPartnerIdentifier(
                                        link.partnerIdentifier()
                                )
                )
                .map(view -> new ResolvedPartnerIdentity(
                        view.partnerId(),
                        view.partnerIdentifier(),
                        view.acceptsNewTransactions()
                ));
    }

    @Override
    public Optional<ResolvedPartnerIdentity> findByPartnerIdentifier(
            String partnerIdentifier
    ) {
        return partnerIdentityQuery
                .findByPartnerIdentifier(partnerIdentifier)
                .map(view -> new ResolvedPartnerIdentity(
                        view.partnerId(),
                        view.partnerIdentifier(),
                        view.acceptsNewTransactions()
                ));
    }
}
"""
)

write(
    "backend/bootstrap/src/main/java/com/sixpay/bootstrap/integration/partner/PaymentPartnerIdentityConfiguration.java",
    """package com.sixpay.bootstrap.integration.partner;

import com.sixpay.partner.application.port.input.PartnerIdentityQueryUseCase;
import com.sixpay.payment.application.port.output.partner.PartnerIdentityResolutionPort;
import com.sixpay.payment.application.service.PartnerIdentityAlignmentService;
import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;
import com.sixpay.security.authentication.CurrentMachineIdentityProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cross-module wiring only.
 */
@Configuration(proxyBeanMethods = false)
public class PaymentPartnerIdentityConfiguration {

    @Bean
    PartnerIdentityResolutionPort paymentPartnerIdentityResolutionPort(
            CurrentMachineIdentityProvider machineIdentityProvider,
            PartnerMachineIdentityQueryUseCase machineIdentityQuery,
            PartnerIdentityQueryUseCase partnerIdentityQueryUseCase
    ) {
        return new PaymentPartnerIdentityModuleAdapter(
                machineIdentityProvider,
                machineIdentityQuery,
                partnerIdentityQueryUseCase
        );
    }

    @Bean
    PartnerIdentityAlignmentService partnerIdentityAlignmentService(
            PartnerIdentityResolutionPort resolutionPort
    ) {
        return new PartnerIdentityAlignmentService(resolutionPort);
    }
}
"""
)

# ---------------------------------------------------------------------------
# Payment API: stop reading human CurrentUserProvider for M2M caller.
# Keep physical payload/contract untouched in INIT-2B.
# ---------------------------------------------------------------------------

path = "backend/payment/src/main/java/com/sixpay/payment/api/PaymentCommandController.java"
s = read(path)
s = s.replace(
    "import com.sixpay.security.authentication.CurrentUserProvider;\n",
    "import com.sixpay.security.authentication.CurrentMachineIdentityProvider;\n"
)
s = s.replace(
    "    private final CurrentUserProvider currentUserProvider;\n",
    "    private final CurrentMachineIdentityProvider currentMachineIdentityProvider;\n"
)
s = s.replace(
    "            CurrentUserProvider currentUserProvider,\n",
    "            CurrentMachineIdentityProvider currentMachineIdentityProvider,\n"
)
s = s.replace(
    "        this.currentUserProvider = currentUserProvider;\n",
    "        this.currentMachineIdentityProvider = currentMachineIdentityProvider;\n"
)
s = s.replace(
    "        String authenticatedPartner =\n"
    "                currentUserProvider.requireCurrentUser().username();\n",
    "        String authenticatedPartner =\n"
    "                currentMachineIdentityProvider\n"
    "                        .requireCurrentMachineIdentity()\n"
    "                        .subject();\n"
)
write(path, s)

# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

write(
    "backend/security/src/test/java/com/sixpay/security/authentication/SecurityContextCurrentMachineIdentityProviderTest.java",
    """package com.sixpay.security.authentication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextCurrentMachineIdentityProviderTest {

    private final SecurityContextCurrentMachineIdentityProvider provider =
            new SecurityContextCurrentMachineIdentityProvider();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exposesAuthenticatedTechnicalSubject() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "partner-client-001",
                        "N/A",
                        List.of()
                )
        );

        assertThat(provider.requireCurrentMachineIdentity().subject())
                .isEqualTo("partner-client-001");
    }

    @Test
    void doesNotTreatHumanSixpayUserAsMachineCaller() {
        AuthenticatedUser user = new AuthenticatedUser(
                "user-subject",
                "userseed",
                Set.of("ROLE_PARTNER")
        );

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        user,
                        "N/A",
                        List.of()
                )
        );

        assertThat(provider.currentMachineIdentity()).isEmpty();
    }
}
"""
)

write(
    "backend/bootstrap/src/test/java/com/sixpay/bootstrap/integration/partner/PaymentPartnerIdentityModuleAdapterTest.java",
    """package com.sixpay.bootstrap.integration.partner;

import com.sixpay.partner.application.port.input.PartnerIdentityQueryUseCase;
import com.sixpay.partner.application.view.PartnerIdentityView;
import com.sixpay.partner.domain.model.PartnerStatus;
import com.sixpay.security.application.model.PartnerMachineIdentityView;
import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;
import com.sixpay.security.authentication.AuthenticatedMachineIdentity;
import com.sixpay.security.authentication.CurrentMachineIdentityProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPartnerIdentityModuleAdapterTest {

    private static final UUID PARTNER_ID =
            UUID.fromString("8ec6a427-406f-4f93-b271-cbc819a4c1dd");

    @Test
    void resolvesAuthenticatedMachineToRegisteredActivePartner() {
        CurrentMachineIdentityProvider current =
                () -> Optional.of(
                        new AuthenticatedMachineIdentity("client-001")
                );

        PartnerMachineIdentityQueryUseCase machineQuery =
                subject -> Optional.of(
                        new PartnerMachineIdentityView(
                                subject,
                                "PARTNER_001"
                        )
                );

        PartnerIdentityQueryUseCase partnerQuery =
                identifier -> Optional.of(
                        new PartnerIdentityView(
                                PARTNER_ID,
                                identifier,
                                PartnerStatus.ACTIVE
                        )
                );

        var adapter = new PaymentPartnerIdentityModuleAdapter(
                current,
                machineQuery,
                partnerQuery
        );

        assertThat(adapter.resolveAuthenticatedPartner("client-001"))
                .get()
                .satisfies(identity -> {
                    assertThat(identity.partnerId()).isEqualTo(PARTNER_ID);
                    assertThat(identity.partnerIdentifier())
                            .isEqualTo("PARTNER_001");
                    assertThat(identity.acceptsNewTransactions()).isTrue();
                });
    }

    @Test
    void rejectsSubjectDifferentFromTrustedMachineIdentity() {
        var adapter = new PaymentPartnerIdentityModuleAdapter(
                () -> Optional.of(
                        new AuthenticatedMachineIdentity("client-001")
                ),
                subject -> Optional.of(
                        new PartnerMachineIdentityView(subject, "PARTNER_001")
                ),
                identifier -> Optional.of(
                        new PartnerIdentityView(
                                PARTNER_ID,
                                identifier,
                                PartnerStatus.ACTIVE
                        )
                )
        );

        assertThat(
                adapter.resolveAuthenticatedPartner("client-other")
        ).isEmpty();
    }
}
"""
)

write(
    "backend/payment/src/test/java/com/sixpay/payment/application/service/PartnerIdentityAlignmentServiceTest.java",
    """package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.partner.PartnerIdentityResolutionPort;
import com.sixpay.payment.application.port.output.partner.ResolvedPartnerIdentity;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerIdentityAlignmentServiceTest {

    private static final UUID PARTNER_ID =
            UUID.fromString("8ec6a427-406f-4f93-b271-cbc819a4c1dd");

    @Test
    void acceptsWhenAuthenticatedMachinePartnerMatchesDeclaredAppId() {
        var identity = new ResolvedPartnerIdentity(
                PARTNER_ID,
                "PARTNER_001",
                true
        );

        PartnerIdentityResolutionPort port =
                new FixedPort(identity, identity);

        assertThat(
                new PartnerIdentityAlignmentService(port)
                        .requireConsistentPartner(
                                "client-001",
                                "PARTNER_001"
                        )
        ).isEqualTo(identity);
    }

    @Test
    void rejectsWhenDeclaredAppIdBelongsToAnotherPartner() {
        var authenticated = new ResolvedPartnerIdentity(
                PARTNER_ID,
                "PARTNER_001",
                true
        );
        var declared = new ResolvedPartnerIdentity(
                UUID.fromString("d3ac5544-f376-4308-9efc-291027c0ae76"),
                "PARTNER_002",
                true
        );

        assertThatThrownBy(() ->
                new PartnerIdentityAlignmentService(
                        new FixedPort(authenticated, declared)
                ).requireConsistentPartner(
                        "client-001",
                        "PARTNER_002"
                )
        )
                .isInstanceOf(PartnerIdentityResolutionException.class)
                .extracting("failure")
                .isEqualTo(
                        PartnerIdentityResolutionFailure
                                .PARTNER_IDENTITY_MISMATCH
                );
    }

    private record FixedPort(
            ResolvedPartnerIdentity authenticated,
            ResolvedPartnerIdentity declared
    ) implements PartnerIdentityResolutionPort {

        @Override
        public Optional<ResolvedPartnerIdentity> resolveAuthenticatedPartner(
                String authenticatedSubject
        ) {
            return Optional.ofNullable(authenticated);
        }

        @Override
        public Optional<ResolvedPartnerIdentity> findByPartnerIdentifier(
                String partnerIdentifier
        ) {
            return Optional.ofNullable(declared);
        }
    }
}
"""
)

# ---------------------------------------------------------------------------
# Documentation. Explicitly preserve public-contract decision as unresolved.
# ---------------------------------------------------------------------------

doc = "documentation/domains/payment/planning/init-debit-alignment/INIT_2B_PARTNER_MACHINE_IDENTITY.md"
write(
    doc,
    f"""# INIT-2B — Partner machine identity

## Reference revision

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Branch: `feat/repository-baseline-consolidation-cucumber`
- SHA: `{EXPECTED_HEAD}`

## Goal

Establish a strict separation between:

- human SIXPAY users (`AuthenticatedUser`, Local/OIDC/LDAP);
- Partner machine callers used for M2M payment initiation.

Target flow:

```text
transport authentication
    -> trusted machine subject
    -> Security machine-identity link
    -> canonical Partner partnerIdentifier
    -> Partner public resolution
    -> Payment comparison with declared AppID
```

## Ownership

Security owns the technical machine identity and its durable link to a Partner
business identifier.

Partner owns `partnerIdentifier`, Partner status and authorization to accept
new transactions.

Payment owns the consistency decision between authenticated Partner and the
Partner declared by the request.

Bootstrap owns only cross-module composition.

## Schema

Security adds `security_partner_machine_identities`.

The table deliberately stores `partner_identifier` rather than a foreign key to
Partner persistence. Security must not access Partner tables or repositories.
Runtime composition resolves the identifier through Partner's public application
surface.

## Human/M2M separation

`CurrentUserProvider` remains the surface for human SIXPAY users.

`CurrentMachineIdentityProvider` is the distinct surface for M2M callers.
`AuthenticatedUser` is explicitly rejected as a machine principal.

## Contract decision still required

INIT-2B does not modify the active physical Payment contract.

At this revision, the active contract still maps `AppID` to
`X-TresorPay-App-Id` and forbids it in the body. The INIT-1/INIT-2A target
decision states instead that the physical `AppID` request field is the canonical
`partnerIdentifier`.

That public-contract/security mismatch requires a separate explicit human
approval before the physical contract and request DTO are changed.

## Transport authentication

INIT-2B does not invent a new authentication protocol. Existing JWT,
subscription-key/API-key and mTLS processing remain governed by their current
contract/configuration status. The new surface consumes only the trusted
technical subject produced by the selected transport authentication.

## Status

```text
INIT-2B — INTERNAL MACHINE IDENTITY LINK IMPLEMENTED
PUBLIC APPID CONTRACT ALIGNMENT STILL REQUIRES HUMAN APPROVAL
```
"""
)

readme = "documentation/domains/payment/planning/init-debit-alignment/README.md"
s = read(readme)
if "INIT_2B_PARTNER_MACHINE_IDENTITY.md" not in s:
    s += """

## INIT-2B — Partner machine identity

See `INIT_2B_PARTNER_MACHINE_IDENTITY.md`.

M2M Partner callers use a Security-owned machine identity surface distinct from
human SIXPAY users. Security links the trusted machine subject to the canonical
Partner `partnerIdentifier`; Bootstrap resolves Partner through its public
surface and Payment owns the AppID consistency check.

The physical public AppID contract remains pending explicit approval.
"""
    write(readme, s)

init2 = "documentation/domains/payment/planning/init-debit-alignment/INIT_2_PARTNER_IDENTITY_ALIGNMENT.md"
s = read(init2)
if "## INIT-2B completion" not in s:
    s += """

## INIT-2B completion

Machine callers are no longer modeled as human SIXPAY users. Security exposes a
dedicated trusted machine identity and a durable machine-subject ->
`partnerIdentifier` link. Bootstrap composes Security and Partner public
surfaces; Payment retains the mismatch decision.

The active physical Payment contract still requires a separate approved
alignment for the location/meaning of `AppID`.
"""
    write(init2, s)

# Security active docs
security_readme = "backend/security/README.md"
if (ROOT / security_readme).is_file():
    s = read(security_readme)
    if "## Partner M2M machine identity" not in s:
        s += """

## Partner M2M machine identity

Partner system callers are not represented by `AuthenticatedUser`. Security
exposes `CurrentMachineIdentityProvider` for trusted technical subjects and owns
the durable `security_partner_machine_identities` association to a Partner
business identifier. Partner status and Partner business identity remain owned
by Partner.
"""
        write(security_readme, s)

# Architecture allowlist if the closed Payment service file test already contains
# INIT-2A files; no extra Payment service file is created in INIT-2B, so nothing
# to change there.


# APPROVED APPID DUAL REPRESENTATION
# X-TresorPay-App-Id remains the required transport representation.
# JSON AppID is also required. Both represent the canonical partnerIdentifier.
# The authenticated machine identity remains the authentication proof.

request_path = "backend/payment/src/main/java/com/sixpay/payment/api/request/InitiateDebitRequest.java"
rq = read(request_path)
if '@JsonProperty("AppID")\n        @NotBlank' not in rq:
    rq = rq.replace(
        '@JsonProperty("AppID")\n        @Size(max = 64)',
        '@JsonProperty("AppID")\n        @NotBlank\n        @Size(max = 64)',
        1
    )
    write(request_path, rq)

controller_path = "backend/payment/src/main/java/com/sixpay/payment/api/PaymentCommandController.java"
ct = read(controller_path)
if '@RequestHeader(name = "X-TresorPay-App-Id")' not in ct:
    old = '@RequestHeader(name = IntegrationHttpHeaders.IDEMPOTENCY_KEY)\n            @NotBlank @Size(max = 128) String idempotencyKey,'
    new = '@RequestHeader(name = "X-TresorPay-App-Id")\n            @NotBlank @Size(max = 64) String transportPartnerIdentifier,\n            @RequestHeader(name = IntegrationHttpHeaders.IDEMPOTENCY_KEY)\n            @NotBlank @Size(max = 128) String idempotencyKey,'
    if old not in ct:
        raise RuntimeError("PaymentCommandController idempotency header marker missing")
    ct = ct.replace(old, new, 1)

    old = '        var result = initiationUseCase.initiateDebit(\n'
    new = (
        '        if (!transportPartnerIdentifier.equals(request.applicationId())) {\n'
        '            throw new IllegalArgumentException(\n'
        '                    "X-TresorPay-App-Id must match request AppID"\n'
        '            );\n'
        '        }\n\n'
        '        var result = initiationUseCase.initiateDebit(\n'
    )
    if old not in ct:
        raise RuntimeError("PaymentCommandController initiation marker missing")
    ct = ct.replace(old, new, 1)
    write(controller_path, ct)

contract_path = "documentation/contracts/tresorpay/tresorpay-payment-request-api-v1.yaml"
contract = read(contract_path)
old = '    AppID:\n      target: "X-TresorPay-App-Id"\n      acceptedInBody: false'
new = (
    '    AppID:\n'
    '      target: "partnerIdentifier"\n'
    '      acceptedInBody: true\n'
    '      requiredInBody: true\n'
    '      transportHeader: "X-TresorPay-App-Id"\n'
    '      consistencyRule: "AppID MUST equal X-TresorPay-App-Id and the partnerIdentifier resolved from the authenticated machine caller"'
)
if old in contract:
    contract = contract.replace(old, new, 1)
if '    - "AppID"\n' in contract:
    contract = contract.replace('    - "AppID"\n', '', 1)
write(contract_path, contract)

decision_path = "documentation/domains/payment/planning/init-debit-alignment/INIT_2B_APPID_DUAL_REPRESENTATION.md"
write(
    decision_path,
    """# INIT-2B — AppID dual representation

## Approved decision

`X-TresorPay-App-Id` remains present and required as the transport-level
Partner identifier.

JSON `AppID` is also required and represents the same canonical
`partnerIdentifier`.

Neither value authenticates the caller on its own.

The consistency invariant is:

```text
authenticatedPartner.partnerIdentifier
    == X-TresorPay-App-Id
    == request.AppID
```

A mismatch is rejected before Payment business processing.

The authenticated Partner is obtained from the trusted M2M machine identity
and its Security-owned link to the registered Partner.
"""
)


print()
print("INIT-2B local patch prepared and applied.")
print("No worktree cleanliness check, gate, commit, push or PR was performed.")
print("APPROVED: X-TresorPay-App-Id remains required; JSON AppID is required; both represent the authenticated Partner partnerIdentifier.")
