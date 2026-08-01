# Traçabilité d’acceptation — Payment Lot 7

| ID | Critère | Preuve |
| --- | --- | --- |
| `PAY-L7-ACC-001` | capacités définies sans imposer les tables physiques | `PAYMENT_CONCEPTUAL_PERSISTENCE.yaml` |
| `PAY-L7-ACC-002` | unicité référence TRESOR PAY | `PAY-PERS-001`, `PAY-IDX-002` |
| `PAY-L7-ACC-003` | unicité référence publique SIXPAY | `PAY-PERS-001`, `PAY-IDX-003` |
| `PAY-L7-ACC-004` | optimistic locking par `businessVersion` | brief section 7 |
| `PAY-L7-ACC-005` | atomicité Payment/audit/Outbox | `PAY-PERS-004`, `PAY-PERS-006` |
| `PAY-L7-ACC-006` | neuf besoins d’index documentés | `queryIndexes` |
| `PAY-L7-ACC-007` | références de compte protégées | `protectedData` |
| `PAY-L7-ACC-008` | aucun token ou credential brut | `rawTokensAllowed: false` |
| `PAY-L7-ACC-009` | conservation et immutabilité audit | `PAY-PERS-004`, `retention.audit` |
| `PAY-L7-ACC-010` | résolution durable des outcomes inconnus | `PAY-PERS-008` |
| `PAY-L7-ACC-011` | 8 scénarios de concurrence déterministes | `concurrencyScenarios` |
| `PAY-L7-ACC-012` | même clé/même payload : une mutation | `PAY-CONC-001` |
| `PAY-L7-ACC-013` | même référence/payload différent : conflit | `PAY-CONC-003` |
| `PAY-L7-ACC-014` | callbacks dupliqués : no-op ou conflit | `PAY-CONC-004`, `PAY-CONC-007` |
| `PAY-L7-ACC-015` | TFJ précoce durablement différé | `PAY-CONC-005` |
| `PAY-L7-ACC-016` | transition concurrente : reload et réévaluation | `PAY-CONC-008` |
| `PAY-L7-ACC-017` | aucune JPA entity ou migration générée | manifeste Lot 7 |
