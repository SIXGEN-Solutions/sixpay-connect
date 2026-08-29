/**
 * Immutable identifiers, Value Objects and closed classifications protected by
 * the Payment domain.
 *
 * <p>Types input this package validate structural invariants only. Contextual
 * decisions involving aggregate state, authoritative evidence or bank
 * configuration remain aggregate invariants or domain policies.</p>
 *
 * <p>The package reuses {@code com.sixpay.common.context.CorrelationId} and
 * {@code com.sixpay.sharedkernel.domain.valueobject.Money}; local duplicates
 * are forbidden.</p>
 */
package com.sixpay.payment.domain.model;
