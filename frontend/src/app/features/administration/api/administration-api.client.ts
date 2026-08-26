import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AdministrationOverviewResponse,
  AdministrationSettingsResponse,
  IntegrationStatusResponse,
} from '../models/administration.response';

const ADMINISTRATION_API_PATH =
  '/internal/api/v1/administration';

@Injectable({ providedIn: 'root' })
export class AdministrationApiClient {
  private readonly http = inject(HttpClient);

  overview(): Observable<AdministrationOverviewResponse> {
    return this.http.get<AdministrationOverviewResponse>(
      `${ADMINISTRATION_API_PATH}/overview`,
    );
  }

  settings(): Observable<AdministrationSettingsResponse> {
    return this.http.get<AdministrationSettingsResponse>(
      `${ADMINISTRATION_API_PATH}/settings`,
    );
  }

  integrations(): Observable<
    readonly IntegrationStatusResponse[]
  > {
    return this.http.get<
      readonly IntegrationStatusResponse[]
    >(
      `${ADMINISTRATION_API_PATH}/integrations`,
    );
  }
}
