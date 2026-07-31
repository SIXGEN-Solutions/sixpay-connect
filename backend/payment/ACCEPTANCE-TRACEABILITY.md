# Traçabilité d’acceptation — Payment Lot 3.2

| ID | Critère | Preuve automatisée |
| --- | --- | --- |
| `PAY-L3.2-ACC-001` | `PaymentId` est un UUID canonique non nul | `PaymentIdentityValueObjectsTest.paymentIdAcceptsOnlyCanonicalNonNilUuid` |
| `PAY-L3.2-ACC-002` | les références externes conservent la casse et leur format | `externalReferencesStripOnlyOuterAsciiWhitespaceAndPreserveCase` |
| `PAY-L3.2-ACC-003` | la référence publique respecte `PAY-` + ULID Crockford | `publicReferenceRequiresPayPrefixedCrockfordUlid` |
| `PAY-L3.2-ACC-004` | identité de requête, fingerprint et corrélation restent distincts | `requestIdentityKeepsThreeDistinctIdentities` |
| `PAY-L3.2-ACC-005` | le code banque est normalisé de manière indépendante de la locale | `financialInstitutionCodeUsesLocaleIndependentUppercase` |
| `PAY-L3.2-ACC-006` | les tokens de comptes protégés ne sont jamais imprimés | `ProtectedAccountValueObjectsTest.debtorReferenceNeverPrintsTheProtectedToken` et `treasuryReferenceEqualityUsesConfigurationIdentity` |
| `PAY-L3.2-ACC-007` | les allocations sont bornées, positives, uniques et équilibrées | `TreasuryAllocationIntentTest` |
| `PAY-L3.2-ACC-008` | `PaymentFailure` applique la matrice category/disposition | `PaymentFailureTest` |
| `PAY-L3.2-ACC-009` | les 17 statuts IA-1 et quatre terminaux sont exacts | `PaymentClassificationTest` |
| `PAY-L3.2-ACC-010` | `NOTIFIED` n’est pas un statut Payment | `PaymentClassificationTest.notifiedIsNotAPaymentStatus` |
| `PAY-L3.2-ACC-011` | `Money`, `CorrelationId` et primitives DDD ne sont pas dupliqués | `PaymentArchitectureTest.moduleReusesSharedPlatformPrimitives` |
| `PAY-L3.2-ACC-012` | aucun type du Lot 3.3 ou Aggregate Root n’est généré | `PaymentArchitectureTest.lot32ImplementsExactlyTheAuthorizedModelSources` |
| `PAY-L3.2-ACC-013` | le domaine reste sans framework ni dépendance inter-domaine | `PaymentArchitectureTest.domainRemainsFrameworkAgnostic` et `paymentDoesNotDependOnAnotherBusinessDomain` |
| `PAY-L3.2-ACC-014` | seule l’autorisation Lot 3.2 est active | `PaymentArchitectureTest.controlledAuthorizationActivatesOnlyLot32` |

## Limites

Les types suivants restent explicitement différés :

```text
EvidenceMetadata and snapshot-support identifiers
PostingInstructionIdentity
ReversalInstructionIdentity
PaymentCommandId
ExpectedBusinessVersion
PaymentEventSequence and event metadata
Payment and PaymentState
```
