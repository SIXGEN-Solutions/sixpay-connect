/** R3-approved operator command response; statuses remain SIXPAY domain statuses. */
export type PaymentT0ManualOutcome =
  | 'NOT_ELIGIBLE_OR_BUSY'
  | 'RETRY_SCHEDULED'
  | 'RESOLVED'
  | 'STALE_CLAIM'
  | 'LOOKUP_FAILED_RETRY_SCHEDULED';

export interface PaymentT0ManualResult {
  readonly paymentId: string;
  readonly outcome: PaymentT0ManualOutcome;
  readonly paymentStatus: string | null;
}

export interface PaymentT0ManualResponse {
  readonly results: readonly PaymentT0ManualResult[];
}
