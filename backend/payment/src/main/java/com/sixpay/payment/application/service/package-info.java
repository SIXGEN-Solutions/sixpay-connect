/**
 * Focused Payment workflow orchestration.
 *
 * <p>Services load or create one Payment Aggregate Root, invoke exactly one
 * domain operation and delegate atomic persistence to
 * {@link com.sixpay.payment.application.service.PaymentMutationCoordinator}.
 * External gateways, HTTP controllers and broker publication are outside this
 * package.</p>
 */
package com.sixpay.payment.application.service;
