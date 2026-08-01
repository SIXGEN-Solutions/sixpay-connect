/**
 * Technology-neutral application orchestration boundary of the Payment module.
 *
 * <p>This package will contain commands, queries, views, input and output ports,
 * and cohesive workflow services. It may coordinate the frozen Payment Domain
 * Kernel but must not duplicate domain decisions or depend on HTTP, JPA, broker
 * implementations or bank-specific adapters.</p>
 */
package com.sixpay.payment.application;
