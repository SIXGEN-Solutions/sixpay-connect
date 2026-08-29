export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type IncidentStatus = 'OPEN' | 'INVESTIGATING' | 'MONITORING' | 'RESOLVED' | 'CLOSED';

export interface IncidentTimelineEntry {
  readonly eventId: string;
  readonly occurredAt: Date;
  readonly message: string;
  readonly actor: string;
}

export interface IncidentSummary {
  readonly incidentId: string;
  readonly severity: IncidentSeverity;
  readonly component: string;
  readonly summary: string;
  readonly status: IncidentStatus;
  readonly openedAt: Date;
  readonly updatedAt: Date;
}

export interface IncidentDetail extends IncidentSummary {
  readonly description: string;
  readonly impact: string;
  readonly accountingBatchId: string | null;
  readonly paymentId: string | null;
  readonly paymentReference: string | null;
  readonly correlationId: string | null;
  readonly timeline: readonly IncidentTimelineEntry[];
}
