import {
  AdministrationOverview,
  GeneralSettings,
  IntegrationStatus,
} from '../models/administration';
import {
  AdministrationOverviewResponse,
  AdministrationSettingsResponse,
  IntegrationStatusResponse,
} from '../models/administration.response';

export function mapAdministrationSettingsResponse(
  response: AdministrationSettingsResponse,
): GeneralSettings {
  return {
    accountingCutoffZone: response.accountingCutoffZone,
    accountingCutoffTime: response.accountingCutoffTime,
  };
}

export function mapIntegrationStatusResponse(
  response: IntegrationStatusResponse,
): IntegrationStatus {
  return {
    integrationId: response.integrationId,
    name: response.name,
    type: response.type,
    health: response.health,
    detail: response.detail,
    lastSuccessfulAt:
      response.lastSuccessfulAt === null ? null : new Date(response.lastSuccessfulAt),
    lastCheckedAt: new Date(response.lastCheckedAt),
  };
}

export function mapAdministrationOverviewResponse(
  response: AdministrationOverviewResponse,
): AdministrationOverview {
  return {
    settings: mapAdministrationSettingsResponse(response.settings),
    integrations: response.integrations.map(mapIntegrationStatusResponse),
    observedAt: new Date(response.observedAt),
  };
}
