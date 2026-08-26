export type IntegrationHealth =
  | 'AVAILABLE'
  | 'DEGRADED'
  | 'UNAVAILABLE'
  | 'UNKNOWN';

export interface GeneralSettings {
  readonly accountingCutoffZone: string;
  readonly accountingCutoffTime: string;
}

export interface IntegrationStatus {
  readonly integrationId: string;
  readonly name: string;
  readonly type: string;
  readonly health: IntegrationHealth;
  readonly detail: string;
  readonly lastSuccessfulAt: Date | null;
  readonly lastCheckedAt: Date;
}

export interface AdministrationOverview {
  readonly settings: GeneralSettings;
  readonly integrations: readonly IntegrationStatus[];
  readonly observedAt: Date;
}
