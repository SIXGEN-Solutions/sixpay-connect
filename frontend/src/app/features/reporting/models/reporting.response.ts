export const TIMELINE_CATEGORIES = [
  'DOMAIN',
  'BANKING_VERIFICATION',
  'ACCOUNTING',
  'NOTIFICATION',
  'TFJ',
  'REVERSAL',
] as const;

export type TimelineCategoryResponse = (typeof TIMELINE_CATEGORIES)[number];

export type PaymentStateResponse =
  | 'RECEIVED'
  | 'AUTHORIZATION_CHECKING'
  | 'BANKING_CHECKING'
  | 'REJECTED'
  | 'APPROVED'
  | 'POSTING'
  | 'ACCOUNTING_OUTCOME_UNKNOWN'
  | 'DEBITED'
  | 'CUT_CREDITED'
  | 'REVERSAL_REQUIRED'
  | 'REVERSAL_PENDING'
  | 'REVERSED'
  | 'FAILED'
  | 'NOTIFIED'
  | 'PENDING_END_OF_DAY_CONFIRMATION'
  | 'TREASURY_INTEGRATED';

export interface PaymentTimelineEntryResponse {
  readonly timelineEntryId: string;
  readonly paymentId: string;
  readonly category: TimelineCategoryResponse;
  readonly eventType: string;
  readonly fromState?: PaymentStateResponse | null;
  readonly toState?: PaymentStateResponse | null;
  readonly result?: 'SUCCESS' | 'FAILURE' | 'DEFERRED' | 'UNKNOWN' | 'PARTIAL' | 'NO_OP' | 'QUARANTINED';
  readonly reasonCode?: string | null;
  readonly occurredAt: string;
  readonly correlationId: string;
  readonly sourceSystem: 'SIXPAY' | 'TRESOR_PAY' | 'AMPLITUDE';
  readonly externalReference?: string | null;
  readonly aggregateVersion: number;
  readonly metadata?: Readonly<Record<string, string | number | boolean | null>>;
}

export interface PaymentTimelinePageResponse {
  readonly items: readonly PaymentTimelineEntryResponse[];
  readonly size: number;
  readonly hasMore: boolean;
  readonly nextCursor?: string | null;
  readonly snapshotAt: string;
}

export interface AuditActorResponse {
  readonly actorType: 'USER' | 'SERVICE' | 'SYSTEM' | 'EXTERNAL_SYSTEM';
  readonly actorId: string;
  readonly roles?: readonly string[];
}

export interface IntegrityEvidenceResponse {
  readonly scheme: 'HASH_CHAIN' | 'SIGNATURE' | 'WORM_REFERENCE';
  readonly value: string;
}

export interface PaymentAuditRecordResponse {
  readonly auditId: string;
  readonly occurredAt: string;
  readonly actor: AuditActorResponse;
  readonly action: string;
  readonly targetType:
    | 'PAYMENT'
    | 'OBSERVED_CUSTOMER'
    | 'BANKING_VERIFICATION'
    | 'POSTING'
    | 'NOTIFICATION'
    | 'TFJ_CONFIRMATION'
    | 'REVERSAL'
    | 'AUDIT_QUERY'
    | 'AUDIT_EXPORT';
  readonly targetId: string;
  readonly paymentId?: string | null;
  readonly paymentReference?: string | null;
  readonly observedCustomerId?: string | null;
  readonly result: 'SUCCESS' | 'FAILURE' | 'DENIED' | 'NO_OP' | 'QUARANTINED';
  readonly reasonCode: string;
  readonly correlationId: string;
  readonly traceId?: string | null;
  readonly sourceSystem: 'SIXPAY' | 'TRESOR_PAY' | 'AMPLITUDE';
  readonly beforeState?: PaymentStateResponse | null;
  readonly afterState?: PaymentStateResponse | null;
  readonly metadata?: Readonly<Record<string, string | number | boolean | null>>;
  readonly integrityEvidence: IntegrityEvidenceResponse;
}

export interface PaymentAuditPageResponse {
  readonly items: readonly PaymentAuditRecordResponse[];
  readonly size: number;
  readonly hasMore: boolean;
  readonly nextCursor?: string | null;
  readonly snapshotAt: string;
}

export interface PaymentAuditExportRequest {
  readonly occurredFrom: string;
  readonly occurredTo: string;
  readonly paymentIds?: readonly string[];
  readonly financialInstitutionCodes?: readonly string[];
  readonly actions?: readonly string[];
  readonly results?: readonly ('SUCCESS' | 'FAILURE' | 'DENIED' | 'NO_OP' | 'QUARANTINED')[];
  readonly businessPurpose: string;
  readonly format: 'CSV' | 'JSONL';
}

export interface PaymentAuditExportJobResponse {
  readonly exportId: string;
  readonly status: 'ACCEPTED' | 'GENERATING' | 'AVAILABLE' | 'FAILED' | 'EXPIRED';
  readonly requestedAt: string;
  readonly requestedBy: string;
  readonly businessPurpose: string;
  readonly recordCount?: number | null;
  readonly checksum?: string | null;
  readonly retrievalUri?: string | null;
  readonly expiresAt: string;
  readonly failureCode?: string | null;
}
