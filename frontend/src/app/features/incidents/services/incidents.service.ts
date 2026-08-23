import {
  HttpErrorResponse,
} from '@angular/common/http';
import {
  inject,
  Injectable,
} from '@angular/core';
import {
  catchError,
  map,
  Observable,
  of,
  throwError,
} from 'rxjs';

import {
  BackendModeService,
} from '../../../core/backend/backend-mode.service';
import {
  IncidentsApiClient,
} from '../api/incidents-api.client';
import {
  mapIncidentDetailResponse,
  mapIncidentSummaryResponse,
} from '../api/incidents-api.mapper';
import {
  IncidentQuery,
} from '../models/incident-query';
import {
  IncidentDetail,
  IncidentSummary,
} from '../models/incidents';
import {
  IncidentsMockService,
} from './incidents-mock.service';

@Injectable({ providedIn: 'root' })
export class IncidentsService {
  private readonly backendMode =
    inject(BackendModeService);

  private readonly api =
    inject(IncidentsApiClient);

  private readonly mock =
    inject(IncidentsMockService);

  search(
    query: IncidentQuery,
  ): Observable<
    readonly IncidentSummary[]
  > {
    return this.backendMode.usesApi
      ? this.api
          .search(query)
          .pipe(
            map((page) =>
              page.content.map(
                mapIncidentSummaryResponse,
              ),
            ),
          )
      : this.mock.search(query);
  }

  get(
    incidentId: string,
  ): Observable<IncidentDetail | null> {
    return this.backendMode.usesApi
      ? this.api
          .get(incidentId)
          .pipe(
            map(
              mapIncidentDetailResponse,
            ),
            catchError(
              (error: unknown) => {
                if (
                  error
                    instanceof HttpErrorResponse
                  && error.status === 404
                ) {
                  return of(null);
                }

                return throwError(
                  () => error,
                );
              },
            ),
          )
      : this.mock.get(incidentId);
  }
}
