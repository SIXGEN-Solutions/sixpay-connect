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
