package com.sixpay.administration.infrastructure.persistence;

import com.sixpay.administration.domain.model.SettingDomain;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "dynamic_setting_value")
public class DynamicSettingJpaEntity {

    @Id
    @Column(name = "setting_key", nullable = false, length = 192)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(name = "setting_domain", nullable = false, length = 32)
    private SettingDomain domain;

    @Column(name = "setting_value", nullable = false, length = 2048)
    private String value;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 256)
    private String updatedBy;

    @Column(name = "change_reason", nullable = false, length = 1024)
    private String reason;

    protected DynamicSettingJpaEntity() {}

    public DynamicSettingJpaEntity(String key, SettingDomain domain, String value,
                                   Instant updatedAt, String updatedBy, String reason) {
        this.key = key;
        this.domain = domain;
        this.value = value;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.reason = reason;
    }

    public String getKey() { return key; }
    public SettingDomain getDomain() { return domain; }
    public String getValue() { return value; }
    public long getVersion() { return rowVersion + 1; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public String getReason() { return reason; }

    public void apply(SettingDomain domain, String value, Instant updatedAt,
                      String updatedBy, String reason) {
        this.domain = domain;
        this.value = value;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.reason = reason;
    }
}
