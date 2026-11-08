# SIXPAY CONNECT — R3 Réconciliation manuelle backend — décision préalable

- Révision autoritative observée sur GitHub : `feat/repository-baseline-consolidation-cucumber` @ `4fd47005dcd75d880d910d0748420cc2b16e224b`.
- Mode : audit statique GitHub ; aucun test, aucune migration, aucune modification distante.
- Statut : `BLOCKED_DECISION` pour la commande manuelle R3.

## Implémentation vérifiée

- R2 contient `PaymentT0RecoveryWorker`, `PaymentT0RecoveryClaimStore` et la migration additive `V303__payment_t0_recovery_attempts.sql`.
- L'exécution bancaire unitaire existe via `RecoverUnknownPaymentT0UseCase` et `PaymentT0RecoveryService` ; cette surface appelle `recover()` sans second POST financier.
- `PaymentAccessPolicy.canReconcile()` et `PaymentAction.RECONCILE` existent : les rôles `ADMIN` ou `OPS` et l'autorité `SCOPE_payment.reconcile` sont déjà requis. Ne pas ajouter de nouvelle permission.
- `PaymentQueryController` est une API de consultation ; `PaymentCommandController` expose `POST /v1/payments/initiate`, sans commande manuelle T0.
- Les claims R2 et les verrous SQL appartiennent à un worker conditionné par `sixpay.payment.t0-recovery.enabled`. Une commande manuelle ne doit pas contourner la concurrence ni les tokens de fencing.

## Contrats consultés et blocage

- `payment-query-api-v1` : `ACTIVE_MVP`, `PENDING_APPROVAL`, `REFERENCE_ONLY`, `codeGenerationAllowed: false`, lecture seule, aucune mutation de Payment.
- `payment-command-api-v1` : `ACTIVE_MVP`, `APPROVED`, `ACTIVE`, `codeGenerationAllowed: true`, mais ne définit que l'initiation sur `/v1/payments/initiate`.
- Aucun contrat consulté n'approuve une nouvelle opération de réconciliation T0 manuelle. Ne pas inventer de méthode, d'URL, de requête, de réponse ni de code HTTP.

## Décisions humaines nécessaires avant écriture applicative

1. Approuver le contrat physique de la commande manuelle interne Payment T0 (sélection de paiements, schémas d'entrée/sortie, réponses, idempotence éventuelle, erreurs et sécurité), et synchroniser le registre selon la gouvernance.
2. Approuver le mécanisme d'audit spécifique aux tentatives et aux refus de réconciliation manuelle, y compris l'identité de l'opérateur, la corrélation et le sort des événements d'audit, sans modifier silencieusement l'audit financier existant.
3. Définir explicitement le comportement si un paiement sélectionné possède déjà un claim du scheduler R2, et la politique de sélection hors de la fenêtre de 48 h si cette possibilité doit exister.

## Correction R3 cible après approbation

- Réutiliser `RecoverUnknownPaymentT0UseCase` pour les consultations bancaires.
- Appliquer `PaymentAction.RECONCILE` et `PaymentAccessPolicy` aux objets choisis, sans modifier les règles existantes.
- Réutiliser ou adapter la coordination durable R2 sous approbation, sans transaction SQL ouverte lors du GET bancaire.
- Conserver `UNKNOWN`/`NOT_FOUND` comme inconclusifs, sans nouveau POST.
- Produire les tests de refus 401/403, accès objet, liste sélectionnée, doublons, concurrence avec scheduler, audit réussi/refusé, 404/503 et interruption.
- Ne pas commencer Accounting T1.

## Validations à exécuter uniquement après correction R3 autorisée

- `cd backend && mvn -pl payment -am -Dtest=PaymentT0RecoveryServiceTest,PaymentT0RecoveryWorkerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Pour les tests d'intégration R2 existants, utiliser `-Dfailsafe.failIfNoSpecifiedTests=false` dans le reactor et **vérifier le nombre réel de tests exécutés**.
- Après implémentation : tests ciblés de l'API manuelle, PostgreSQL/Testcontainers concurrence, `mvn -pl payment -am verify`, `mvn verify`, gates documentaires et contractuels applicables.

## Garantie de ce livrable

Ce rapport n'est pas une implémentation R3. Son script de génération ne modifie aucun code, contrat, audit, sécurité, migration ou configuration et refuse d'écraser un document divergent.
