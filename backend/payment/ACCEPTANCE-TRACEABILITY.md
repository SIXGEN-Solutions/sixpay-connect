# Traçabilité d’acceptation — Payment Lot 3.5

| ID | Critère | Preuve automatisée |
| --- | --- | --- |
| `PAY-L3.5-ACC-001` | création version 1 + `PaymentReceived` | `PaymentCreationAndReconstitutionTest` |
| `PAY-L3.5-ACC-002` | reconstitution sans transition ni événement | `reconstitutionRestoresStateWithoutEventOrMutation` |
| `PAY-L3.5-ACC-003` | progression favorable jusqu’à `APPROVED_FOR_POSTING` | `PaymentPreFinancialLifecycleTest` |
| `PAY-L3.5-ACC-004` | replay identique strictement no-op | tests authorization, posting et TFJ |
| `PAY-L3.5-ACC-005` | conflit/transition invalide sans mutation | tests préfinanciers et posting |
| `PAY-L3.5-ACC-006` | posting complet → résultat immédiat + TFJ tracking | `PaymentPostingLifecycleTest` |
| `PAY-L3.5-ACC-007` | outcome inconnu résolu uniquement autoritativement | `unknownPostingCanOnlyBeResolvedByAuthoritativeEvidence` |
| `PAY-L3.5-ACC-008` | finalité uniquement par TFJ intégré et match unique | `PaymentTfjAndReversalLifecycleTest` |
| `PAY-L3.5-ACC-009` | reversal explicite, original posting préservé | `postingReversalRequirementCanBeAuthorizedAndConfirmed` |
| `PAY-L3.5-ACC-010` | exactement 33 événements stables | `PaymentEventCatalogueTest` |
| `PAY-L3.5-ACC-011` | aucun événement ne contient Aggregate/State/Snapshot | `explicitEventsNeverContainWholeAggregateOrSnapshotFields` |
| `PAY-L3.5-ACC-012` | 17 opérations nommées, aucun setter/dispatcher générique | `PaymentArchitectureTest` |
| `PAY-L3.5-ACC-013` | domaine sans framework, I/O, persistence ou adapter | `paymentDomainRemainsFrameworkAndIoFree` |
| `PAY-L3.5-ACC-014` | autorisation limitée au Lot 3.5 | `lot35AuthorizationIsActiveAndGlobalGenerationIsFalse` |
