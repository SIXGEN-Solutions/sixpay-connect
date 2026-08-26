/**
 * The 33 explicit, immutable and minimized Payment domain events.
 *
 * <p>Every event carries a common metadata object, uses its Java simple class
 * name as stable event type and contains only an approved safe payload. No
 * snapshot or Aggregate Root is serialized automatically.</p>
 */
package com.sixpay.payment.domain.event;
