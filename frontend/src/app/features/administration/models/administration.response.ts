import { IntegrationHealth } from './administration';

export interface AdministrationSettingsResponse {
  readonly accountingCutoffZone: string;
  readonly accountingCutoffTime: string;
}

export interface IntegrationStatusResponse {
  readonly integrationId: string;
  readonly name: string;
  readonly type: string;
  readonly health: IntegrationHealth;
  readonly detail: string;
  readonly lastSuccessfulAt: string | null;
  readonly lastCheckedAt: string;
}

export interface AdministrationOverviewResponse {
  readonly settings: AdministrationSettingsResponse;
  readonly integrations: readonly IntegrationStatusResponse[];
  readonly observedAt: string;
}
