# SIXPAY CONNECT — Réconciliation bancaire T0 — R0 Audit et cadrage technique

- Repository : `SIXGEN-Solutions/sixpay-connect`
- Révision d'analyse GitHub : `feat/repository-baseline-consolidation-cucumber` @ `7234a7cca9c63fc951dc36796f2ca13c98259a9a` (comparaison GitHub : identical)
- Mode : revue statique en lecture seule, sans exécution des tests ni environnement local
- Objet : R0 uniquement. Ce rapport ne modifie aucune décision de gouvernance ni contrat.

## Matrice d'écarts

| Capacité | Observation sur le SHA analysé | Qualification | Suite |
|---|---|---|---|
| Récupération unitaire T0 | `PaymentT0RecoveryService` délègue à `PaymentEventLifecycleOrchestrationService.recover()` pour `POSTING_PENDING` et `POSTING_OUTCOME_UNKNOWN`. | Présent, conserver | Tests fonctionnels à exécuter localement |
| Récupération bancaire | `AmplitudePaymentEventRecoveryAdapter` cherche par `paymentReference`, puis `idempotencyKey` si résultat absent ou `UNKNOWN`. | Présent, conserver | Couverture GET réelle à vérifier |
| Absence bancaire 404 | Le client GET retourne `Optional.empty`; deux absences deviennent `NOT_FOUND` et `recover()` n'impose pas de transition. | Conforme statiquement | Vérifier en intégration |
| Issue inconnue / 503 | Le client remonte une exception d'incertitude pour les erreurs réseau/5xx ; aucune nouvelle exécution POST dans `recover()`. | Présent, conserver | Vérifier absence de transition et de POST en intégration |
| `COMPLETED` sans `dco` | La récupération échoue explicitement au lieu d'inventer une date bancaire. | Présent, conserver | Tester la fermeture avec réponse réelle conforme |
| Sécurité bancaire configurable | Le même `RestClient`, token provider, `AmplitudePostingProperties.Security`, OAuth2 optionnel et SSL conditionnel sont partagés par POST et GET. | Présent, conserver | Vérifier profils et injection runtime |
| Autorisation métier | `PaymentAction.RECONCILE`, `PaymentAuthority.RECONCILE` et `PaymentAccessPolicy.canReconcile()` existent. Rôles `ADMIN` ou `OPS` + autorité `SCOPE_payment.reconcile`. | Présent, conserver | Ne pas créer de permission supplémentaire |
| Consultation partenaire | `PaymentConsultationController` renvoie le modèle SIXPAY courant et n'appelle pas la banque. | Présent, conserver | Aucun nouveau champ, notamment `checks` |
| Consultation interne | `PaymentQueryController` fournit la recherche et le détail masqués. | Présent, conserver | Conserver le contrat et la pagination |
| Interface Payment | `payment-list-page.component.ts` et `PaymentsApiClient` proposent la consultation et les filtres, pas une commande de réconciliation. | Écart fonctionnel de lot ultérieur, pas défaut R0 | R3/R4 après approbation applicable |
| Sélection automatique | Aucun scheduler de réconciliation T0 démontré dans les composants R0 inspectés. | Non démontré, pas preuve d'absence globale | Inventaire exhaustif et R2 ultérieur |
| Concurrence du worker | Aucune preuve de claim/lease batch au sein des composants inspectés. | Point non vérifié | Décision de persistance/migration si nécessaire |

## Parcours technique observé

`PaymentT0RecoveryService` -> `PaymentEventLifecycleOrchestrationService.recover()` -> `PaymentEventRecoveryPort` -> `AmplitudePaymentEventRecoveryAdapter` -> `RestAmplitudePaymentEventRecoveryClient` -> Core Banking GET par référence et éventuellement GET par idempotence -> `PaymentFinalizationService.resolvePaymentEventOutcome()`.

Les appels utilisent la même configuration `sixpay.payment.banking.amplitude.posting` que le posting. Les chemins de récupération sont définis dans `application-payment-banking-capabilities.yml`.

## Contrats et autorité

- `amplitude-payment-posting-api-v1` : `ACTIVE_MVP`, `APPROVED`, `ACTIVE`, `codeGenerationAllowed: true`.
- `payment-consultation-api-v1` : `ACTIVE_MVP`, `PENDING_APPROVAL`, `REFERENCE_ONLY`, `codeGenerationAllowed: false`.
- `payment-query-api-v1` : `ACTIVE_MVP`, `PENDING_APPROVAL`, `REFERENCE_ONLY`, `codeGenerationAllowed: false`.
- R0 ne change aucun contrat. Aucune donnée `checks` ne doit sortir dans la consultation partenaire.

## Fichiers à conserver

- `backend/payment/src/main/java/com/sixpay/payment/application/service/PaymentT0RecoveryService.java`
- `backend/payment/src/main/java/com/sixpay/payment/application/service/PaymentEventLifecycleOrchestrationService.java`
- `backend/payment/src/main/java/com/sixpay/payment/infrastructure/banking/amplitude/posting/AmplitudePaymentEventRecoveryAdapter.java`
- `backend/payment/src/main/java/com/sixpay/payment/infrastructure/banking/amplitude/posting/client/RestAmplitudePaymentEventRecoveryClient.java`
- `backend/payment/src/main/java/com/sixpay/payment/infrastructure/banking/amplitude/posting/configuration/AmplitudePostingConfiguration.java`
- `backend/payment/src/main/java/com/sixpay/payment/application/security/PaymentAccessPolicy.java`
- `backend/payment/src/main/java/com/sixpay/payment/api/PaymentConsultationController.java`
- `backend/payment/src/main/java/com/sixpay/payment/api/PaymentQueryController.java`
- `frontend/src/app/features/payments/components/payment-list-page.component.ts`

## Stratégie de validation locale proposée — aucune commande exécutée par l'auteur

1. `cd backend && mvn -pl payment -am -Dtest=PaymentT0RecoveryServiceTest,PaymentT06PendingRecoveryTest,AmplitudePaymentEventRecoveryAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test`
2. `cd backend && mvn -pl payment -am -Dtest=PaymentEventLifecycleExecutionGuardTest,PaymentConsultationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
3. Compléter les tests ciblés GET (200 COMPLETED, REJECTED, UNKNOWN ; double 404 ; 503 ; OAuth2 ON/OFF ; mTLS ON/OFF) si ces cas ne sont pas déjà couverts.
4. `cd backend && mvn -pl payment -am verify` puis `cd backend && mvn verify` ; exécuter les tests PostgreSQL/Testcontainers seulement avec Docker disponible et autorisation du lot concerné.
5. `cd frontend && npm run verify:sixpay` pour la non-régression consultation.
6. `py scripts/verify_master_prompt_input_manifest.py` et les gates documentaires/contractuelles applicables dans les sources de gouvernance.

Preuves attendues : code de sortie 0 de chaque commande exécutée ; assertions sur absence de second POST, fallback idempotence, maintien d'UNKNOWN/NOT_FOUND et conservation des champs partenaires.

## Décisions et risques reportés aux lots ultérieurs

- Fenêtre glissante 48 h, lot 50 et fréquence 2 min : propositions, **pas** comportement constaté ni paramétrage déployé.
- Sélection batch, verrous, retries, lease ou migration : nécessitent vérification et approbation si modèle transactionnel/persistance change.
- Déclenchement manuel backend/frontend : réutiliser `RECONCILE` existant et exiger la validation d'une API de commande avant toute création d'endpoint.
- Aucun ajout aux contrats de consultation, aux DTO publics ni au schéma SQL dans R0.
- Worktree local non observé ; ascendance `baselineCommit` -> SHA cible vérifiée sur GitHub (`ahead`, 308 commits, `behind=0`), mais aucune vérification de révision/worktree Git local ni exécution des gates.

## Différence entre les révisions de référence

- GitHub : `bb2da6a90f4d9e69dcd4e7ef153f8ca8aadc1edc` vers `7234a7cca9c63fc951dc36796f2ca13c98259a9a` : un seul commit, modification de `frontend/proxy.conf.json` uniquement (2 lignes ajoutées, 2 supprimées).
- Les huit fichiers de contrôle de R0 conservent leurs empreintes Git connues sur le SHA cible.
- Ne pas écraser ou restaurer `frontend/proxy.conf.json`; ce chemin ne fait pas partie des écritures R0.

## État R0

Audit et périmètre documentés au SHA cible, **aucune correction de code démontrée nécessaire dans R0**. Les comportements ne sont pas validés dynamiquement. Les écarts de livraison R2/R3/R4 ne doivent pas être corrigés sous couvert de R0.
