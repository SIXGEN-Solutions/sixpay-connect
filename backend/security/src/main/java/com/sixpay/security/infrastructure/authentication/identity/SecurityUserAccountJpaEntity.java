package com.sixpay.security.infrastructure.authentication.identity;

import com.sixpay.security.domain.authentication.SixpayUserAccountStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_user_accounts")
public class SecurityUserAccountJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String username;

    @Column(name = "normalized_username", nullable = false, unique = true, length = 150)
    private String normalizedUsername;

    @Column(length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SixpayUserAccountStatus status;

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
}
