/**
 * Append-only Payment audit persistence.
 *
 * <p>Only safe Payment domain-event metadata is stored. Audit writes require
 * an existing transaction and perform no workflow orchestration.</p>
 */
package com.sixpay.payment.infrastructure.audit;
