-- SIXPAY CONNECT canonical Administration Flyway baseline
-- This file creates the complete current Administration schema from an empty database.


-- ---------------------------------------------------------------------------
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS operational_incident (
    incident_id VARCHAR(64) PRIMARY KEY,
    severity VARCHAR(16) NOT NULL,
    component VARCHAR(128) NOT NULL,
    summary VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL,
    description VARCHAR(4096) NOT NULL,
    impact VARCHAR(2048) NOT NULL,
    accounting_batch_id UUID,
    payment_id UUID,
    payment_reference VARCHAR(64),
    correlation_id UUID,
    opened_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_operational_incident_severity
        CHECK (
            severity IN (
                'LOW',
                'MEDIUM',
                'HIGH',
                'CRITICAL'
            )
        ),

    CONSTRAINT ck_operational_incident_status
        CHECK (
            status IN (
                'OPEN',
                'INVESTIGATING',
                'MONITORING',
                'RESOLVED',
                'CLOSED'
            )
        ),

    CONSTRAINT ck_operational_incident_dates
        CHECK (updated_at >= opened_at)
);

CREATE TABLE IF NOT EXISTS operational_incident_timeline (
    event_id VARCHAR(64) PRIMARY KEY,
    incident_id VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    message VARCHAR(1024) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    sequence_no INTEGER NOT NULL,

    CONSTRAINT fk_operational_incident_timeline_incident
        FOREIGN KEY (incident_id)
        REFERENCES operational_incident(incident_id)
        ON DELETE CASCADE,

    CONSTRAINT ck_operational_incident_timeline_sequence
        CHECK (sequence_no >= 0),

    CONSTRAINT uk_operational_incident_timeline_sequence
        UNIQUE (incident_id, sequence_no)
);

CREATE INDEX IF NOT EXISTS idx_operational_incident_status_opened
    ON operational_incident (
        status,
        opened_at DESC
    );

CREATE INDEX IF NOT EXISTS idx_operational_incident_severity_opened
    ON operational_incident (
        severity,
        opened_at DESC
    );

CREATE INDEX IF NOT EXISTS idx_operational_incident_component
    ON operational_incident (
        lower(component)
    );

CREATE INDEX IF NOT EXISTS idx_operational_incident_timeline_incident
    ON operational_incident_timeline (
        incident_id,
        sequence_no
    );


-- ---------------------------------------------------------------------------
-- General parameters
-- ---------------------------------------------------------------------------

CREATE TABLE general_parameter (
    type_code VARCHAR(64) PRIMARY KEY,
    valeur_code VARCHAR(256) NOT NULL,
    description VARCHAR(512)
);


-- ---------------------------------------------------------------------------
-- Dynamic settings
-- ---------------------------------------------------------------------------

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
