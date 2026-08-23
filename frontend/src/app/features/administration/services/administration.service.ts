import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import {
  BackendModeService,
} from '../../../core/backend/backend-mode.service';
import {
  AdministrationApiClient,
} from '../api/administration-api.client';
import {
  mapAdministrationOverviewResponse,
  mapAdministrationSettingsResponse,
  mapIntegrationStatusResponse,
} from '../api/administration-api.mapper';
import {
  AdministrationOverview,
  GeneralSettings,
  IntegrationStatus,
} from '../models/administration';
import {
  AdministrationMockService,
} from './administration-mock.service';

@Injectable({ providedIn: 'root' })
export class AdministrationService {
  private readonly backendMode =
    inject(BackendModeService);

  private readonly api =
    inject(AdministrationApiClient);

  private readonly mock =
    inject(AdministrationMockService);

  overview(): Observable<AdministrationOverview> {
    return this.backendMode.usesApi
      ? this.api
          .overview()
          .pipe(
            map(
              mapAdministrationOverviewResponse,
            ),
          )
      : this.mock.overview();
  }

  settings(): Observable<GeneralSettings> {
    return this.backendMode.usesApi
      ? this.api
          .settings()
          .pipe(
            map(
              mapAdministrationSettingsResponse,
            ),
          )
      : this.mock.settings();
  }

  integrations(): Observable<
    readonly IntegrationStatus[]
  > {
    return this.backendMode.usesApi
      ? this.api
          .integrations()
          .pipe(
            map((responses) =>
              responses.map(
                mapIntegrationStatusResponse,
              ),
            ),
          )
      : this.mock.integrations();
  }
}
