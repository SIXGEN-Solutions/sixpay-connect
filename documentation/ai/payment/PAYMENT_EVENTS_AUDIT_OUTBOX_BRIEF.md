# SIXPAY CONNECT — Événements, audit et Outbox Payment

> **Lot:** `6 — Événements, audit et Outbox`  
> **Branche:** `feat/payment-domain-generation-brief`  
> **Statut:** `NORMATIVE_IMPLEMENTED`  
> **Comportement Java modifié:** `NON`

## 1. Décision de périmètre

Le noyau Payment produit déjà 33 Domain Events explicites et sûrs. Le Lot 6 ne
renomme pas ces événements vers la liste candidate historique et ne crée pas
une seconde hiérarchie. Il formalise :

- les payloads sûrs et interdits ;
- la relation Domain Event → audit → Outbox → Integration Event ;
- la transaction atomique ;
- le versionnement et l’ordre ;
- la déduplication ;
- le comportement après échec Kafka ;
- les événements audit-only et publiables.

## 2. Frontière transactionnelle

```text
Payment state transition
+ Payment persistence
+ businessVersion
+ immutable audit records
+ Outbox messages
+ idempotency result when applicable
= ONE DATABASE TRANSACTION
```

La transaction est entièrement validée et écrite par la future couche
`application` avec les adaptateurs de `infrastructure.persistence` et
`infrastructure.messaging`.

L’Aggregate Root :

- ne connaît ni Kafka ni l’Outbox ;
- n’effectue aucun I/O ;
- produit uniquement des Domain Events immuables ;
- ne marque jamais un événement comme publié.

## 3. Flux normatif

```text
Payment Aggregate
    │ registers ordered Domain Events
    ▼
Application transaction
    ├── persists Payment state/version
    ├── appends one audit record per Domain Event
    ├── maps publishable events through an explicit allowlist
    ├── appends Outbox messages
    └── stores idempotency result when applicable
            │
            └── COMMIT
                    │
                    ▼
Generic Integration Outbox Relay
    ├── claims with FOR UPDATE SKIP LOCKED
    ├── publishes through configured Internal Bus/Kafka transport
    ├── preserves event identity and payload
    └── marks PUBLISHED or RETRYABLE
```

## 4. Échec Kafka après commit

Un échec de publication après le commit :

```text
ne rollbacke pas Payment
ne rollbacke pas l’audit
ne supprime pas l’Outbox
ne rejoue pas la transition
ne rejoue pas le débit ou le reversal
```

La ligne Outbox reste `PENDING` ou devient `RETRYABLE`. Le relais générique
réessaie la publication du **même événement**, avec le même `eventId`,
`eventType`, `schemaVersion`, `occurredAt`, payload et hash.

Après dépassement du seuil approuvé, la ligne devient `QUARANTINED` et une
alerte opérationnelle est produite.

## 5. Versionnement et ordre

Pour une mutation réussie :

```text
businessVersion = previousVersion + 1
all events.aggregateVersion = businessVersion
eventSequence = 1..N
```

Ordre global par Payment :

```text
partition key = paymentId
order = aggregateVersion, eventSequence
```

Une republication conserve l’identité. Une nouvelle version du contrat
d’intégration exige un nouveau `schemaVersion` et une compatibilité explicite.

## 6. Déduplication

| Niveau | Clé | Règle |
| --- | --- | --- |
| Outbox | `eventId` | une ligne logique par événement |
| Relais | `eventId` | republication autorisée, identité inchangée |
| Consommateur | `eventId` | traitement et processed marker atomiques |
| Notification | `sourceEventId + notificationPhase` | aucun renvoi financier |
| Process request financier | clé métier spécifique | jamais `eventId` seul pour autoriser un débit |

Même `eventId` avec un payload différent :

```text
QUARANTINE_AND_ALERT
```

## 7. Domain Events et Integration Events

```text
com.sixpay.payment.domain.event
→ faits métier internes de l’Aggregate Root

com.sixpay.payment.events
→ futurs contrats d’intégration versionnés
```

La publication directe d’un Domain Event est interdite. Un mapper explicite par
allowlist construit le contrat d’intégration. La sérialisation automatique de
`Payment`, `PaymentState`, d’un snapshot ou d’un Value Object protégé est
interdite.

## 8. Données autorisées

Les métadonnées communes autorisées sont :

```text
eventId
paymentId
publicPaymentReference
correlationId
paymentStatus
aggregateVersion
eventSequence
causationId?
occurredAt
safe result/failure code?
opaque or masked posting reference?
approved evidence fingerprint?
```

`ExternalPaymentReference` n’est exposée que lorsque le contrat consommateur
l’exige explicitement.

## 9. Données interdites

```text
Bearer Token
Subscription Key
PIN
API key, password, secret or private key
raw JWT claims/signature/JWKS
clear debtor or Treasury account
full RIB
raw KYC or customer identity
available or ledger balance
raw TRESOR PAY/Amplitude/TFJ payload
free-form provider error
stack trace, SQL, endpoint or infrastructure topology
operator credential or bank instruction document
```

## 10. Catalogue exhaustif des 33 événements

Événements avec Outbox : **26**  
Événements audit-only : **7**

| ID | Domain Event | Rôle | Schéma | Payload minimal | Données interdites | Consommateurs | Audit | Outbox | Déduplication | Ordre |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- | --- | --- |
| `PAY-EVT-001` | `PaymentReceived` | `LIFECYCLE_FACT` | v1 | `externalPaymentReference`: ExternalPaymentReference (BUSINESS_REFERENCE)<br>`source`: PaymentSource (INTERNAL_NON_SENSITIVE)<br>`financialInstitutionCode`: FinancialInstitutionCode (INTERNAL_NON_SENSITIVE)<br>`requestedAmount`: MoneyPayload (RESTRICTED_FINANCIAL)<br>`maskedDebtorAccountReference`: string (RESTRICTED_DISPLAY)<br>`receivedAt`: Instant (INTERNAL_NON_SENSITIVE) | ExternalSubscriptionReference unless an approved consumer contract requires it.<br>Clear debtor account.<br>Inbound headers or raw request.<br>Authorization token or Subscription Key. | `customer` — Create or update the initial ObservedCustomer projection from minimized Payment facts.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-002` | `PaymentAuthorizationCheckingStarted` | `PROCESS_REQUEST` | v1 | `startedAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `security` — Resolve and validate canonical TRESOR PAY authorization evidence.<br>`integration` — Coordinate local token/JWKS validation without exposing credentials to Payment.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-003` | `PaymentAuthorizationDecisionRecorded` | `EVIDENCE_FACT` | v1 | `outcome`: APPROVED\|REJECTED (INTERNAL_NON_SENSITIVE)<br>`authorizationEvidenceReference`: AuthorizationEvidenceReference (RESTRICTED_SECURITY_EVIDENCE)<br>`evidenceFingerprint`: EvidenceFingerprint (INTERNAL_SECURITY_METADATA)<br>`rejectionCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`acceptedAt`: Instant (INTERNAL_NON_SENSITIVE) | Raw JWT.<br>Raw jti.<br>Raw claims.<br>Signature bytes.<br>JWKS document. | `reporting` — Update Payment search and immutable timeline projections. | `true` | `false` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-004` | `PaymentBankingVerificationRequested` | `PROCESS_REQUEST` | v1 | `financialInstitutionCode`: FinancialInstitutionCode (INTERNAL_NON_SENSITIVE)<br>`debtorAccountBindingFingerprint`: AccountBindingFingerprint (INTERNAL_SECURITY_METADATA)<br>`requestedAt`: Instant (INTERNAL_NON_SENSITIVE) | Clear account.<br>Account integration token.<br>NIU or customer identity values.<br>Requested KYC payload. | `customer` — Execute or reuse the canonical fresh customer/account verification workflow.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-005` | `PaymentRejected` | `TERMINAL_FACT` | v1 | `failureCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`failureCategory`: FailureCategory (INTERNAL_NON_SENSITIVE)<br>`failureStage`: FailureStage (INTERNAL_NON_SENSITIVE)<br>`finalizedAt`: Instant (INTERNAL_NON_SENSITIVE) | Human free-form provider error.<br>Raw KYC or account data.<br>Stack trace. | `customer` — Update ObservedCustomer rejection and decision counters.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-006` | `PaymentImmediateResultAvailable` | `RESULT_INTENT` | v1 | `externalPaymentReference`: ExternalPaymentReference (BUSINESS_REFERENCE)<br>`resultType`: REJECTED\|FAILED\|PROCESSING\|POSTED_PENDING_TFJ\|REVERSAL_REQUIRED (INTERNAL_NON_SENSITIVE)<br>`failureCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`principalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`businessDate`: LocalDate (INTERNAL_NON_SENSITIVE)<br>`availableAt`: Instant (INTERNAL_NON_SENSITIVE) | Customer identity.<br>Clear account.<br>Provider diagnostics.<br>Full PaymentFailure.safeMessage by default. | `notification` — Create or reuse the immediate TRESOR PAY NotificationDelivery.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-007` | `PaymentBankingVerificationRecorded` | `EVIDENCE_FACT` | v1 | `verificationId`: BankingVerificationId (INTERNAL_NON_SENSITIVE)<br>`outcome`: VERIFIED\|REJECTED\|INDETERMINATE (INTERNAL_NON_SENSITIVE)<br>`checkResults`: bounded list<SafeCheckResult> (INTERNAL_NON_SENSITIVE)<br>`evidenceFingerprint`: EvidenceFingerprint (INTERNAL_SECURITY_METADATA)<br>`acceptedAt`: Instant (INTERNAL_NON_SENSITIVE) | NIU.<br>Customer name/contact.<br>KYC values.<br>Raw customer/account reference.<br>Available balance. | `customer` — Update ObservedCustomer with minimized observed banking outcomes.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-008` | `PaymentFundsControlRequested` | `PROCESS_REQUEST` | v1 | `financialInstitutionCode`: FinancialInstitutionCode (INTERNAL_NON_SENSITIVE)<br>`requestedAmount`: MoneyPayload (RESTRICTED_FINANCIAL)<br>`debtorAccountBindingFingerprint`: AccountBindingFingerprint (INTERNAL_SECURITY_METADATA)<br>`requestedAt`: Instant (INTERNAL_NON_SENSITIVE) | Clear account.<br>Account integration token.<br>Current or available balance. | `accounting` — Execute or reuse the exact amount/account read-only funds control.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-009` | `PaymentProcessingDeferred` | `LIFECYCLE_FACT` | v1 | `failureCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`failureCategory`: FailureCategory (INTERNAL_NON_SENSITIVE)<br>`failureStage`: FailureStage (INTERNAL_NON_SENSITIVE)<br>`retryDisposition`: RetryDisposition (INTERNAL_NON_SENSITIVE)<br>`deferredAt`: Instant (INTERNAL_NON_SENSITIVE) | Stack trace.<br>Endpoint.<br>Raw provider error.<br>Credential. | `reporting` — Update Payment search and immutable timeline projections. | `true` | `false` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-010` | `PaymentFundsControlRecorded` | `EVIDENCE_FACT` | v1 | `verificationReference`: FundsVerificationReference (INTERNAL_NON_SENSITIVE)<br>`outcome`: VERIFIED\|REJECTED\|INDETERMINATE (INTERNAL_NON_SENSITIVE)<br>`checkResults`: bounded list<SafeCheckResult> (INTERNAL_NON_SENSITIVE)<br>`validUntil`: Instant (INTERNAL_NON_SENSITIVE)<br>`evidenceFingerprint`: EvidenceFingerprint (INTERNAL_SECURITY_METADATA)<br>`acceptedAt`: Instant (INTERNAL_NON_SENSITIVE) | Available amount.<br>Ledger balance.<br>Clear account.<br>Raw Amplitude response. | `reporting` — Update Payment search and immutable timeline projections. | `true` | `false` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-011` | `PaymentTreasuryAccountResolutionRequested` | `PROCESS_REQUEST` | v1 | `financialInstitutionCode`: FinancialInstitutionCode (INTERNAL_NON_SENSITIVE)<br>`allocationIntentFingerprint`: EvidenceFingerprint (INTERNAL_SECURITY_METADATA)<br>`requestedAt`: Instant (INTERNAL_NON_SENSITIVE) | Inbound Treasury account.<br>Treasury account token.<br>Clear CUT account. | `accounting` — Resolve the protected CUT/Treasury configuration for the Payment bank and allocation intent.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-012` | `PaymentTreasuryAccountResolutionRecorded` | `EVIDENCE_FACT` | v1 | `outcome`: RESOLVED\|REJECTED (INTERNAL_NON_SENSITIVE)<br>`treasuryConfigurationId`: string (RESTRICTED_BANK_CONFIGURATION)<br>`configurationVersion`: string (RESTRICTED_BANK_CONFIGURATION)<br>`maskedTreasuryAccountReference`: string (RESTRICTED_DISPLAY)<br>`rejectionCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`evidenceFingerprint`: EvidenceFingerprint (INTERNAL_SECURITY_METADATA)<br>`acceptedAt`: Instant (INTERNAL_NON_SENSITIVE) | Treasury account token.<br>Clear CUT account.<br>Raw protected configuration. | `reporting` — Update Payment search and immutable timeline projections. | `true` | `false` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-013` | `PaymentApprovedForPosting` | `LIFECYCLE_FACT` | v1 | `financialInstitutionCode`: FinancialInstitutionCode (INTERNAL_NON_SENSITIVE)<br>`requestedAmount`: MoneyPayload (RESTRICTED_FINANCIAL)<br>`approvedAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `reporting` — Update Payment search and immutable timeline projections. | `true` | `false` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-014` | `PaymentPostingAuthorized` | `FINANCIAL_AUTHORIZATION_FACT` | v1 | `postingInstructionId`: PostingInstructionId (INTERNAL_NON_SENSITIVE)<br>`postingIdempotencyKey`: PostingIdempotencyKey (RESTRICTED_OPERATIONAL_REFERENCE)<br>`postingInstructionFingerprint`: PostingInstructionFingerprint (INTERNAL_SECURITY_METADATA)<br>`authorizedAt`: Instant (INTERNAL_NON_SENSITIVE) | Debtor account token.<br>Treasury account token.<br>Clear account. | `reporting` — Update Payment search and immutable timeline projections. | `true` | `false` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-015` | `PaymentPostingRequested` | `PROCESS_REQUEST` | v1 | `postingInstructionId`: PostingInstructionId (INTERNAL_NON_SENSITIVE)<br>`postingIdempotencyKey`: PostingIdempotencyKey (RESTRICTED_OPERATIONAL_REFERENCE)<br>`postingInstructionFingerprint`: PostingInstructionFingerprint (INTERNAL_SECURITY_METADATA)<br>`financialInstitutionCode`: FinancialInstitutionCode (INTERNAL_NON_SENSITIVE)<br>`requestedAmount`: MoneyPayload (RESTRICTED_FINANCIAL)<br>`requestedAt`: Instant (INTERNAL_NON_SENSITIVE) | Clear debtor account.<br>Debtor account token in general integration event payload.<br>Treasury account token in general integration event payload.<br>A replacement posting identity. | `accounting` — Create or reuse the unique posting workflow and obtain protected execution material through the approved internal port.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-016` | `PaymentPostingOutcomeRecorded` | `FINANCIAL_EVIDENCE_FACT` | v1 | `postingInstructionId`: PostingInstructionId (INTERNAL_NON_SENSITIVE)<br>`outcome`: COMPLETED\|REJECTED_NO_FINANCIAL_EFFECT\|DEBIT_CONFIRMED_CUT_CREDIT_PENDING\|REVERSAL_REQUIRED\|UNKNOWN (INTERNAL_NON_SENSITIVE)<br>`principalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`debitLeg`: PostingLegPayload (RESTRICTED_FINANCIAL)<br>`cutCreditLeg`: PostingLegPayload (RESTRICTED_FINANCIAL)<br>`businessDate`: LocalDate (INTERNAL_NON_SENSITIVE)<br>`rejectionCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`nextAction`: PostingNextAction (INTERNAL_NON_SENSITIVE)<br>`evidenceFingerprint`: EvidenceFingerprint (INTERNAL_SECURITY_METADATA)<br>`acceptedAt`: Instant (INTERNAL_NON_SENSITIVE) | Post-debit balance.<br>Clear account.<br>Raw core-banking entries.<br>Raw Amplitude response. | `accounting` — Correlate Payment decision with the canonical posting/reconciliation workflow.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-017` | `PaymentEndOfDayTrackingRequested` | `PROCESS_REQUEST` | v1 | `financialInstitutionCode`: FinancialInstitutionCode (INTERNAL_NON_SENSITIVE)<br>`principalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`businessDate`: LocalDate (INTERNAL_NON_SENSITIVE)<br>`requestedAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `accounting` — Register unique TFJ matching and scheduled read-only fallback reconciliation.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-018` | `PaymentDebitConfirmed` | `FINANCIAL_FACT` | v1 | `postingInstructionId`: PostingInstructionId (INTERNAL_NON_SENSITIVE)<br>`principalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`debitLegReference`: BankPostingReference.debitLeg (RESTRICTED_FINANCIAL_REFERENCE)<br>`businessDate`: LocalDate (INTERNAL_NON_SENSITIVE)<br>`debitedAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `accounting` — Update reconciliation views without issuing another debit.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-019` | `PaymentPostingOutcomeLookupRequested` | `PROCESS_REQUEST` | v1 | `postingInstructionId`: PostingInstructionId (INTERNAL_NON_SENSITIVE)<br>`postingIdempotencyKey`: PostingIdempotencyKey (RESTRICTED_OPERATIONAL_REFERENCE)<br>`principalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`lookupMode`: IDEMPOTENCY_KEY\|BANK_REFERENCE (INTERNAL_NON_SENSITIVE)<br>`unknownSince`: Instant (INTERNAL_NON_SENSITIVE)<br>`requestedAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `accounting` — Perform authoritative read-only posting lookup using the original instruction identity.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-020` | `PaymentReversalRequired` | `LIFECYCLE_AND_PROCESS_REQUEST` | v1 | `principalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`reasonCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`sourceStage`: POSTING\|TFJ\|REVERSAL (INTERNAL_NON_SENSITIVE)<br>`knownDebitStatus`: PostingLegStatus (INTERNAL_NON_SENSITIVE)<br>`knownCutCreditStatus`: PostingLegStatus (INTERNAL_NON_SENSITIVE)<br>`requiredAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `accounting` — Open or reuse the reversal/reconciliation case; do not reverse automatically.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-021` | `PaymentPostingOutcomeResolved` | `FINANCIAL_EVIDENCE_FACT` | v1 | `postingInstructionId`: PostingInstructionId (INTERNAL_NON_SENSITIVE)<br>`previousOutcome`: UNKNOWN (INTERNAL_NON_SENSITIVE)<br>`resolvedOutcome`: COMPLETED\|REJECTED_NO_FINANCIAL_EFFECT\|DEBIT_CONFIRMED_CUT_CREDIT_PENDING\|REVERSAL_REQUIRED (INTERNAL_NON_SENSITIVE)<br>`principalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`debitLeg`: PostingLegPayload (RESTRICTED_FINANCIAL)<br>`cutCreditLeg`: PostingLegPayload (RESTRICTED_FINANCIAL)<br>`businessDate`: LocalDate (INTERNAL_NON_SENSITIVE)<br>`rejectionCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`evidenceFingerprint`: EvidenceFingerprint (INTERNAL_SECURITY_METADATA)<br>`resolvedAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `accounting` — Close or update the posting reconciliation case.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-022` | `PaymentEndOfDayConfirmationRecorded` | `TREASURY_EVIDENCE_FACT` | v1 | `confirmationId`: TfjConfirmationId (INTERNAL_NON_SENSITIVE)<br>`financialInstitutionCode`: FinancialInstitutionCode (INTERNAL_NON_SENSITIVE)<br>`businessDate`: LocalDate (INTERNAL_NON_SENSITIVE)<br>`principalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`tfjStatus`: INTEGRATED\|FAILED (INTERNAL_NON_SENSITIVE)<br>`failureCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`recoveryAction`: MANUAL_RECONCILIATION\|REVERSAL_REVIEW\|REVERSAL_REQUIRED (INTERNAL_NON_SENSITIVE)<br>`confirmedAt`: Instant (INTERNAL_NON_SENSITIVE)<br>`matchedAt`: Instant (INTERNAL_NON_SENSITIVE)<br>`evidenceFingerprint`: EvidenceFingerprint (INTERNAL_SECURITY_METADATA) | Raw TFJ payload or file.<br>Unmatched confirmation data.<br>Clear CUT or debtor account. | `accounting` — Close or update the uniquely matched TFJ reconciliation workflow.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-023` | `TreasuryIntegrationConfirmed` | `TERMINAL_FINANCIAL_FACT` | v1 | `confirmationId`: TfjConfirmationId (INTERNAL_NON_SENSITIVE)<br>`principalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`businessDate`: LocalDate (INTERNAL_NON_SENSITIVE)<br>`confirmedAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `customer` — Update ObservedCustomer success and finality counters.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-024` | `PaymentFinalResultAvailable` | `RESULT_INTENT` | v1 | `externalPaymentReference`: ExternalPaymentReference (BUSINESS_REFERENCE)<br>`resultType`: TREASURY_INTEGRATED (INTERNAL_NON_SENSITIVE)<br>`principalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`businessDate`: LocalDate (INTERNAL_NON_SENSITIVE)<br>`confirmationId`: TfjConfirmationId (INTERNAL_NON_SENSITIVE)<br>`availableAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `notification` — Create or reuse the final TRESOR PAY NotificationDelivery.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-025` | `PaymentTreasuryReconciliationRequired` | `PROCESS_REQUEST` | v1 | `confirmationId`: TfjConfirmationId (INTERNAL_NON_SENSITIVE)<br>`principalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`businessDate`: LocalDate (INTERNAL_NON_SENSITIVE)<br>`failureCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`recoveryAction`: MANUAL_RECONCILIATION\|REVERSAL_REVIEW (INTERNAL_NON_SENSITIVE)<br>`requiredAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `accounting` — Open or reuse a manual Treasury reconciliation case.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-026` | `PaymentReversalAuthorized` | `FINANCIAL_AUTHORIZATION_FACT` | v1 | `reversalInstructionId`: ReversalInstructionId (INTERNAL_NON_SENSITIVE)<br>`reversalIdempotencyKey`: ReversalIdempotencyKey (RESTRICTED_OPERATIONAL_REFERENCE)<br>`originalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`authorizationType`: BANK_INSTRUCTION\|APPROVED_RUNBOOK (INTERNAL_NON_SENSITIVE)<br>`authorizationReference`: ReversalAuthorizationReference (RESTRICTED_OPERATIONAL_REFERENCE)<br>`reasonCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`authorizedAt`: Instant (INTERNAL_NON_SENSITIVE) | Bank instruction document.<br>Operator credential.<br>Free-form operator comment. | `reporting` — Update Payment search and immutable timeline projections. | `true` | `false` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-027` | `PaymentReversalRequested` | `PROCESS_REQUEST` | v1 | `reversalInstructionId`: ReversalInstructionId (INTERNAL_NON_SENSITIVE)<br>`reversalIdempotencyKey`: ReversalIdempotencyKey (RESTRICTED_OPERATIONAL_REFERENCE)<br>`originalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`instructionFingerprint`: EvidenceFingerprint (INTERNAL_SECURITY_METADATA)<br>`requestedAt`: Instant (INTERNAL_NON_SENSITIVE) | Original posting idempotency key reuse.<br>Clear account.<br>Raw bank instruction. | `accounting` — Submit or reuse the explicitly authorized reversal instruction.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-028` | `PaymentReversalOutcomeRecorded` | `FINANCIAL_EVIDENCE_FACT` | v1 | `reversalInstructionId`: ReversalInstructionId (INTERNAL_NON_SENSITIVE)<br>`outcome`: REVERSED\|REJECTED\|NOT_ALLOWED\|UNKNOWN (INTERNAL_NON_SENSITIVE)<br>`reversalReference`: ReversalReference (RESTRICTED_FINANCIAL_REFERENCE)<br>`reversalEntryReference`: string (RESTRICTED_FINANCIAL_REFERENCE)<br>`reasonCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`evidenceFingerprint`: EvidenceFingerprint (INTERNAL_SECURITY_METADATA)<br>`acceptedAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `accounting` — Update the reversal and reconciliation workflow.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-029` | `PaymentReversalResultAvailable` | `RESULT_INTENT` | v1 | `externalPaymentReference`: ExternalPaymentReference (BUSINESS_REFERENCE)<br>`resultType`: REVERSED\|REVERSAL_REQUIRED (INTERNAL_NON_SENSITIVE)<br>`originalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`reversalReference`: ReversalReference (RESTRICTED_FINANCIAL_REFERENCE)<br>`failureCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`availableAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `notification` — Create or reuse the reversal-phase TRESOR PAY NotificationDelivery.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-030` | `PaymentReversalOutcomeLookupRequested` | `PROCESS_REQUEST` | v1 | `reversalInstructionId`: ReversalInstructionId (INTERNAL_NON_SENSITIVE)<br>`reversalIdempotencyKey`: ReversalIdempotencyKey (RESTRICTED_OPERATIONAL_REFERENCE)<br>`reversalReference`: ReversalReference (RESTRICTED_FINANCIAL_REFERENCE)<br>`requestedAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `accounting` — Perform authoritative read-only lookup for the original reversal instruction.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-031` | `PaymentReversalOutcomeResolved` | `FINANCIAL_EVIDENCE_FACT` | v1 | `reversalInstructionId`: ReversalInstructionId (INTERNAL_NON_SENSITIVE)<br>`previousOutcome`: UNKNOWN (INTERNAL_NON_SENSITIVE)<br>`resolvedOutcome`: REVERSED\|REJECTED\|NOT_ALLOWED (INTERNAL_NON_SENSITIVE)<br>`reversalReference`: ReversalReference (RESTRICTED_FINANCIAL_REFERENCE)<br>`reversalEntryReference`: string (RESTRICTED_FINANCIAL_REFERENCE)<br>`reasonCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`evidenceFingerprint`: EvidenceFingerprint (INTERNAL_SECURITY_METADATA)<br>`resolvedAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `accounting` — Close or update the reversal outcome-reconciliation case.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-032` | `PaymentFailedWithoutFinancialEffect` | `TERMINAL_FACT` | v1 | `failureCode`: FailureCode (INTERNAL_NON_SENSITIVE)<br>`failureCategory`: FailureCategory (INTERNAL_NON_SENSITIVE)<br>`failureStage`: FailureStage (INTERNAL_NON_SENSITIVE)<br>`finalizedAt`: Instant (INTERNAL_NON_SENSITIVE) | Stack trace.<br>Endpoint or topology.<br>Raw provider error.<br>Customer/account data. | `customer` — Update ObservedCustomer technical-failure counters and last outcome.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |
| `PAY-EVT-033` | `PaymentReversed` | `TERMINAL_FINANCIAL_FACT` | v1 | `reversalInstructionId`: ReversalInstructionId (INTERNAL_NON_SENSITIVE)<br>`originalPostingReference`: BankPostingReference.principal (RESTRICTED_FINANCIAL_REFERENCE)<br>`reversalReference`: ReversalReference (RESTRICTED_FINANCIAL_REFERENCE)<br>`reversedAt`: Instant (INTERNAL_NON_SENSITIVE) | Denylist globale Lot 6. | `customer` — Update ObservedCustomer final reversal outcome.<br>`reporting` — Update Payment search and immutable timeline projections. | `true` | `true` | `eventId` | `aggregateVersion,eventSequence` |

## 11. Reproductibilité

Chaque événement publiable peut être reproduit sans relire ou remuter
l’Aggregate Root à partir de :

```text
persisted Outbox payload
+ eventId
+ eventType
+ schemaVersion
+ payloadHash
+ catalogue de schéma
```

La reconstruction depuis le seul état courant de Payment est interdite, car
elle pourrait perdre l’état historique exact de l’événement.
