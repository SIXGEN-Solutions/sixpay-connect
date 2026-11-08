import { isT0RecoveryCandidate, validT0Selection } from './payment-t0-reconciliation-selection';

describe('T0 manual reconciliation selection', () => {
  it('accepts only uncertain posting statuses, not reversal or terminal states', () => {
    expect(isT0RecoveryCandidate({ status: 'POSTING_PENDING' })).toBe(true);
    expect(isT0RecoveryCandidate({ status: 'POSTING_OUTCOME_UNKNOWN' })).toBe(true);
    expect(isT0RecoveryCandidate({ status: 'REVERSAL_OUTCOME_UNKNOWN' })).toBe(false);
    expect(isT0RecoveryCandidate({ status: 'POSTED_PENDING_TFJ' })).toBe(false);
    expect(isT0RecoveryCandidate({ status: 'REJECTED' })).toBe(false);
  });

  it('rejects empty, duplicate, and oversized selections', () => {
    expect(validT0Selection([])).toBe(false);
    expect(validT0Selection(['one', 'one'])).toBe(false);
    expect(validT0Selection(Array.from({ length: 51 }, (_, i) => String(i)))).toBe(false);
    expect(validT0Selection(Array.from({ length: 50 }, (_, i) => String(i)))).toBe(true);
  });
});
