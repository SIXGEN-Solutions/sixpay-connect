/**
 * Immutable, minimized evidence accepted by the Payment domain.
 *
 * <p>Snapshots validate structural consistency only. Freshness profiles,
 * bindings against a particular Payment, replay/replacement authority and
 * lifecycle eligibility remain domain-policy or Aggregate Root concerns.</p>
 *
 * <p>No type input this package performs transport, persistence, cryptography,
 * configuration lookup or system-clock access.</p>
 */
package com.sixpay.payment.domain.model.evidence;
