package com.sixpay.security.infrastructure.authentication.machine;

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
