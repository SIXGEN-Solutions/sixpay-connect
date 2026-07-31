# Traçabilité d’acceptation — Payment Lot 3.3

| ID | Critère | Preuve automatisée |
| --- | --- | --- |
| `PAY-L3.3-ACC-001` | métadonnées complètes et chronologie valide | `EvidenceMetadataTest` |
| `PAY-L3.3-ACC-002` | autorisation minimisée et matrice cohérente | `AuthorizationEvidenceSnapshotTest` |
| `PAY-L3.3-ACC-003` | checks bancaires uniques, canoniques et cohérents | `BankingAndFundsSnapshotsTest` |
| `PAY-L3.3-ACC-004` | fonds positifs, valides et sans solde disponible | `BankingAndFundsSnapshotsTest` |
| `PAY-L3.3-ACC-005` | résolution Treasury issue de configuration protégée | `TreasuryAndPostingSnapshotsTest` |
| `PAY-L3.3-ACC-006` | cinq outcomes de posting cohérents | `TreasuryAndPostingSnapshotsTest` |
| `PAY-L3.3-ACC-007` | références de jambes cohérentes | `bankAndLegReferencesMustBeConsistent` |
| `PAY-L3.3-ACC-008` | TFJ n’accepte que `INTEGRATED` ou `FAILED` | `EndOfDayAndReversalSnapshotsTest` |
| `PAY-L3.3-ACC-009` | reversal conserve les identités originales | `reversalSnapshotPreservesOriginalInstructionAndAuthorization` |
| `PAY-L3.3-ACC-010` | matrice des outcomes de reversal | `reversalOutcomeMatrixIsStrict` |
| `PAY-L3.3-ACC-011` | snapshots sans I/O, horloge ou crypto | `PaymentArchitectureTest` |
| `PAY-L3.3-ACC-012` | Aggregate, Policies, Services et Events différés | `aggregatePoliciesServicesAndEventsRemainDeferred` |
| `PAY-L3.3-ACC-013` | seule l’autorisation Lot 3.3 est active | `controlledAuthorizationActivatesOnlyLot33` |

## Limites

Les contrôles suivants seront exercés après implémentation des Policies et de
l’Aggregate Root :

```text
freshness profiles
Payment binding
replay / conflict / replacement
state eligibility
atomic mutation
event registration
```
