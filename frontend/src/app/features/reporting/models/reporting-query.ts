import { TimelineCategoryResponse } from './reporting.response';

export interface PaymentTimelineQuery {
  readonly category?: TimelineCategoryResponse;
  readonly occurredFrom?: Date;
  readonly occurredTo?: Date;
  readonly cursor?: string;
  readonly size?: number;
}

export interface PaymentAuditQuery {
  readonly paymentId?: string;
  readonly paymentReference?: string;
  readonly observedCustomerId?: string;
  readonly actorId?: string;
  readonly actorType?: 'USER' | 'SERVICE' | 'SYSTEM' | 'EXTERNAL_SYSTEM';
  readonly action?: string;
  readonly result?: 'SUCCESS' | 'FAILURE' | 'DENIED' | 'NO_OP' | 'QUARANTINED';
  readonly reasonCode?: string;
  readonly correlationId?: string;
  readonly sourceSystem?: 'SIXPAY' | 'TRESOR_PAY' | 'AMPLITUDE';
  readonly occurredFrom: Date;
  readonly occurredTo: Date;
  readonly sort?: 'OCCURRED_AT_ASC' | 'OCCURRED_AT_DESC';
  readonly cursor?: string;
  readonly size?: number;
}
