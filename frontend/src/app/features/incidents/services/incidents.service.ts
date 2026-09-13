import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, throwError } from 'rxjs';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { IncidentsApiClient } from '../api/incidents-api.client';
import { mapIncidentDetailResponse } from '../api/incidents-api.mapper';
import { IncidentQuery } from '../models/incident-query';
import { IncidentDetail } from '../models/incidents';
import { IncidentPageResponse } from '../models/incidents.response';
import { IncidentsMockService } from './incidents-mock.service';

@Injectable({ providedIn: 'root' })
export class IncidentsService {
  private readonly backendMode = inject(BackendModeService);

  private readonly api = inject(IncidentsApiClient);

  private readonly mock = inject(IncidentsMockService);

  search(query: IncidentQuery): Observable<IncidentPageResponse> {
    return this.backendMode.usesApi
      ? this.api.search(query)
      : this.mock.search(query).pipe(
          map((content) => ({
            content: content.map((incident) => ({
              incidentId: incident.incidentId,
              severity: incident.severity,
              component: incident.component,
              summary: incident.summary,
              status: incident.status,
              openedAt: incident.openedAt.toISOString(),
              updatedAt: incident.updatedAt.toISOString(),
            })),
            page: query.page ?? 0,
            size: query.size ?? 20,
            totalElements: content.length,
            totalPages: content.length === 0 ? 0 : 1,
          })),
        );
  }

  get(incidentId: string): Observable<IncidentDetail | null> {
    return this.backendMode.usesApi
      ? this.api.get(incidentId).pipe(
          map(mapIncidentDetailResponse),
          catchError((error: unknown) => {
            if (error instanceof HttpErrorResponse && error.status === 404) {
              return of(null);
            }

            return throwError(() => error);
          }),
        )
      : this.mock.get(incidentId);
  }
}
