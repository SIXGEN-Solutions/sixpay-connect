import {
  PartnerAuditPageResponse,
  PartnerResponse,
  PartnerStatusResponse,
} from '../models/partners.response';
import { Partner, PartnerAuditPage, PartnerStatusView } from '../models/partners';

export function mapPartnerResponse(response: PartnerResponse): Partner {
  return {
    ...response,
    statusReason: response.statusReason ?? null,
    validationThresholds: response.validationThresholds.map((threshold) => ({ ...threshold })),
    createdAt: new Date(response.createdAt),
    updatedAt: new Date(response.updatedAt),
  };
}

export function mapPartnerStatusResponse(response: PartnerStatusResponse): PartnerStatusView {
  return {
    ...response,
    statusReason: response.statusReason ?? null,
    connection: {
      ...response.connection,
      supportedAuthenticationMethods: [...response.connection.supportedAuthenticationMethods],
    },
    updatedAt: new Date(response.updatedAt),
  };
}

export function mapPartnerAuditPageResponse(response: PartnerAuditPageResponse): PartnerAuditPage {
  return {
    ...response,
    items: response.items.map((item) => ({
      ...item,
      occurredAt: new Date(item.occurredAt),
    })),
  };
}
