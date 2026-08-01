/**
 * Payment idempotency persistence, replay and transaction-scoped concurrency
 * coordination.
 *
 * <p>This package contains no HTTP controller and no application workflow.
 * The future application layer must provide the canonical request
 * representation and invoke the coordinator and replay store inside the same
 * transaction as Payment, audit and outbox persistence.</p>
 */
package com.sixpay.payment.infrastructure.idempotency;
