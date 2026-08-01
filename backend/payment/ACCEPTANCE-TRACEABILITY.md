# Traçabilité d’acceptation — Payment Lot 5

| ID | Critère | Preuve |
| --- | --- | --- |
| `PAY-L5-ACC-001` | 76 invariants conservés sans renumérotation | `PAYMENT_INVARIANT_CATALOGUE.yaml` |
| `PAY-L5-ACC-002` | moment de contrôle pour chaque invariant | champ `applicationStrategy.checkMoment` |
| `PAY-L5-ACC-003` | composant apportant la preuve | champ `proofProvider` |
| `PAY-L5-ACC-004` | méthode Aggregate Root concernée | champ `aggregateMethods` |
| `PAY-L5-ACC-005` | code d’erreur stable | champ `stableErrorCode` |
| `PAY-L5-ACC-006` | tests associés | champ `testRefs` |
| `PAY-L5-ACC-007` | impact audit | champ `auditImpact` |
| `PAY-L5-ACC-008` | index/contrainte DB ou intégration | champ `persistenceOrIntegrationConstraint` |
| `PAY-L5-ACC-009` | répartition aggregate/application/persistence/idempotency/integration | `PAYMENT_INVARIANT_RESPONSIBILITY_MATRIX.md` |
| `PAY-L5-ACC-010` | couverture des 22 invariants initiaux | section crosswalk de `PAYMENT_INVARIANT_FORMALIZATION.md` |
| `PAY-L5-ACC-011` | unicité externe hors Aggregate Root | `PAY-INV-003`, `PAY-INV-067`, `PAY-INV-068`, `PAY-INV-071` |
| `PAY-L5-ACC-012` | posting inconnu sans resoumission aveugle | `PAY-INV-042` |
| `PAY-L5-ACC-013` | audit/Outbox atomiques documentés | `PAY-INV-050`, `PAY-INV-074` |
| `PAY-L5-ACC-014` | aucun comportement Java modifié | périmètre documentaire du manifeste |
