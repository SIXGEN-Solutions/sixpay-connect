/**
 * Phase 7.2 intentionally keeps Payment on the mock datasource.
 *
 * The real HttpClient-backed implementation of the published
 * payment-query-api-v1 contract is introduced in Phase 7.7.
 * Components must depend on PaymentsService and never on HttpClient directly.
 */
export {};
