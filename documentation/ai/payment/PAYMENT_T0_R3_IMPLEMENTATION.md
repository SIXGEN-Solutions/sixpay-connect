# SIXPAY CONNECT — R3 — Réconciliation manuelle T0

Source de vérification: `SIXGEN-Solutions/sixpay-connect` @ `4fd47005dcd75d880d910d0748420cc2b16e224b`.

La commande interne `POST /internal/api/v1/payments/reconciliation` réutilise le chemin de récupération **bancaire** T0 existant (`RecoverUnknownPaymentT0UseCase` → GET payment-events par référence puis fallback par idempotence). Elle ne réutilise pas `reconcileTfj()` car ce cas d'usage est strictement réservé au TFJ. Aucun POST financier n'est lancé.

## Autorisation et sélection

- Rôles existants `ADMIN`/`OPS` et autorité `SCOPE_payment.reconcile`; vérification au niveau objet avec `PaymentAccessPolicy`.
- Entrée: liste de 1 à 50 `paymentIds` UUID uniques, en-tête `X-Correlation-ID` UUID obligatoire.
- Les claims sélectionnés appliquent les mêmes seuils d'âge, fenêtre glissante de 48 h, transactions courtes et verrous `SKIP LOCKED` que R2.
- Les opérations concurrentes du scheduler et d'autres opérateurs ne peuvent pas posséder simultanément le claim.
- Le moteur bancaire est appelé hors transaction SQL, puis la tentative est clôturée ou reprogrammée avec fencing.
- Les demandes échouées/inconclusives ne déclenchent jamais un nouveau POST bancaire.

## Audit

- Migration additive V304: journal opérationnel append-only, distinct du journal financier.
- Chaque paiement autorisé et évalué est tracé avec identité opérateur, corrélation, résultat et instant.
- Les refus au niveau objet sont tracés. Les refus de filtre HTTP 401/403 avant l'entrée du contrôleur doivent être complétés par une journalisation de sécurité transverse existante; ce lot n'invente pas de filtre Security.
- Les retours `NOT_ELIGIBLE_OR_BUSY` sont volontairement génériques: ne pas exposer l'état d'un claim concurrent.

## Validation manuelle (non exécutée par l'auteur)

Depuis `backend`:

1. `mvn -pl payment -am "-Dtest=PaymentT0ManualReconciliationServiceTest,PaymentT0RecoveryWorkerTest,PaymentT0RecoveryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
2. `mvn -pl payment -am -Pfull-tests "-Dit.test=PaymentT0RecoveryClaimStoreIT" "-Dfailsafe.failIfNoSpecifiedTests=false" verify` — contrôler que le test ciblé a réellement été exécuté, pas seulement le code de sortie.
3. `mvn -pl payment -am verify`
4. `mvn verify`

Depuis la racine:

5. `py scripts/verify_master_prompt_input_manifest.py`
6. `py scripts/verify_documentation_contract_references.py`

Aucune commande de projet ou écriture Git n'a été exécutée durant la préparation du correctif.
