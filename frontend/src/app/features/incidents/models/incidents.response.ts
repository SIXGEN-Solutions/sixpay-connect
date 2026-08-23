import {
  IncidentSeverity,
  IncidentStatus,
} from './incidents';

export interface IncidentSummaryResponse {
  readonly incidentId: string;
  readonly severity: IncidentSeverity;
  readonly component: string;
  readonly summary: string;
  readonly status: IncidentStatus;
  readonly openedAt: string;
  readonly updatedAt: string;
}

export interface IncidentTimelineEntryResponse {
  readonly eventId: string;
  readonly occurredAt: string;
  readonly message: string;
  readonly actor: string;
}

export interface IncidentDetailResponse
  extends IncidentSummaryResponse {
  readonly description: string;
  readonly impact: string;
  readonly accountingBatchId: string | null;
  readonly paymentId: string | null;
  readonly paymentReference: string | null;
  readonly correlationId: string | null;
  readonly timeline:
    readonly IncidentTimelineEntryResponse[];
}

export interface IncidentPageResponse {
  readonly content:
    readonly IncidentSummaryResponse[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}
