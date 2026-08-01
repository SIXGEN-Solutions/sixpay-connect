/**
 * Versioned Payment integration-event boundary.
 *
 * <p>Integration events are explicit external contracts and are distinct from
 * domain events. They must expose safe payloads only and must never serialize
 * the Payment aggregate, persistence entities or internal evidence snapshots.</p>
 */
package com.sixpay.payment.events;
