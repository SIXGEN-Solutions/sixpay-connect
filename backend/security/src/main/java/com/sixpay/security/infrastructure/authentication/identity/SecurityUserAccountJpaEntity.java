package com.sixpay.security.infrastructure.authentication.identity;

import com.sixpay.security.domain.authentication.SixpayUserAccount;
import com.sixpay.security.domain.authentication.SixpayUserAccountStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "security_user_accounts")
public class SecurityUserAccountJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String username;

    @Column(
            name = "normalized_username",
            nullable = false,
            unique = true,
            length = 150
    )
    private String normalizedUsername;

    @Column(length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SixpayUserAccountStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "security_user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role", nullable = false, length = 100)
    private Set<String> roles = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "security_user_permissions",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "permission", nullable = false, length = 150)
    private Set<String> permissions = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected SecurityUserAccountJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public SixpayUserAccountStatus getStatus() {
        return status;
    }

    public Set<String> getRoles() {
        return Set.copyOf(roles);
    }

    public Set<String> getPermissions() {
        return Set.copyOf(permissions);
    }

    public Set<String> getAuthorities() {
        return toDomain().authorities();
    }

    public SixpayUserAccount toDomain() {
        return new SixpayUserAccount(
                id,
                username,
                email,
                status,
                roles,
                permissions
        );
    }
}
