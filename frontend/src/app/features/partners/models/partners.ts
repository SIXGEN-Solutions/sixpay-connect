import { PartnerAuthenticationMethod, PartnerStatus } from './partners.response';

export interface ValidationThreshold {
  readonly transactionType: string;
  readonly currency: string;
  readonly amount: number;
  readonly validationLevels: number;
}

export interface Partner {
  readonly id: string;
  readonly legalName: string;
  readonly technicalContactName: string;
  readonly technicalContactEmail: string;
  readonly authorizedTransactionTypes: readonly string[];
  readonly status: PartnerStatus;
  readonly statusReason: string | null;
  readonly validationThresholds: readonly ValidationThreshold[];
  readonly createdAt: Date;
  readonly updatedAt: Date;
}

export interface PartnerConnectionInfo {
  readonly apiBasePath: string;
  readonly supportedAuthenticationMethods: readonly PartnerAuthenticationMethod[];
  readonly newTransactionsAllowed: boolean;
}

export interface PartnerStatusView {
  readonly partnerId: string;
  readonly status: PartnerStatus;
  readonly statusReason: string | null;
  readonly connection: PartnerConnectionInfo;
  readonly updatedAt: Date;
}

export interface PartnerAuditEntry {
  readonly partnerId: string;
  readonly action: string;
  readonly result: string;
  readonly actorId: string;
  readonly correlationId: string;
  readonly details: string;
  readonly occurredAt: Date;
}

export interface PartnerAuditPage {
  readonly items: readonly PartnerAuditEntry[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}
