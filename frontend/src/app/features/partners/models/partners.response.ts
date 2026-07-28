export const PARTNER_STATUSES = ['PENDING_VALIDATION', 'ACTIVE', 'REJECTED', 'SUSPENDED'] as const;

export type PartnerStatus = (typeof PARTNER_STATUSES)[number];

export const PARTNER_AUTHENTICATION_METHODS = ['MTLS', 'API_KEY'] as const;

export type PartnerAuthenticationMethod = (typeof PARTNER_AUTHENTICATION_METHODS)[number];

export interface ValidationThresholdResponse {
  readonly transactionType: string;
  readonly currency: string;
  readonly amount: number;
  readonly validationLevels: number;
}

export interface PartnerResponse {
  readonly id: string;
  readonly legalName: string;
  readonly technicalContactName: string;
  readonly technicalContactEmail: string;
  readonly authorizedTransactionTypes: readonly string[];
  readonly status: PartnerStatus;
  readonly statusReason?: string | null;
  readonly validationThresholds: readonly ValidationThresholdResponse[];
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface PartnerConnectionInfoResponse {
  readonly apiBasePath: string;
  readonly supportedAuthenticationMethods: readonly PartnerAuthenticationMethod[];
  readonly newTransactionsAllowed: boolean;
}

export interface PartnerStatusResponse {
  readonly partnerId: string;
  readonly status: PartnerStatus;
  readonly statusReason?: string | null;
  readonly connection: PartnerConnectionInfoResponse;
  readonly updatedAt: string;
}

export interface PartnerAuditResponse {
  readonly partnerId: string;
  readonly action: string;
  readonly result: string;
  readonly actorId: string;
  readonly correlationId: string;
  readonly details: string;
  readonly occurredAt: string;
}

export interface PartnerAuditPageResponse {
  readonly items: readonly PartnerAuditResponse[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}
