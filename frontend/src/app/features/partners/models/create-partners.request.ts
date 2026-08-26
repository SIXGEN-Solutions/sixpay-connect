export const PARTNER_DECISIONS = ['APPROVE', 'REJECT'] as const;

export type PartnerDecision = (typeof PARTNER_DECISIONS)[number];

export interface CreatePartnerRequest {
  readonly legalName: string;
  readonly technicalContactName: string;
  readonly technicalContactEmail: string;
  readonly authorizedTransactionTypes: readonly string[];
}

export interface PartnerDecisionRequest {
  readonly decision: PartnerDecision;
  readonly reason?: string | null;
}

export interface SuspendPartnerRequest {
  readonly reason: string;
}

export interface ConfigureValidationThresholdRequest {
  readonly currency: string;
  readonly amount: number;
  readonly validationLevels: number;
}

export interface PartnerAuditQuery {
  readonly from: string;
  readonly to: string;
  readonly page?: number;
  readonly size?: number;
}
