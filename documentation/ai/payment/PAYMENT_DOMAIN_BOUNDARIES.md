# SIXPAY CONNECT — Payment Domain Boundaries

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Lot:** `1 — Ubiquitous Language and Domain Boundaries`  
> **Status:** `DRAFT_PENDING_VALIDATION`  
> **Code generation:** **FORBIDDEN**

## 1. Boundary decision

`Payment` owns the SIXPAY business lifecycle of one payment intention and the
minimal immutable evidence required to explain its decisions. It does not own
external systems, transport mechanisms or unbounded process histories.

## 2. What Payment owns

| Owned concept | Meaning |
| --- | --- |
| Payment lifecycle | Current legal business state and allowed transitions |
| Payment identities | SIXPAY identity, public reference and external TRESOR PAY reference |
| External subscription reference | Traceability only |
| Requested amount | Positive `Money` with explicit currency |
| Protected account references | Debtor and resolved CUT references |
| Business decisions | Authorization, verification, posting, rejection, failure, TFJ and reversal interpretations |
| Minimal decision evidence | Immutable, dated, minimized snapshots |
| Failures | Stable category, code, stage and safe message |
| Business version | Monotonic aggregate version |
| Domain events | Facts raised by accepted transitions |
| Notification intentions | Logical intent, not delivery attempts |

Payment owns an interpretation of external facts, never the external master
fact itself.

## 3. What Payment does not own

| Excluded concept | Owner | Payment interaction |
| --- | --- | --- |
| TRESOR PAY subscription lifecycle | TRESOR PAY | Stores external reference and consumes authorization decision |
| JWT, Subscription Key, credentials, JWKS | Security / Integration | Receives only minimized result |
| Banking customer/account master | Amplitude | Stores protected reference and verification snapshot |
| Balance and available funds | Amplitude | Consumes fresh funds-control result |
| CUT configuration | Accounting / protected bank configuration | Stores resolved opaque reference |
| HTTP, REST, Kafka or internal bus | Integration | Uses canonical ports/messages |
| Amplitude call execution | Integration / Accounting | Receives canonical results |
| Notification delivery | Notification | Emits intent only |
| Generic Outbox implementation | Infrastructure | Requires atomic local intent |
| TFJ matching/quarantine workflow | Accounting | Consumes matched result only |
| Unmatched TFJ results | Accounting / Operations | Never enters aggregate |
| Query projections and timeline | Reporting | Consumes facts/events |
| Full transition history | Audit / Reporting | Aggregate retains current bounded state |
| Reversal execution | Accounting / Integration | Authorizes and records outcome |

## 4. Context interactions

### Subscription / TRESOR PAY

TRESOR PAY owns subscription authority. Security and Integration validate the
signed evidence. Payment consumes the canonical result and never mutates a local
Subscription aggregate.

### Customer

Customer and Integration obtain authoritative Amplitude facts.
Payment records a minimized verification snapshot and decides progression.
`ObservedCustomer` remains a read/audit projection outside Payment.

### Accounting

Payment authorizes one financial action. Accounting and Integration execute
funds control, posting, lookup, TFJ and reversal. Payment interprets canonical
outcomes and owns the legal lifecycle transition.

### Notification

Payment emits a business event or notification intent. Notification owns
delivery attempts and acknowledgements. Delivery status does not move the
financial lifecycle.

### Integration

Integration translates external payloads and canonical messages. It does not
decide Payment transitions.

## 5. Aggregate content rule

A value may enter Payment only when it:

1. protects an invariant or explains a decision;
2. is bounded;
3. is immutable or an immutable snapshot;
4. contains no raw credential or unnecessary sensitive data;
5. has a documented authoritative owner;
6. has source and observation time;
7. creates no direct dependency on another business module.

## 6. Minimal evidence policy

Payment may retain minimized authorization, banking verification, funds,
posting, matched TFJ and reversal evidence. It must never retain raw external
payloads.

## 7. Boundary invariants

| ID | Invariant |
| --- | --- |
| `PAY-BOUND-001` | Payment never owns Subscription lifecycle. |
| `PAY-BOUND-002` | Payment never stores credentials or raw tokens. |
| `PAY-BOUND-003` | Payment never stores raw Amplitude payloads. |
| `PAY-BOUND-004` | Payment never calls HTTP, Kafka or Amplitude directly. |
| `PAY-BOUND-005` | Payment never owns notification attempts. |
| `PAY-BOUND-006` | Notification failure never changes financial state. |
| `PAY-BOUND-007` | Payment never owns unmatched TFJ input. |
| `PAY-BOUND-008` | Payment records only a matched canonical TFJ result. |
| `PAY-BOUND-009` | Payment never blindly resubmits a financial command. |
| `PAY-BOUND-010` | Payment never imports another business-domain implementation. |
| `PAY-BOUND-011` | Payment retains bounded evidence, not unbounded histories. |
| `PAY-BOUND-012` | State transitions remain domain decisions. |

## 8. Confirmed ownership answers

| Question | Decision |
| --- | --- |
| Lifecycle | **YES** |
| References | **YES** |
| Amount | **YES** |
| Business decisions/results | **YES**, canonical interpretations |
| Transitions | **YES** |
| Minimal evidence | **YES** |
| Failures | **YES** |
| Business version | **YES** |
| Subscription lifecycle | **NO** |
| Credentials/JWT | **NO** |
| CUT configuration | **NO** |
| HTTP/Kafka transport | **NO** |
| Notification delivery | **NO** |
| Amplitude execution | **NO** |
| Generic Outbox implementation | **NO** |
| Accounting TFJ logic | **NO**, matched result only |
