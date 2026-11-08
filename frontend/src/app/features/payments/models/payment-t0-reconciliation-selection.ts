import { PaymentSummary } from './payments';

/** Only the two uncertain posting states are eligible for T0 GET recovery. */
export function isT0RecoveryCandidate(payment: Pick<PaymentSummary, 'status'>): boolean {
  return payment.status === 'POSTING_PENDING' || payment.status === 'POSTING_OUTCOME_UNKNOWN';
}

/** Backend contract: one explicit, distinct selection of at most 50 payment UUIDs. */
export function validT0Selection(ids: readonly string[]): boolean {
  return ids.length >= 1 && ids.length <= 50 && new Set(ids).size === ids.length;
}
