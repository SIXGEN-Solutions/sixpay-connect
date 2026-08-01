# Traçabilité d’acceptation — Payment Lot 6

| ID | Critère | Preuve |
| --- | --- | --- |
| `PAY-L6-ACC-001` | 33 Domain Events conservés sans duplication | `PAYMENT_EVENT_CATALOG.yaml` |
| `PAY-L6-ACC-002` | payload minimal et classification pour chaque événement | catalogue événementiel et matrice de publication |
| `PAY-L6-ACC-003` | denylist globale et interdictions spécifiques | `sensitiveDataDenylist` et `sensitiveDataForbidden` |
| `PAY-L6-ACC-004` | version Payment présente dans chaque événement | `aggregateVersion` obligatoire |
| `PAY-L6-ACC-005` | ordre déterministe | `aggregateVersion,eventSequence` |
| `PAY-L6-ACC-006` | déduplication | `eventId` |
| `PAY-L6-ACC-007` | audit append-only par Domain Event | `PAYMENT_AUDIT_RECORD_SCHEMA.yaml` |
| `PAY-L6-ACC-008` | Payment + audit + Outbox atomiques | `transactionBoundary` |
| `PAY-L6-ACC-009` | Domain Event distinct de Integration Event | `domainToIntegrationEventPolicy` |
| `PAY-L6-ACC-010` | publication directe Kafka interdite | `aggregateDirectKafkaCallAllowed: false` |
| `PAY-L6-ACC-011` | échec Kafka après commit sans rollback Payment | `publisherFailureAfterCommit` |
| `PAY-L6-ACC-012` | republication avec identité inchangée | `republishRule` |
| `PAY-L6-ACC-013` | consommateurs idempotents | `consumerGuarantee` |
| `PAY-L6-ACC-014` | chaque événement publiable est reproductible | `lot6Publication.reproducibleFrom` |
| `PAY-L6-ACC-015` | aucun comportement Java modifié | périmètre documentaire du manifeste |
