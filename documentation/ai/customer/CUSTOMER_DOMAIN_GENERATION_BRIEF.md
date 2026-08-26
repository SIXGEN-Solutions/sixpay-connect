# SIXPAY CONNECT — Customer Verification & ObservedCustomer Brief

> Ce brief est un document de support au pilote Payment. Il n’autorise pas la
> génération autonome d’un domaine Customer transactionnel.

## 1. Métadonnées

| Champ | Valeur |
| --- | --- |
| Identifiant | `payment-pilot-customer-support-v1.1.0-rc1` |
| Domaine pilote | `payment` |
| Domaine support | `customer` |
| Modèle | `ObservedCustomer` — lecture et audit |
| Statut | `BLOCKING_DECISIONS_CLOSED_PENDING_PAYMENT_PREFLIGHT` |
| Code autorisé | Non |
| Gate | `GATE_IA_0_CUSTOMER_PREFLIGHT.md` |

## 2. Décisions structurantes

- TRESOR PAY est maître des abonnements pour le MVP.
- SIXPAY ne crée, ne valide, ne suspend et ne réactive pas d’abonnement local.
- Payment devient le domaine pilote.
- Customer fournit des contrôles bancaires et une projection d’observation.
- La projection naît dès la première demande de paiement, succès ou échec.
- La notification immédiate et la confirmation après TFJ sont deux résultats
  distincts.
- L’évolution permettant au gestionnaire de valider un abonnement depuis SIXPAY
  est différée.
- Le KYC numérique avec pièce d’identité et selfie est hors MVP.

## 3. Responsabilité de Customer dans le MVP

Customer fournit au workflow Payment :

- recherche du client bancaire;
- vérification du NIU;
- récupération de l’identité bancaire;
- recherche des comptes;
- contrôle d’appartenance du RIB/IBAN;
- contrôle du statut du compte;
- détection des blocages et oppositions;
- récupération des champs KYC requis.

Les résultats sont des faits de vérification horodatés. Ils ne transforment pas
SIXPAY en système maître du client bancaire.

## 4. ObservedCustomer

### 4.1 Finalité

`ObservedCustomer` est une projection CQRS destinée :

- au suivi des clients ayant présenté au moins une demande de paiement;
- à la consultation des paiements réussis ou échoués;
- à l’audit des décisions et appels externes;
- à la recherche opérationnelle par NIU, référence et période.

### 4.2 Données candidates

- `observedCustomerId`;
- NIU;
- raison sociale;
- téléphone et e-mail observés;
- institutions financières observées;
- références de comptes masquées;
- date de première et dernière observation;
- nombre total de paiements, succès et échecs;
- dernier statut et dernier motif d’échec;
- références Payment et identifiants de corrélation.

### 4.3 Interdictions

`ObservedCustomer` :

- n’est pas un Aggregate Root du modèle d’écriture;
- n’est pas une base KYC autoritative;
- ne porte pas le cycle de vie d’un abonnement;
- ne permet pas de modifier le client dans Amplitude;
- ne stocke pas de token, secret ou numéro de compte en clair.

## 5. Cas d’usage de lecture

- `SearchObservedCustomers`;
- `GetObservedCustomer`;
- `ListObservedCustomerPayments`;
- `GetPaymentAuditTrail`;
- `ExportObservedCustomerAudit` sous contrôle d’accès.

Filtres minimum : NIU, raison sociale, institution, statut du dernier paiement,
motif d’échec, date de première/dernière observation et période de paiement.

## 6. Événements sources candidats

- `PaymentRequestReceived`;
- `BankingVerificationCompleted`;
- `PaymentRejected`;
- `PaymentPosted`;
- `ImmediatePaymentNotificationDelivered`;
- `TreasuryIntegrationConfirmed`;
- `EndOfDayConfirmationNotificationDelivered`;
- `PaymentFailed`;
- `PaymentReversed`.

Les projections sont idempotentes sur `eventId` et reconstruisibles.

## 7. Persistance et audit

- Payment conserve la vérité transactionnelle.
- ObservedCustomer est stocké dans le modèle de lecture.
- La piste d’audit relie paiement, requête TRESOR PAY, corrélation, contrôles
  Amplitude, écritures, notifications et confirmation TFJ.
- Les payloads sensibles sont minimisés et masqués.
- La durée de conservation et les règles d’export restent à approuver.

## 8. Sécurité

Rôles candidats :

- `OPS` : consultation opérationnelle limitée;
- `MANAGER` : consultation, supervision et export autorisé;
- `AUDITOR` : lecture de la piste d’audit;
- `ADMIN` : configuration, sans accès implicite aux données sensibles.

Les recherches, consultations détaillées et exports sont audités.

## 9. Contrats et dépendances

- Registre normatif : `documentation/contracts/CONTRACT_REGISTRY.yaml`.
- Entrée principale : futur contrat Payment TRESOR PAY → SIXPAY.
- Vérifications : `amplitude-customer-verification-api-v1.yaml`, classé
  `REFERENCE_MVP` et utilisable comme référence uniquement.
- Notifications : contrats Payment immédiat et confirmation TFJ à produire.
- Contrats TRESOR PAY de demande d’autorisation et de décision : classés
  `DEFERRED_FUTURE`, `DRAFT` et `EXCLUDED`; conservés pour traçabilité, hors
  chemin critique du MVP et interdits à la génération.
- Timeout, retry et SLA Amplitude restent configurables.

## 10. Tests attendus

- création de la projection au premier paiement réussi;
- création de la projection au premier paiement échoué;
- rapprochement de plusieurs paiements sur le même NIU;
- idempotence en cas de rejeu d’événement;
- coexistence d’un client avec plusieurs institutions;
- masquage des comptes et absence de secrets;
- distinction notification immédiate / confirmation TFJ;
- reconstruction de projection;
- filtrage et contrôle d’accès;
- audit des consultations et exports.

## 11. Décisions fermées

Le registre normatif
`documentation/ai/customer/IA_0R_BLOCKING_DECISIONS.yaml` arrête les décisions
suivantes :

- `ObservedCustomerId` est un UUID stable; le NIU est un attribut, pas une clé;
- aucun rapprochement automatique n’est réalisé entre institutions;
- les comptes sont masqués et les accès suivent une matrice RBAC auditée;
- paiements et audit métier sont conservés dix ans par défaut;
- l’authentification MVP utilise Token + Subscription Key;
- timeout, retry et backoff sont configurables avec des defaults non
  contractuels;
- une écriture partielle ou inconnue n’est jamais rejouée aveuglément;
- la confirmation TFJ est distincte du résultat immédiat et doit être corrélée.

Les valeurs SLA définitives et les signatures de Gate restent des validations
externes non structurantes.

## 12. Definition of Ready

- [x] TRESOR PAY maître des abonnements MVP.
- [x] Subscription local retiré du périmètre immédiat.
- [x] Payment déclaré pilote.
- [x] ObservedCustomer limité au modèle de lecture.
- [x] Paiement et confirmation TFJ intégrés.
- [x] Contrats existants reclassifiés et règles de génération explicitées.
- [ ] Contract Pack Payment approuvé.
- [x] Décisions d’architecture bloquantes arbitrées.
- [x] Baseline sécurité, rétention et audit définie.

**Aucune génération de code Customer n’est autorisée à partir de ce brief seul.**
