/**
 * Stable, versioned Payment-owned event contracts used to feed read-side
 * projections.
 *
 * <p>Types input this package:</p>
 * <ul>
 *     <li>contain no dependency on another business module;</li>
 *     <li>contain no framework, persistence or transport dependency;</li>
 *     <li>use only stable primitives, JDK value types and Payment-owned
 *     enums;</li>
 *     <li>represent the Payment state captured at source-event creation
 *     time;</li>
 *     <li>must remain backward compatible for each published event
 *     version.</li>
 * </ul>
 */
package com.sixpay.payment.application.event.projection;