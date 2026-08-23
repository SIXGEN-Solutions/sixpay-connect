# SIXPAY CONNECT — Registre des contrats

Ce dossier contient les contrats d’intégration et d’API versionnés de
SIXPAY CONNECT.

La présence physique d’un contrat dans le dépôt ne détermine pas à elle seule
son usage courant. La classification normative et l’index cross-domain sont
portés par [`CONTRACT_REGISTRY.yaml`](./CONTRACT_REGISTRY.yaml).

## Canonical contract index

`CONTRACT_REGISTRY.yaml` is the **canonical contractual table of contents** for
the current SIXPAY CONNECT repository baseline.

It answers the repository-level governance questions:

- which contracts exist;
- which domain and capability they belong to;
- who owns the capability and security boundary;
- the interaction direction;
- source system and system of record;
- lifecycle and approval state;
- generation policy;
- security classification;
- current MVP usage;
- the canonical physical contract path.

The registry is therefore authoritative for **contract classification,
ownership, lifecycle, approval, generation policy and usage metadata**.

Physical contract files remain authoritative for the **interface itself**:

- paths/endpoints;
- operations;
- request and response payloads;
- schemas;
- parameters;
- protocol-level security declarations;
- error responses;
- event/message structure for asynchronous contracts.

The intended relationship is:

```text
CONTRACT_REGISTRY.yaml
        |
        +-- contract identity / capability
        +-- ownership / direction
        +-- lifecycle / approval
        +-- generation policy
        +-- security classification
        +-- MVP usage
        +-- canonical physical path
                    |
                    v
          physical contract file
                    |
                    +-- interface/protocol truth
```

A physical contract must not become a second independent registry.
Conversely, the registry must not duplicate full interface definitions.

When a physical contract repeats governance metadata through
`info.x-sixpay-contract`, that metadata is a contract-local mirror used for
traceability and validation. It must remain consistent with
`CONTRACT_REGISTRY.yaml`; the registry remains the canonical cross-contract
index.

### Source-of-truth rule

For the current repository baseline:

1. `CONTRACT_REGISTRY.yaml` is authoritative for registry-level governance
   metadata.
2. The physical contract referenced by `path` is authoritative for the
   interface/protocol definition.
3. Git history is authoritative for historical changes.
4. Transitional patch artifacts are not valid current-state sources of truth.

This separation allows the future Master AI Context to discover the complete
contract landscape from a single registry without flattening bounded API and
integration contracts into one monolithic specification.

## Classement au Gate IA-0R

| Contrat | Classement | Usage MVP | Génération |
| --- | --- | --- | --- |
| `amplitude-customer-verification-api-v1.yaml` | `REFERENCE_MVP` | Vérification bancaire en support du paiement | Référence uniquement |
| `tresorpay-authorization-request-api-v1.yaml` | `DEFERRED_FUTURE` | Aucun | Exclue |
| `tresorpay-authorization-decision-webhook-v1.yaml` | `DEFERRED_FUTURE` | Aucun | Exclue |

Le contrat Amplitude existant ne couvre ni le contrôle du solde disponible, ni
le débit du client, ni le crédit du CUT, ni la confirmation après TFJ. Ces
capacités doivent être définies dans le Contract Pack Payment.

Les deux contrats TRESOR PAY d’autorisation sont conservés à leurs chemins
actuels pour la traçabilité et une évolution future du parcours d’abonnement.
Pour le MVP, TRESOR PAY reste maître de l’abonnement et SIXPAY ne gère aucun
cycle de vie local d’abonnement.

## Contrats internes de consultation — étape 1.7

| Contrat | Classement | Usage MVP | Génération |
| --- | --- | --- | --- |
| `internal/payment-query-api-v1.yaml` | `ACTIVE_MVP` | Consultation opérationnelle Payment masquée | Référence uniquement jusqu’à approbation |
| `internal/observed-customer-query-api-v1.yaml` | `ACTIVE_MVP` | Consultation ObservedCustomer masquée | Référence uniquement jusqu’à approbation |
| `internal/payment-audit-query-api-v1.yaml` | `ACTIVE_MVP` | Timeline, audit immuable et export contrôlé | Référence uniquement jusqu’à approbation |

Ces trois contrats sont strictement en lecture seule. Le contrat d’audit est
séparé du contrat de consultation Payment afin d’appliquer des scopes, une
classification et une traçabilité renforcés. Les opérations d’export restent
contrôlées et ne constituent ni une mutation métier ni une commande de replay.

## Règle de gouvernance

Toute évolution de classement doit mettre à jour dans le même changement :

1. `CONTRACT_REGISTRY.yaml`;
2. l’extension `info.x-sixpay-contract` du contrat;
3. le manifeste IA du domaine concerné, notamment
   `documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml` pour le Payment Contract Pack;
4. le document de Gate concerné.

## FS-2.1 — Repository baseline consolidation decisions

During `FS-2.1 — Contract consolidation`, the following internal contracts are
explicitly classified as **KEEP**.

`KEEP` is a repository-consolidation decision. It does not replace the
normative lifecycle, approval, generation, security or ownership metadata in
`CONTRACT_REGISTRY.yaml`.

| Physical contract | Decision | Preserved boundary |
| --- | --- | --- |
| `internal/payment-query-api-v1.yaml` | `KEEP` | Operational masked Payment query capability |
| `internal/payment-audit-query-api-v1.yaml` | `KEEP` | Privileged immutable Payment audit, timeline and controlled export boundary |
| `internal/observed-customer-query-api-v1.yaml` | `KEEP` | Customer-owned non-authoritative ObservedCustomer query projection |
| `internal/accounting-query-api-v1.yaml` | `KEEP` | Accounting batch query capability |
| `internal/notification-operational-trigger-v1.md` | `KEEP` | Inbound semantic trigger contract consumed by Notification |
| `internal/notification-operational-email-v1.md` | `KEEP` | Outbound operational email dispatch/provider boundary |

### Preservation rationale

These contracts must remain physically independent because they represent
different ownership, security, data-classification, direction or operational
semantics.

In particular:

- Payment Query and Payment Audit remain separate. Audit has stronger
  confidentiality, traceability and export semantics and must not be folded
  into the ordinary Payment query API.
- ObservedCustomer remains separate from authoritative Customer Management.
  It is a non-authoritative projection created from observed Payment facts.
- Accounting Batch Query remains an Accounting-owned bounded query capability.
- Notification Trigger and Notification Email remain separate because the
  trigger contract describes semantic facts entering Notification, while the
  email contract describes the provider-facing dispatch boundary leaving
  Notification.

No endpoint, schema, capability, authorization rule or registry identity is
changed by this preservation decision.
