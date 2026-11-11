# R4 — Interface de réconciliation T0 sur la page Payment

Révision cible : `dc241ddd5e6b0d368d97446a502c9ae656767cb6`.

- La liste Payment existante reste l'écran unique ; aucun nouveau parcours métier.
- Filtres de statut existants : `POSTING_PENDING` et `POSTING_OUTCOME_UNKNOWN`. Le backend accepte un seul statut par recherche ; raccourci dédié à `POSTING_OUTCOME_UNKNOWN`.
- Sélection explicite sur la page courante uniquement, limitée à 50 paiements éligibles. La sélection est réinitialisée lors d'une navigation ou recherche.
- POST interne R3 existant `/internal/api/v1/payments/reconciliation`, sans nouveau contrat ni appel bancaire direct depuis Angular.
- L'intercepteur global ajoute `X-Correlation-ID` ; l'authentification reste backend-authoritative.
- Le mode mock ne déclenche aucune commande financière ; pas de simulation de succès de réconciliation.
- Les résultats R3 sont affichés individuellement ; 400/401/403/503 donnent un message sans données sensibles.
- **Limitation gouvernance** : le catalogue frontend `SIXPAY_ROLES` ne contient pas OPS, contrairement au contrat R3. R4 ne change ni ce catalogue ni les guards/routes : l'action est offerte aux ADMIN disposant de `payment.reconcile`. Une décision Security est requise pour rendre la page et l'action accessibles à OPS.

## Vérifications locales (non exécutées par le générateur)

Dans `frontend` : `npm test -- --include=src/app/features/payments` (vérifier syntaxe de sélection selon Angular CLI), puis `npm run format:check`, `npm run lint`, `npm run build`, `npm run verify:quality`.
Vérifier manuellement : ADMIN avec permission / sans permission, MANAGER, AUDITOR, mode mock, 400/401/403/503, pagination, sélection de 50, résultats mixtes R3, absence de POST bancaire direct.
