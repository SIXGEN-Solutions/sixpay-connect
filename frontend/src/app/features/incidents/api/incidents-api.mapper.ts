import {
  IncidentDetail,
  IncidentSummary,
  IncidentTimelineEntry,
} from '../models/incidents';
import {
  IncidentDetailResponse,
  IncidentSummaryResponse,
  IncidentTimelineEntryResponse,
} from '../models/incidents.response';

export function mapIncidentSummaryResponse(
  response: IncidentSummaryResponse,
): IncidentSummary {
  return {
    incidentId: response.incidentId,
    severity: response.severity,
    component: response.component,
    summary: response.summary,
    status: response.status,
    openedAt: new Date(
      response.openedAt,
    ),
    updatedAt: new Date(
      response.updatedAt,
    ),
  };
}

export function mapIncidentTimelineEntryResponse(
  response: IncidentTimelineEntryResponse,
): IncidentTimelineEntry {
  return {
    eventId: response.eventId,
    occurredAt: new Date(
      response.occurredAt,
    ),
    message: response.message,
    actor: response.actor,
  };
}

export function mapIncidentDetailResponse(
  response: IncidentDetailResponse,
): IncidentDetail {
  return {
    ...mapIncidentSummaryResponse(response),
    description: response.description,
    impact: response.impact,
    accountingBatchId:
      response.accountingBatchId,
    paymentId: response.paymentId,
    paymentReference:
      response.paymentReference,
    correlationId:
      response.correlationId,
    timeline: response.timeline.map(
      mapIncidentTimelineEntryResponse,
    ),
  };
}
