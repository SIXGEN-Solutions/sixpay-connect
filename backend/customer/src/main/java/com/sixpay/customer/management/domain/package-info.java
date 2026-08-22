/**
 * Customer Management owns explicitly enrolled SIXPAY customers and their
 * linked bank-account identities.
 *
 * <p>This domain stays separate from customer.observation and
 * customer.verification. Amplitude remains authoritative for banking facts;
 * SIXPAY owns the enrollment lifecycle.</p>
 */
package com.sixpay.customer.management.domain;
