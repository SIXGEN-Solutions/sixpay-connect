import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { IncidentQuery } from '../models/incident-query';
import { IncidentDetailResponse, IncidentPageResponse } from '../models/incidents.response';

const INCIDENTS_API = '/internal/api/v1/incidents';

@Injectable({ providedIn: 'root' })
export class IncidentsApiClient {
  private readonly http = inject(HttpClient);

  search(query: IncidentQuery): Observable<IncidentPageResponse> {
    let params = new HttpParams()
      .set('page', String(query.page ?? 0))
      .set('size', String(query.size ?? 20));

    if (query.severity) {
      params = params.set('severity', query.severity);
    }

    if (query.status) {
      params = params.set('status', query.status);
    }

    const component = query.component?.trim();

    if (component) {
      params = params.set('component', component);
    }

    return this.http.get<IncidentPageResponse>(INCIDENTS_API, { params });
  }

  get(incidentId: string): Observable<IncidentDetailResponse> {
    return this.http.get<IncidentDetailResponse>(
      `${INCIDENTS_API}/${encodeURIComponent(incidentId)}`,
    );
  }
}
