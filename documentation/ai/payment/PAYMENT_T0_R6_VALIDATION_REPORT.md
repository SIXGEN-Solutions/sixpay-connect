# SIXPAY CONNECT — R6 — Matrice de validation intégrée Payment T0

- Repository : `SIXGEN-Solutions/sixpay-connect`
- Révision documentaire examinée : `feat/repository-baseline-consolidation-cucumber` @ `d9c2ec23c4dd0f16d9a833b7985a3cd82a18394b`
- Statut des preuves dynamiques : **NON EXÉCUTÉES par le préparateur**.
- Périmètre : récupération T0 bancaire, claims R2/R3, notification après résolution et régression.
- Hors périmètre : Accounting T1, modification de contrat, migration, API, permissions ou débit bancaire.

## Autorité contractuelle conservée

- `payment-t0-manual-reconciliation-api-v1` : `ACTIVE_MVP / APPROVED / ACTIVE / codeGenerationAllowed=true`.
- `payment-consultation-api-v1` : `ACTIVE_MVP / PENDING_APPROVAL / REFERENCE_ONLY / codeGenerationAllowed=false`.
- `tresorpay-payment-status-webhook-v1` : `ACTIVE_MVP / APPROVED / REFERENCE_ONLY / codeGenerationAllowed=false`.
- Ces statuts sont constatés dans le registre à la révision cible, sans changement R6.

## Matrice et traces attendues

| Cas | Source / test | Assertion ou preuve recherchée | Résultat local |
|---|---|---|---|
| UNKNOWN → COMPLETED | `PaymentT0ManualReconciliationServiceTest.r6ConclusiveCompletedBankLookupResolvesOwnedClaim` et tests T0 métier | `POSTED_PENDING_TFJ`, claim clôturé, audit RESOLVED, aucun deuxième POST bancaire | NON EXÉCUTÉ |
| UNKNOWN → REJECTED | `PaymentAtomicEventLifecycleTest.r6AuthoritativeRejectedLookupResolvesUnknownToRejected` + `...r6ConclusiveRejectedBankLookupResolvesOwnedClaimWithoutRetry` | `REJECTED`, claim clôturé, pas de second POST, transition persistée à confirmer en intégration | NON EXÉCUTÉ |
| GET bancaire 404 | `RestAmplitudePaymentEventRecoveryClientTest.r6Http404OnPaymentReferenceIsInconclusiveAndReturnsEmpty` ; fallback testé dans adapter | 404 = absence de résultat, non une preuve de rejet ; deuxième GET d'idempotence si pertinent | NON EXÉCUTÉ |
| GET bancaire 503 | `...r6Http503OnBankLookupIsUnknownNotAConclusiveRejection` + `...r6UnavailableBankLookupSchedulesRetryWithoutFalseRejection` | erreur technique incertaine, claim reprogrammé, pas de rejet artificiel | NON EXÉCUTÉ |
| UNKNOWN toujours inconnu | `...r6StillUnknownBankLookupKeepsTheAttemptRetryable` | RETRY_SCHEDULED, délai 2 minutes, claim non clôturé | NON EXÉCUTÉ |
| Concurrence opérateur/scheduler | `PaymentT0RecoveryClaimStoreIT.manualClaimExcludesSchedulerAndHonoursFencing` ; `twoOperatorsCannotClaimTheSamePayment` | un seul token possédé, fencing, aucune double acquisition | NON EXÉCUTÉ |
| Claim expiré / token obsolète | `PaymentT0RecoveryClaimStoreIT.staleLeaseReclaimedAndOldTokenCannotComplete` + `...r6OldFencingTokenCannotReportSuccessfulResolution` | ancien propriétaire ne clôture pas et ne prétend pas être RESOLVED | NON EXÉCUTÉ |
| Hors fenêtre | `PaymentT0RecoveryClaimStoreIT.r6ManualSelectionRejectsOldPostingDespiteRecentPaymentUpdate` | aucun claim après 49 h malgré `updated_at` récent | NON EXÉCUTÉ |
| Trop récent | `PaymentT0RecoveryClaimStoreIT.r6ManualSelectionRejectsPaymentBeforeMinimumPostingAge` | aucun claim pendant l'âge minimum de 2 minutes | NON EXÉCUTÉ |
| Callback COMPLETED | `PaymentCallbackPlanFactoryTest.cutCreditedIsBoundToDurableCompletedT0OutcomeEvent` ; `PaymentCallbackOutboxRelayTest` | événement métier durable → CUT_CREDITED, livraison et retry contrôlés | NON EXÉCUTÉ |
| Callback REJECTED | `PaymentCallbackPlanFactoryTest.r6RejectedT0EventCurrentlyHasNoCallbackPlanMapping` | **ÉCART STATIQUE : absence de mapping pour REJECTED dans la fabrique callback** | NON EXÉCUTÉ |

## Attention : preuve intégrée et blocage

Ces tests de composants n'établissent PAS à eux seuls un parcours complet :
banque simulée → mutation Payment persistée → outbox durable → callback HTTP réel → consultation partenaire.
Ce parcours doit être validé localement avec une intégration appropriée, y compris PostgreSQL/Testcontainers et récepteur callback, après vérification de ses fixtures.

L'implémentation du callback `REJECTED` n'est pas démontrée dans
`PaymentCallbackPlanFactory`. Le contrat webhook est approuvé et protégé :
**aucune modification de payload, de mapping de notification ou de contrat n'est appliquée par R6**.
Décision humaine requise pour valider le traitement attendu de la notification
après résolution T0 rejetée et son périmètre exact, avant correction de production.

## Commandes à exécuter localement (depuis `backend`)

```powershell
mvn -pl payment -am "-Dtest=PaymentT0ManualReconciliationServiceTest,PaymentT0RecoveryWorkerTest,PaymentT0RecoveryServiceTest,PaymentAtomicEventLifecycleTest,AmplitudePaymentEventRecoveryAdapterTest,RestAmplitudePaymentEventRecoveryClientTest,PaymentCallbackPlanFactoryTest,PaymentCallbackOutboxRelayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl payment -am -Pfull-tests "-Dit.test=PaymentT0RecoveryClaimStoreIT" "-Dfailsafe.failIfNoSpecifiedTests=false" verify
mvn -pl payment -am verify
mvn verify
```

Contrôler explicitement les rapports Surefire et Failsafe : **code de sortie nul seul insuffisant si un IT est ignoré par manque de Docker**. Exécuter ensuite les gates de gouvernance/doc du repository selon `AGENTS.md` et `TESTS_AND_GATES.md`.

## Preuves à renseigner après exécution locale

| Preuve | Valeur / lien local |
|---|---|
| Révision réellement testée (`git rev-parse HEAD`) | À renseigner |
| Date, JDK et version Maven | À renseigner |
| Docker / Testcontainers disponibles | À renseigner |
| Commande ciblée, exit code, nombre de tests | À renseigner |
| Tests IT PostgreSQL réellement exécutés, nombre | À renseigner |
| Commande globale, exit code, nombre de tests | À renseigner |
| État Payment et audit après COMPLETED / REJECTED | À renseigner |
| Vérification outbox et réception callback après résolution | À renseigner |
| Absence de nouveau POST financier en récupération | À renseigner |
| Anomalies, écarts et décisions restantes | À renseigner |

**La fermeture fonctionnelle de Payment T0 et le démarrage d'Accounting T1 ne sont pas autorisés par ce rapport de préparation.**
