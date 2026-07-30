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
| Statut | `REBASELINED_PENDING_PAYMENT_PREFLIGHT` |
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

- Entrée principale : futur contrat Payment TRESOR PAY → SIXPAY.
- Vérifications : `amplitude-customer-verification-api-v1.yaml`.
- Notifications : contrats Payment immédiat et confirmation TFJ à produire.
- Contrats d’abonnement TRESOR PAY existants : conservés pour évolution, hors
  chemin critique du MVP.
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

## 11. Décisions ouvertes

- identité stable d’ObservedCustomer lorsqu’un NIU manque ou évolue;
- critères de rapprochement entre institutions;
- données visibles selon les rôles;
- durée de rétention;
- protocole exact de confirmation TFJ;
- traitement des écritures partielles et extournes;
- valeurs définitives de timeout, retry, backoff et SLA.

## 12. Definition of Ready

- [x] TRESOR PAY maître des abonnements MVP.
- [x] Subscription local retiré du périmètre immédiat.
- [x] Payment déclaré pilote.
- [x] ObservedCustomer limité au modèle de lecture.
- [x] Paiement et confirmation TFJ intégrés.
- [ ] Contract Pack Payment approuvé.
- [ ] Décisions ouvertes arbitrées.
- [ ] Sécurité, rétention et audit approuvés.

**Aucune génération de code Customer n’est autorisée à partir de ce brief seul.**
