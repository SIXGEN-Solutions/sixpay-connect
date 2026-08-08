import { PaymentStateResponse, TimelineCategoryResponse } from './reporting.response';

export interface PaymentTimelineEntry {
  readonly timelineEntryId: string;
  readonly paymentId: string;
  readonly category: TimelineCategoryResponse;
  readonly eventType: string;
  readonly fromState: PaymentStateResponse | null;
  readonly toState: PaymentStateResponse | null;
  readonly result: string | null;
  readonly reasonCode: string | null;
  readonly occurredAt: Date;
  readonly correlationId: string;
  readonly sourceSystem: 'SIXPAY' | 'TRESOR_PAY' | 'AMPLITUDE';
  readonly externalReference: string | null;
  readonly aggregateVersion: number;
  readonly metadata: Readonly<Record<string, string | number | boolean | null>>;
}

export interface PaymentTimelinePage {
  readonly items: readonly PaymentTimelineEntry[];
  readonly size: number;
  readonly hasMore: boolean;
  readonly nextCursor: string | null;
  readonly snapshotAt: Date;
}

export interface PaymentAuditRecord {
  readonly auditId: string;
  readonly occurredAt: Date;
  readonly actorType: 'USER' | 'SERVICE' | 'SYSTEM' | 'EXTERNAL_SYSTEM';
  readonly actorId: string;
  readonly actorRoles: readonly string[];
  readonly action: string;
  readonly targetType: string;
  readonly targetId: string;
  readonly paymentId: string | null;
  readonly paymentReference: string | null;
  readonly observedCustomerId: string | null;
  readonly result: 'SUCCESS' | 'FAILURE' | 'DENIED' | 'NO_OP' | 'QUARANTINED';
  readonly reasonCode: string;
  readonly correlationId: string;
  readonly traceId: string | null;
  readonly sourceSystem: 'SIXPAY' | 'TRESOR_PAY' | 'AMPLITUDE';
  readonly beforeState: PaymentStateResponse | null;
  readonly afterState: PaymentStateResponse | null;
  readonly metadata: Readonly<Record<string, string | number | boolean | null>>;
  readonly integrityScheme: 'HASH_CHAIN' | 'SIGNATURE' | 'WORM_REFERENCE';
  readonly integrityValue: string;
}

export interface PaymentAuditPage {
  readonly items: readonly PaymentAuditRecord[];
  readonly size: number;
  readonly hasMore: boolean;
  readonly nextCursor: string | null;
  readonly snapshotAt: Date;
}

export interface PaymentAuditExportJob {
  readonly exportId: string;
  readonly status: 'ACCEPTED' | 'GENERATING' | 'AVAILABLE' | 'FAILED' | 'EXPIRED';
  readonly requestedAt: Date;
  readonly requestedBy: string;
  readonly businessPurpose: string;
  readonly recordCount: number | null;
  readonly checksum: string | null;
  readonly retrievalUri: string | null;
  readonly expiresAt: Date;
  readonly failureCode: string | null;
}
