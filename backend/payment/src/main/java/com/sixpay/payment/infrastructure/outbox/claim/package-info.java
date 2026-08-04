/**
 * Short-transaction claiming of Payment outbox events.
 *
 * <p>Rows are locked with PostgreSQL {@code SKIP LOCKED}, transitioned to
 * processing and returned as immutable detached claims. Event delivery is
 * deliberately outside the claim transaction.</p>
 */
package com.sixpay.payment.infrastructure.outbox.claim;
