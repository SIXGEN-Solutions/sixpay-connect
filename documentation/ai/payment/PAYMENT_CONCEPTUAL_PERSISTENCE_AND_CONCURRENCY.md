# SIXPAY CONNECT — Persistance conceptuelle et concurrence Payment

> **Lot:** `7 — Persistance conceptuelle et concurrence`  
> **Statut:** `NORMATIVE_IMPLEMENTED`  
> **JPA / migrations:** `INTERDITS DANS CE LOT`

## 1. Décision de périmètre

Le Lot 7 définit des **capacités de persistance**, pas un schéma physique final.
Les noms `payments`, `payment_audit`, `payment_idempotency` ou
`payment_outbox` restent des noms candidats jusqu’à l’approbation de la
conception de persistance et des conventions Flyway.

La structure technique finale devra suivre le golden module `partner` :

```text
application
→ démarcation transactionnelle et orchestration

domain
→ état, invariants et décisions

infrastructure.persistence
→ mapping explicite domaine ↔ stockage

infrastructure.messaging
→ Outbox locale et adaptateur vers le relais transverse

configuration
→ assemblage Spring Boot 4
```

## 2. Modèle transactionnel

```text
canonical request/idempotency arbitration
        ↓
load or create Payment
        ↓
domain operation
        ↓
optimistic version check
        ↓
persist Payment state/version
+ append immutable audit
+ append publishable Outbox rows
+ persist idempotency result when applicable
        ↓
ONE DATABASE COMMIT
```

Aucun broker n’est appelé dans cette transaction.

## 3. Capacités requises

| ID | Capacité | Stockage candidat | Obligatoire | Finalité | Contraintes principales |
| --- | --- | --- | --- | --- | --- |
| `PAY-PERS-001` | Aggregate state persistence | `payments` | `True` | Persist and reconstitute the complete PaymentState without creating events. | PRIMARY KEY(payment_id)<br>UNIQUE(public_payment_reference)<br>UNIQUE(payment_source, external_payment_reference)<br>CHECK(business_version > 0)<br>optimistic lock on business_version |
| `PAY-PERS-002` | Current failure persistence | `payments columns or payment_failures` | `True` | Persist the current safe PaymentFailure required for reconstitution. | no free-form provider message<br>no stack trace<br>no secret or account value |
| `PAY-PERS-003` | Failure history | `payment_failure_history` | `False` | Retain an immutable history when operational or regulatory requirements demand more than the current failure. | append-only<br>ordered by payment_id and occurred_at<br>safe failure fields only |
| `PAY-PERS-004` | Immutable audit | `payment_audit` | `True` | Persist one immutable audit record for every registered Payment Domain Event. | UNIQUE(event_id)<br>UNIQUE(payment_id, aggregate_version, event_sequence)<br>UPDATE and DELETE blocked<br>same transaction as Payment and Outbox |
| `PAY-PERS-005` | Request idempotency | `payment_idempotency` | `True` | Resolve identical replays to the original result and reject conflicting payloads. | UNIQUE(idempotency_scope, idempotency_key)<br>store canonical request fingerprint<br>store original safe response/result reference<br>same transaction as successful first admission when applicable |
| `PAY-PERS-006` | Transactional Outbox | `payment_outbox` | `True` | Persist publishable integration intents without contacting Kafka in the business transaction. | UNIQUE(event_id)<br>UNIQUE(aggregate_id, aggregate_version, event_sequence)<br>same transaction as Payment and audit<br>at-least-once relay |
| `PAY-PERS-007` | Authorization replay protection | `payment_authorization_replay` | `PROFILE_DEPENDENT` | Prevent reuse of protected authorization evidence where the approved security profile requires one-time semantics. | UNIQUE(issuer, evidence_reference_or_jti_hash)<br>store only protected fingerprint/hash<br>never store bearer token or raw JWT<br>expiry aligned with approved security profile |
| `PAY-PERS-008` | Authoritative financial outcome lookup correlation | `payments and/or dedicated protected lookup projection` | `True` | Retain immutable posting/reversal instruction keys and opaque bank references required to resolve uncertain outcomes. | UNIQUE(posting_idempotency_key)<br>reversal idempotency key unique within payment scope<br>original principal posting reference immutable<br>no blind financial resubmission |

## 4. Contraintes minimales

```text
UNIQUE(payment_source, external_payment_reference)
UNIQUE(public_payment_reference)
optimistic lock on business_version
atomic Payment + audit + Outbox
immutable original posting/reversal identities
no raw token, credential, JWT or bank payload
```

Les contraintes uniques constituent l’arbitre final contre les courses. Une
prélecture applicative améliore le chemin nominal mais ne remplace jamais la
contrainte atomique de la base.

## 5. Index de consultation

| ID | Cas d’usage | Colonnes conceptuelles | Type |
| --- | --- | --- | --- |
| `PAY-IDX-001` | PaymentId lookup | `payment_id` | `PRIMARY_OR_UNIQUE` |
| `PAY-IDX-002` | TRESOR PAY reference lookup | `payment_source, external_payment_reference` | `UNIQUE` |
| `PAY-IDX-003` | Public SIXPAY reference lookup | `public_payment_reference` | `UNIQUE` |
| `PAY-IDX-004` | Bank posting reference lookup | `principal_posting_reference` | `INDEX_OR_UNIQUE_WHEN_BANK_CONTRACT_ALLOWS` |
| `PAY-IDX-005` | Status work queue | `payment_status, updated_at` | `INDEX` |
| `PAY-IDX-006` | Period consultation | `received_at, payment_id` | `INDEX` |
| `PAY-IDX-007` | Correlation tracing | `correlation_id, occurred_at` | `INDEX_ON_AUDIT_AND_IDEMPOTENCY` |
| `PAY-IDX-008` | TFJ reconciliation | `financial_institution_code, business_date, principal_posting_reference` | `INDEX` |
| `PAY-IDX-009` | Outbox relay | `status, next_attempt_at, occurred_at` | `INDEX` |

Les index doivent soutenir les contrats internes de consultation sans exposer
les colonnes protégées. Les recherches par compte utilisent des empreintes
approuvées, jamais le numéro complet.

## 6. Données protégées

La persistance conserve trois représentations distinctes lorsque nécessaire :

```text
référence technique protégée/tokenisée
valeur masquée de consultation/audit
empreinte de binding/recherche autorisée
```

Interdictions :

```text
Bearer token
Subscription key
PIN
API key/password/secret
JWT brut
RIB ou numéro de compte complet en clair
payload bancaire brut
stack trace ou erreur fournisseur libre
```

## 7. Optimistic locking

`businessVersion` est à la fois :

- la version métier de l’Aggregate Root ;
- la valeur de verrou optimiste persistée ;
- la version portée par les événements et audits.

Une mise à jour suit conceptuellement :

```sql
UPDATE payment
SET ..., business_version = :nextVersion
WHERE payment_id = :paymentId
  AND business_version = :expectedVersion
```

`0 row updated` produit `PAYMENT_VERSION_CONFLICT`. Le cas d’usage recharge
l’agrégat et réévalue la décision. Il ne réémet jamais aveuglément une commande
financière.

## 8. Résultats bancaires incertains

Les capacités de persistance doivent conserver durablement :

```text
postingInstructionId
postingIdempotencyKey
postingInstructionFingerprint
principalBankPostingReference?
reversalInstructionId?
reversalIdempotencyKey?
last accepted evidence identity/fingerprint
lookup-required status
```

`POSTING_OUTCOME_UNKNOWN` et `REVERSAL_OUTCOME_UNKNOWN` déclenchent un lookup
autoritatif. Ils ne permettent ni nouveau posting ni nouveau reversal.

## 9. Scénarios de concurrence

| ID | Scénario | Arbitrage | Gagnant | Concurrent/perdant | Résultat déterministe |
| --- | --- | --- | --- | --- | --- |
| `PAY-CONC-001` | Two simultaneous requests with the same idempotency key and the same payload | Unique idempotency key plus canonical fingerprint | The transaction that creates the idempotency record first | Reloads the committed original result and returns it | `IDEMPOTENT_REPLAY_OR_RETRY_AFTER_WINNER_COMMIT` |
| `PAY-CONC-002` | Same external reference with different idempotency keys and equivalent payload | UNIQUE(payment_source, external_payment_reference) plus canonical fingerprint comparison | First committed Payment | Returns the existing Payment/result as a semantic replay | `EXISTING_PAYMENT_RETURNED` |
| `PAY-CONC-003` | Same external reference with different payloads | Unique external reference and canonical fingerprint mismatch | First committed Payment remains authoritative | Rejected as conflict without mutation | `PAYMENT_IDEMPOTENCY_CONFLICT` |
| `PAY-CONC-004` | Duplicate Amplitude callback | Evidence identity and fingerprint plus optimistic locking | First accepted evidence transition | Identical callback is no-op; different callback is quarantined/conflict | `NO_OP_OR_EVIDENCE_CONFLICT` |
| `PAY-CONC-005` | TFJ callback received before the expected projection or consumer state | Durable inbox/staging keyed by confirmation identity and posting reference | Callback is durably retained | Not applicable | `DEFERRED_MATCH_WITHOUT_DATA_LOSS` |
| `PAY-CONC-006` | Notification delivery replay | Consumer idempotency by sourceEventId and notification phase | First delivery/processed marker | No-op delivery replay | `NOTIFICATION_NO_OP` |
| `PAY-CONC-007` | Duplicate reversal result | Reversal evidence identity/fingerprint plus optimistic locking | First conclusive reversal outcome | Identical no-op; conflicting evidence quarantined | `NO_OP_OR_EVIDENCE_CONFLICT` |
| `PAY-CONC-008` | Concurrent transition while reconciliation is processing | Optimistic lock on business_version and full state re-evaluation | First committed valid transition | Reloads current Payment and re-runs domain decision; never blind retries a financial command | `VERSION_CONFLICT_THEN_REEVALUATION` |

## 10. Callback TFJ précoce

Le callback TFJ reçu avant que Payment soit matchable ne doit pas être perdu ni
forcer une transition invalide. La future intégration doit offrir une capacité
durable d’inbox/staging :

```text
receive callback
→ deduplicate by confirmation identity
→ persist safely
→ attempt unique match
→ defer while unmatched
→ consume once Payment/posting projection is available
```

Aucun Payment fantôme n’est créé depuis le callback.

## 11. Rétention

Les durées exactes restent soumises à Compliance, Security, Accounting et
Operations. Le modèle exige néanmoins :

- audit append-only pendant toute la durée réglementaire ;
- idempotency record pendant au moins la fenêtre maximale de retry et de
  réconciliation financière ;
- protection contre replay d’autorisation pendant le TTL approuvé ;
- Outbox publié conservé selon les besoins d’audit et de reproduction.

## 12. Critère de sortie

Pour tout retry, doublon ou conflit concurrent :

```text
un seul Payment logique
au plus une commande financière logique
une version par mutation réussie
replay identique → résultat original ou no-op
payload différent → conflit stable
outcome inconnu → lookup autoritatif
aucune mutation partielle
```
