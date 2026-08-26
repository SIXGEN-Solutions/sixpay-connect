/**
 * Pure Payment Domain Services coordinating policy decisions across immutable
 * contexts and evidence.
 *
 * <p>Services return typed decisions only. They never mutate Payment, persist,
 * publish, call external systems or read the system clock.</p>
 */
package com.sixpay.payment.domain.service;
