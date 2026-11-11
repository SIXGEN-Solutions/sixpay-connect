# SIXPAY CONNECT — R2 : Réconciliation bancaire T0 automatique par lots

Référence autoritative : `feat/repository-baseline-consolidation-cucumber` @ `e0c7599ab3e5e5b51d1247de732d187c39aab4a9`.
Mode de production : script Python local, aucun changement distant, aucune validation Java ou PostgreSQL exécutée.

## Périmètre

- Worker Spring conditionnel, **désactivé par défaut** (`sixpay.payment.t0-recovery.enabled=false`).
- Scan PostgreSQL des paiements `POSTING_PENDING` ou `POSTING_OUTCOME_UNKNOWN` dont l'événement d'audit durable `PaymentPostingAuthorized` est compris entre maintenant − 48 h et maintenant − 2 min.
- Création paresseuse d'un enregistrement de suivi par Payment (migration Flyway additive `V303`).
- Claim en transaction courte avec `FOR UPDATE SKIP LOCKED`, lease par jeton UUID et tentative incrémentée.
- Consultation **hors transaction SQL** via `RecoverUnknownPaymentT0UseCase.recoverByPaymentId`, qui réutilise le GET et le fallback d'idempotence existants. Aucun POST bancaire.
- Résultat encore incertain ou erreur technique : backoff durable 2, 5, 15, 30 puis 60 minutes (plafond), jusqu'à expiration de la fenêtre de 48 h ; jamais de rejet automatique sur `404`, `UNKNOWN` ou `503`.
- Résultat sortant des états éligibles : claim marqué résolu ; la finalisation financière reste réalisée par le moteur Payment existant.
- Après interruption : reclaim à expiration du lease ; les jetons d'ancienne tentative sont rejetés à la clôture.

## Configuration externe

Exemple de propriétés Spring, à appliquer seulement dans un environnement approuvé :

```properties
sixpay.payment.t0-recovery.enabled=true
sixpay.payment.t0-recovery.poll-delay=PT2M
sixpay.payment.t0-recovery.window=PT48H
sixpay.payment.t0-recovery.minimum-age=PT2M
sixpay.payment.t0-recovery.batch-size=50
sixpay.payment.t0-recovery.claim-timeout=PT5M
sixpay.payment.t0-recovery.worker-id=payment-t0-recovery-node-1
```

Le `worker-id` doit être distinct par instance. Les valeurs ci-dessus sont des paramètres de départ, non une preuve de capacité bancaire.

## Invariants et limites

- Aucune mutation de `PaymentJpaEntity`, du contrat bancaire, des contrôles `checks`, de l'API partenaire ou des états métier.
- La table de récupération est distincte de `payment_outbox_events` et des claims de callbacks.
- Un lease expiré peut permettre des GET bancaires concurrents ; la clôture est fenced. Le moteur Payment conserve son verrou optimiste et ses règles de transition pour la résolution financière.
- La frontière de 48 heures se fonde sur le posting durable, pas sur `updated_at`.
- Une transaction trop ancienne demeure incertaine ; son traitement manuel appartient à R3.
- Un worker n'effectue aucun traitement Accounting T1.
- Vérifier la visibilité du nouvel emplacement Flyway dans le runtime `bootstrap` avant activation sur environnement partagé.

## Plan de validations locales, non exécutées ici

Depuis `backend` :

```powershell
mvn -pl payment -am "-Dtest=PaymentT0RecoveryWorkerTest,PaymentT0RecoveryServiceTest,PaymentT06PendingRecoveryTest,AmplitudePaymentEventRecoveryAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl payment -am -Pfull-tests "-Dit.test=PaymentT0RecoveryClaimStoreIT" verify
mvn -pl payment -am verify
mvn verify
```

Depuis la racine :

```powershell
py scripts/verify_master_prompt_input_manifest.py
```

Attendus : création de V303 dans PostgreSQL ; un seul claim concurrent par Payment ; impossibilité pour un ancien jeton de clôturer ; reprise après crash ; backoff durable ; fenêtre 48 h ; aucune mutation pour une absence bancaire ; aucune exécution POST ; non-régression Payment. Les commandes n'ont pas été exécutées par l'auteur.
