package com.sixpay.administration.infrastructure.persistence;

import com.sixpay.administration.domain.model.SettingDomain;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dynamic_setting_history")
public class DynamicSettingHistoryJpaEntity {

    @Id
    @Column(name = "history_id", nullable = false)
    private UUID historyId;

    @Column(name = "setting_key", nullable = false, length = 192)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(name = "setting_domain", nullable = false, length = 32)
    private SettingDomain domain;

    @Column(name = "previous_value", length = 2048)
    private String previousValue;

    @Column(name = "new_value", nullable = false, length = 2048)
    private String newValue;

    @Column(name = "previous_version", nullable = false)
    private long previousVersion;

    @Column(name = "new_version", nullable = false)
    private long newVersion;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "changed_by", nullable = false, length = 256)
    private String changedBy;

    @Column(name = "change_reason", nullable = false, length = 1024)
    private String reason;

    @Column(name = "operation", nullable = false, length = 64)
    private String operation;

    protected DynamicSettingHistoryJpaEntity() {}

    public DynamicSettingHistoryJpaEntity(
            UUID historyId, String key, SettingDomain domain,
            String previousValue, String newValue,
            long previousVersion, long newVersion,
            Instant changedAt, String changedBy,
            String reason, String operation
    ) {
        this.historyId = historyId;
        this.key = key;
        this.domain = domain;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.previousVersion = previousVersion;
        this.newVersion = newVersion;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
        this.reason = reason;
        this.operation = operation;
    }

    public UUID getHistoryId() { return historyId; }
    public String getKey() { return key; }
    public SettingDomain getDomain() { return domain; }
    public String getPreviousValue() { return previousValue; }
    public String getNewValue() { return newValue; }
    public long getPreviousVersion() { return previousVersion; }
    public long getNewVersion() { return newVersion; }
    public Instant getChangedAt() { return changedAt; }
    public String getChangedBy() { return changedBy; }
    public String getReason() { return reason; }
    public String getOperation() { return operation; }
}
