import { IncidentSeverity, IncidentStatus } from './incidents';

export interface IncidentQuery {
  readonly severity?: IncidentSeverity;
  readonly status?: IncidentStatus;
  readonly component?: string;
}
