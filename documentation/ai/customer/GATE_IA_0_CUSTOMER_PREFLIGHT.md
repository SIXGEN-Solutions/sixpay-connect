# SIXPAY CONNECT — Gate IA-0R Customer / Payment

| Métadonnée | Valeur |
| --- | --- |
| Statut | **IA-0R CLOSED — READY FOR PAYMENT PREFLIGHT** |
| Type | Réalignement métier et architectural, sans génération de code |
| Branche | `feat/customer-foundation-contract` |
| Commit de référence | `ceb11c732f0c74927238863c37860256c8b200c4` |
| Domaine pilote | `payment` |
| Support | `customer`, `integration`, `notification` |
| Niveau d’autonomie | **A — Assistance** |

## 1. Décision de Gate

Le cadrage initial Customer + Subscription est remplacé par un pilote Payment.
Pour le MVP, **TRESOR PAY est le système maître des abonnements**. SIXPAY ne crée,
ne valide, ne suspend et ne réactive aucun abonnement local.

Le périmètre immédiat devient :

```text
Payment
  + vérifications bancaires
  + ObservedCustomer (lecture/audit)
  + exécution comptable
  + notification immédiate
  + confirmation après TFJ
```

Les décisions d’architecture bloquantes sont fermées dans
`documentation/ai/customer/IA_0R_BLOCKING_DECISIONS.yaml`. La génération de
code reste interdite jusqu’à validation du Contract Pack Payment et obtention
des signatures de Gate.

## 2. Parcours métier de référence

### 2.1 Abonnement préalable — hors SIXPAY pour le MVP

1. Le client crée son abonnement sur TRESOR PAY.
2. TRESOR PAY place l’abonnement en attente de validation bancaire.
3. Le gestionnaire réalise le KYC depuis l’interface TRESOR PAY.
4. TRESOR PAY conserve et fait évoluer le statut de l’abonnement.
5. SIXPAY ne maintient aucune copie autoritative de cet abonnement.

### 2.2 Paiement — périmètre pilote SIXPAY

1. TRESOR PAY transmet un ordre de paiement à SIXPAY.
2. SIXPAY persiste la demande avant tout traitement externe.
3. SIXPAY crée ou actualise une projection `ObservedCustomer`.
4. SIXPAY contrôle l’identité bancaire, le compte, son statut, les
   blocages/oppositions et la disponibilité des fonds via Amplitude.
5. Si les contrôles réussissent, la banque débite le compte client et crédite le
   Compte Unique du Trésor (CUT).
6. SIXPAY notifie TRESOR PAY du résultat immédiat.
7. Après les Travaux de Fin de Journée (TFJ), SIXPAY reçoit ou rapproche la
   confirmation d’intégration au compte du Trésor.
8. SIXPAY notifie TRESOR PAY du résultat définitif.

## 3. Séparation des horizons

| Capacité | MVP | Évolution ultérieure |
| --- | --- | --- |
| Création et statut d’abonnement | TRESOR PAY | TRESOR PAY reste maître |
| Validation bancaire d’abonnement | Interface TRESOR PAY | Interface SIXPAY appelant TRESOR PAY |
| KYC | Manuel / système bancaire | KYC numérique, pièce d’identité et selfie |
| Traitement du paiement | SIXPAY | SIXPAY |
| Vue clients payeurs et audit | SIXPAY, dès la première demande | Enrichissement analytique |
| Confirmation TFJ | SIXPAY | Automatisation et rapprochement avancé |

## 4. Frontières de domaine

### Payment

`Payment` est l’Aggregate Root pilote. Il porte la référence TRESOR PAY,
l’idempotence, le montant, les comptes concernés, les résultats de contrôles,
les écritures, les notifications et la confirmation TFJ.

### Customer

Le domaine Customer ne porte pas d’agrégat transactionnel dans ce pilote. Il
fournit `ObservedCustomer`, projection de lecture construite dès la première
demande de paiement, réussie ou échouée.

### Subscription

Le module Subscription est explicitement différé. Aucun repository, workflow ou
machine à états locale d’abonnement ne doit être généré pour le MVP.

### Integration et Notification

`integration` contient les adaptateurs TRESOR PAY et Amplitude.
`notification` assure une livraison fiable avec outbox, rejeu contrôlé, retry,
backoff et DLQ.

## 5. Modèle candidat

### Payment

- identité : `PaymentId`;
- références : `tresorPayRequestId`, `paymentReference`;
- corrélation : `correlationId`, `idempotencyKey`;
- payeur observé : NIU, raison sociale, téléphone, e-mail;
- banque et compte : institution, RIB/IBAN ou numéro client;
- montant, devise et CUT;
- résultats des diligences;
- références d’écritures;
- état de notification immédiate;
- état et date de confirmation TFJ;
- audit technique et métier.

### ObservedCustomer

- identifiant de projection;
- NIU et identité observée;
- coordonnées observées;
- institutions et comptes masqués observés;
- première et dernière dates de paiement;
- nombre de demandes, succès et échecs;
- dernière décision et dernier motif d’échec;
- références de paiement et corrélations auditables.

`ObservedCustomer` n’est ni une preuve KYC autoritative, ni un abonnement, ni un
client bancaire maître.

## 6. États Payment candidats

```text
RECEIVED
AUTHORIZATION_CHECKING
BANKING_CHECKING
REJECTED
APPROVED
POSTING
ACCOUNTING_OUTCOME_UNKNOWN
REVERSAL_REQUIRED
DEBITED
CUT_CREDITED
NOTIFIED
PENDING_END_OF_DAY_CONFIRMATION
TREASURY_INTEGRATED
FAILED
REVERSAL_PENDING
REVERSED
```

Le Contract Pack Payment doit traduire ces états et la politique d’écriture
partielle du registre IA-0R sans les réinterpréter.

## 7. Invariants minimum

- toute demande est persistée avant les appels externes;
- `tresorPayRequestId` et `Idempotency-Key` empêchent le double traitement;
- une réémission identique restitue le résultat existant;
- une clé réutilisée avec un payload différent est rejetée;
- aucun débit n’est lancé sans contrôles bancaires favorables;
- un échec reste visible dans Payment et ObservedCustomer;
- les données sensibles sont minimisées, masquées et auditées;
- la notification immédiate ne vaut pas confirmation TFJ;
- la confirmation TFJ est corrélée au paiement et idempotente;
- les notifications utilisent une outbox et sont rejouables.

## 8. Contrats

Le registre normatif
`documentation/contracts/CONTRACT_REGISTRY.yaml` reclassifie les contrats
existants sans déplacer leurs fichiers :

- `amplitude-customer-verification-api-v1.yaml` : `REFERENCE_MVP`,
  `PENDING_APPROVAL`, `REFERENCE_ONLY`; il supporte les vérifications bancaires,
  avec timeout/retry/SLA configurables, mais ne couvre pas l’exécution du
  paiement;
- `tresorpay-authorization-request-api-v1.yaml` : `DEFERRED_FUTURE`, `DRAFT`,
  `EXCLUDED`; aucun usage dans le MVP;
- `tresorpay-authorization-decision-webhook-v1.yaml` : `DEFERRED_FUTURE`,
  `DRAFT`, `EXCLUDED`; aucun usage dans le MVP.

La présence d’un contrat différé dans le dépôt ne l’autorise ni pour
l’implémentation, ni pour la génération de code. Sa réactivation exige une
décision de périmètre et une nouvelle approbation.

Contract Pack à produire avant implémentation Payment :

- API entrante de paiement TRESOR PAY → SIXPAY;
- opérations de contrôle et de comptabilisation Amplitude;
- notification du résultat immédiat;
- notification de confirmation TFJ;
- schémas d’événements, erreurs RFC 7807 et règles d’idempotence.

## 9. Ordre de réalisation révisé

1. **IA-0R** — réalignement du périmètre;
2. **IA-0P** — préflight Payment;
3. **IA-0.5P** — Contract Pack Payment;
4. **IA-1P** — modèle Payment et règles de domaine;
5. **IA-2P** — persistance, idempotence et audit;
6. **IA-3P** — intégrations Amplitude/TRESOR PAY et outbox;
7. **IA-4P** — projection ObservedCustomer et APIs de lecture;
8. **IA-5P** — écran clients/paiements et piste d’audit;
9. **IA-6P** — confirmation TFJ et rapprochement;
10. **IA-7P** — sécurité, résilience, observabilité et recette.

## 10. Décisions bloquantes fermées

Le registre IA-0R formalise :

- l’identité stable d’ObservedCustomer et l’absence de merge interbancaire
  automatique;
- la matrice d’accès, le masquage et la rétention;
- Token + Subscription Key comme authentification du MVP;
- les defaults configurables de timeout, retry et backoff;
- le traitement prudent des résultats comptables partiels ou inconnus;
- le protocole de confirmation TFJ, sa corrélation et sa stratégie de
  rapprochement.

Les valeurs SLA définitives peuvent être fournies ultérieurement par la banque
sans modifier ces décisions.

## 11. Conditions de sortie

- [x] TRESOR PAY déclaré maître des abonnements du MVP.
- [x] Gestion locale de l’abonnement retirée du périmètre immédiat.
- [x] `ObservedCustomer` défini comme modèle de lecture.
- [x] Payment positionné comme domaine pilote.
- [x] MVP séparé des évolutions d’abonnement.
- [x] Parcours de paiement et confirmation TFJ intégrés au cadrage.
- [x] Contrats existants formellement reclassifiés dans un registre normatif.
- [x] Décisions d’architecture bloquantes formellement fermées.
- [ ] Contract Pack Payment validé.
- [ ] Signatures formelles Product, Architecture, Security, Integration et
  Operations obtenues.

**Verdict : IA-0R techniquement fermé ; passage autorisé vers IA-0P. La
génération de code reste interdite avant IA-0.5P et les signatures de Gate.**
