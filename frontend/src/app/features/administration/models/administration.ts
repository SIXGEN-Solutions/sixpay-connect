export type IntegrationHealth = 'AVAILABLE' | 'DEGRADED' | 'UNAVAILABLE' | 'UNKNOWN';

export interface GeneralSettings {
  readonly accountingBatchSize: number;
  readonly paymentTimeoutMs: number;
  readonly operationalRetentionDays: number;
  readonly maxPaymentAmountXaf: number;
  readonly maintenanceMode: boolean;
  readonly updatedAt: Date;
  readonly updatedBy: string;
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
}
