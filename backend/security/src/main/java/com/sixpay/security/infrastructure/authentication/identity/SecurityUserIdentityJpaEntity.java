package com.sixpay.security.infrastructure.authentication.identity;

import com.sixpay.security.domain.authentication.AuthenticationIdentityType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_user_identities")
public class SecurityUserIdentityJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private SecurityUserAccountJpaEntity userAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false, length = 32)
    private AuthenticationIdentityType identityType;

    @Column(nullable = false, length = 500)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SecurityUserIdentityJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public SecurityUserAccountJpaEntity getUserAccount() {
        return userAccount;
    }

    public AuthenticationIdentityType getIdentityType() {
        return identityType;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
