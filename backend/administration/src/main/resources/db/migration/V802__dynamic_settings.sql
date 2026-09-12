CREATE TABLE dynamic_setting_value (
    setting_key VARCHAR(192) PRIMARY KEY,
    setting_domain VARCHAR(32) NOT NULL,
    setting_value VARCHAR(2048) NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(256) NOT NULL,
    change_reason VARCHAR(1024) NOT NULL
);

CREATE TABLE dynamic_setting_history (
    history_id UUID PRIMARY KEY,
    setting_key VARCHAR(192) NOT NULL,
    setting_domain VARCHAR(32) NOT NULL,
    previous_value VARCHAR(2048),
    new_value VARCHAR(2048) NOT NULL,
    previous_version BIGINT NOT NULL,
    new_version BIGINT NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    changed_by VARCHAR(256) NOT NULL,
    change_reason VARCHAR(1024) NOT NULL,
    operation VARCHAR(64) NOT NULL,
    CONSTRAINT uk_dynamic_setting_history_key_version UNIQUE (setting_key, new_version)
);

CREATE INDEX idx_dynamic_setting_history_key_changed_at
    ON dynamic_setting_history(setting_key, changed_at DESC);
