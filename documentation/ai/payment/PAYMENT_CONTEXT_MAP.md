# SIXPAY CONNECT — Payment Context Map

## 1. Identification

| Propriété | Valeur |
| --- | --- |
| Gate | `IA-0P — Payment Preflight` |
| Étape | `0P.3 — Fixer les responsabilités des systèmes` |
| Branche | `feat/payment-contract-pack` |
| Commit analysé | `9f5730a214863623e6792a41b3e68712452487c9` |
| Domaine pilote | `payment` |
| Statut | `CONTEXT_MAP_ESTABLISHED` |
| Génération de code | **Interdite** |
| Étape suivante | `0P.4 — Décrire les parcours de paiement` |

## 2. Objectif

Cette Context Map fixe :

- le système maître de chaque information et opération ;
- la distinction entre propriété métier, copie locale et projection ;
- les responsabilités des modules SIXPAY ;
- les ports et sens d’échange entre domaines ;
- les dépendances autorisées ;
- les responsabilités interdites.

Elle complète :

- `PAYMENT_SOURCE_BASELINE.md` ;
- la section `Scope / Out of Scope` de
  `GATE_IA_0P_PAYMENT_PREFLIGHT.md` ;
- `IA_0R_BLOCKING_DECISIONS.yaml`.

## 3. Principes de responsabilité

### 3.1 Système maître

Le système maître est l’autorité qui peut créer ou modifier la vérité métier
de référence. Une copie présente dans SIXPAY ne transfère jamais cette
autorité.

### 3.2 Propriétaire de processus

Le propriétaire d’un processus décide de son cycle de vie et de ses
transitions. Il peut orchestrer des opérations dont les données sont détenues
par d’autres systèmes.

Ainsi :

- TRESOR PAY est maître de l’ordre demandé ;
- SIXPAY est maître du Payment traité ;
- Amplitude est maître de l’effet bancaire réellement exécuté.

### 3.3 Snapshot

Un snapshot est une photographie immuable des faits externes utilisés pour
prendre une décision. Il sert à l’audit, mais ne devient pas la vérité courante
du système source.

### 3.4 Projection

Une projection est reconstruite à partir d’événements ou de faits persistés.
Elle sert à la lecture et ne porte aucune décision transactionnelle.

`ObservedCustomer` est une projection, pas un Aggregate Root de commande.

### 3.5 Anti-corruption

Les modèles TRESOR PAY et Amplitude ne traversent pas directement le domaine
Payment. Le module Integration les traduit vers les contrats canoniques
SIXPAY.

## 4. Matrice d’autorité principale

| Information ou opération | Système maître | Copie ou représentation SIXPAY | Module SIXPAY responsable | Règle d’autorité |
| --- | --- | --- | --- | --- |
| Souscription externe TRESOR PAY (hors MVP) | TRESOR PAY | Aucune copie autoritative ; référence externe seulement si nécessaire à la trace | Aucun module de souscription externe dans le flux Payment MVP | SIXPAY ne crée, ne valide et ne modifie pas la souscription TRESOR PAY |
| CustomerSubscription local | SIXPAY / customer | Ressource locale conforme au contrat Customer Subscription | customer, hors du bounded context Payment | Payment ne possède ni ne commande ce cycle de vie |
| Ordre de paiement demandé | TRESOR PAY | Snapshot immuable du message reçu | Integration pour la réception ; Payment pour l’enregistrement métier | TRESOR PAY définit l’intention ; SIXPAY définit son résultat de traitement |
| Client bancaire | Amplitude | Snapshot de vérification et projection ObservedCustomer | Customer | Toute donnée courante doit être relue depuis Amplitude |
| Compte bancaire | Amplitude | Référence protégée, résultat de contrôle et snapshot | Customer | SIXPAY ne modifie ni ne tient le compte |
| Paiement traité | SIXPAY | Aggregate Root Payment et historique de transitions | Payment | Payment est l’unique autorité sur l’état SIXPAY du paiement |
| Écritures bancaires | Amplitude | Intention d’écriture, références et résultat observé | Accounting | SIXPAY demande et rapproche ; Amplitude exécute et confirme |
| Projection ObservedCustomer | SIXPAY | Projection CQRS | Customer | Projection dérivée, non autoritative pour le KYC ou le compte |
| Confirmation TFJ | Amplitude | Résultat TFJ rapproché et état Payment associé | Accounting pour le rapprochement ; Payment pour l’état métier | SIXPAY ne fabrique jamais la finalité bancaire |
| Audit d’intégration | SIXPAY | Journaux immuables, corrélation et preuves minimisées | Integration pour les échanges ; Payment pour les décisions | L’audit conserve la preuve, jamais les credentials |

Sources : `PAY-SRC-011` à `PAY-SRC-019`, `PAY-SRC-020` à `PAY-SRC-036`,
`PAY-SRC-041` à `PAY-SRC-046`.

## 5. Matrice d’autorité étendue

| Objet ou fait | Autorité | SIXPAY peut écrire | SIXPAY peut lire | Conservation SIXPAY |
| --- | --- | ---: | ---: | --- |
| Identité et session de l’utilisateur TRESOR PAY | TRESOR PAY | Non | Non | Aucune |
| Token et Subscription Key | Infrastructure de sécurité convenue | Non | Validation technique uniquement | Jamais dans les données métier ou logs |
| Référence de paiement TRESOR PAY | TRESOR PAY | Non | Oui | Snapshot immuable |
| Référence de recouvrement | TRESOR PAY | Non | Oui | Snapshot et index de recherche |
| Payload canonique reçu | TRESOR PAY pour l’intention ; SIXPAY pour la copie reçue | SIXPAY enregistre l’empreinte et les champs nécessaires | Oui | Selon politique Payment/audit |
| `Idempotency-Key` entrante | TRESOR PAY fournit ; SIXPAY gouverne son registre | Oui, dans le registre d’idempotence | Oui | 13 mois après dernière tentative |
| `CorrelationId` | SIXPAY accepte ou génère selon le futur contrat | Oui | Oui | Payment, audit et traces |
| Identifiant Payment | SIXPAY | Oui | Oui | Cycle de vie du Payment |
| État Payment | SIXPAY | Oui | Oui | Historique immuable des transitions |
| Identité bancaire courante | Amplitude | Non | Oui | Snapshot daté seulement |
| NIU bancaire observé | Amplitude pour le fait bancaire | Non | Oui | Attribut ObservedCustomer historisé |
| Statut du compte | Amplitude | Non | Oui | Résultat de contrôle daté |
| Blocage ou opposition | Amplitude | Non | Oui | Résultat de contrôle daté |
| Solde disponible | Amplitude | Non | Oui | Preuve minimale de décision, jamais un solde maître |
| Compte CUT | Configuration bancaire approuvée | Configuration contrôlée uniquement | Oui | Référence protégée et versionnée |
| Intention de posting | SIXPAY | Oui | Oui | Accounting et audit |
| Résultat de posting | Amplitude | Non | Oui | Références, statut et preuve retournée |
| Référence bancaire | Amplitude | Non | Oui | Payment/Accounting pour corrélation |
| Date métier bancaire | Amplitude | Non | Oui | Payment/Accounting |
| Résultat TFJ | Amplitude | Non | Oui | Snapshot TFJ rapproché |
| État de livraison d’une notification | SIXPAY | Oui | Oui | NotificationDelivery |
| Quittance | TRESOR PAY | Non | Référence éventuelle seulement | SIXPAY ne génère pas le document |
| ObservedCustomer | SIXPAY | Projection uniquement | Oui | Selon les paiements conservés |
| Timeline Payment | SIXPAY | Projection uniquement | Oui | Read model/audit |
| Audit métier | SIXPAY | Append-only | Oui selon RBAC | 10 ans par défaut |
| Audit technique d’échange | SIXPAY | Append-only et minimisé | Oui selon RBAC | Selon politique d’audit |

## 6. Vue des bounded contexts et systèmes

```text
┌──────────────────────┐
│     TRESOR PAY       │
│ Souscription externe │
│ Ordre de paiement    │
│ Quittance            │
└──────────┬───────────┘
           │ Published Language / REST
           ▼
┌──────────────────────────────────────────────────────────────┐
│                        SIXPAY CONNECT                        │
│                                                              │
│  ┌─────────────┐        ┌───────────────────────────────┐    │
│  │ Integration │───────▶│ Payment                       │    │
│  │ ACL externe │        │ Aggregate + Process Owner     │    │
│  └──────┬──────┘        └──────┬───────────┬────────────┘    │
│         │                      │           │                 │
│         │               contrôles│           │posting/TFJ    │
│         │                      ▼           ▼                 │
│         │              ┌────────────┐ ┌────────────┐         │
│         │              │ Customer   │ │ Accounting │         │
│         │              │ facts/read │ │ write/ref. │         │
│         │              └─────┬──────┘ └─────┬──────┘         │
│         │                    │              │                │
│         │                    └──────┬───────┘                │
│         │                           │ ports externes         │
│         │                           ▼                        │
│         │                    ┌─────────────┐                 │
│         └────────────────────│ Integration │                 │
│                              │ Amplitude   │                 │
│                              └──────┬──────┘                 │
│                                     │                        │
│  Domain Events                      │                        │
│       ├──────────────▶ Notification │                        │
│       ├──────────────▶ Reporting    │                        │
│       └──────────────▶ Projections  │                        │
└─────────────────────────────────────┼────────────────────────┘
                                      │ REST / callback / query
                                      ▼
                            ┌──────────────────────┐
                            │      AMPLITUDE       │
                            │ Client, comptes      │
                            │ Solde, écritures     │
                            │ Résultat TFJ         │
                            └──────────────────────┘
```

`Integration` apparaît aux frontières entrante et sortante, mais reste un seul
bounded context technique. Il ne possède aucune règle de décision Payment.

## 7. Responsabilités du module Payment

### 7.1 Responsabilités possédées

Payment est le domaine pilote et le propriétaire du processus.

Il possède :

- l’Aggregate Root `Payment` ;
- `PaymentId` et le cycle de vie SIXPAY ;
- les invariants de transition ;
- le registre métier d’idempotence de la demande ;
- le snapshot canonique nécessaire au traitement ;
- la décision d’accepter, rejeter ou maintenir en traitement ;
- l’orchestration des contrôles Customer ;
- la décision de demander le posting Accounting ;
- l’interprétation métier des résultats bancaires ;
- les états `ACCOUNTING_OUTCOME_UNKNOWN` et `REVERSAL_REQUIRED` ;
- l’attente de la confirmation TFJ ;
- les événements métier Payment ;
- l’écriture atomique du Payment et de ses événements Outbox ;
- l’historique des décisions et transitions.

### 7.2 Entrées

- commande canonique `ReceivePaymentRequest` issue d’Integration ;
- résultat `BankingCustomerVerification` produit par Customer ;
- résultat `PaymentPostingOutcome` produit par Accounting ;
- résultat `EndOfDayConfirmationOutcome` produit par Accounting ;
- état de livraison de notification lorsque celui-ci influence le suivi
  opérationnel.

### 7.3 Sorties

- commandes de vérification ;
- demandes de posting ;
- demandes de rapprochement ;
- événements Payment ;
- demandes de notification immédiate et définitive ;
- données nécessaires aux projections.

### 7.4 Responsabilités interdites

Payment ne doit pas :

- appeler directement un SDK ou endpoint TRESOR PAY/Amplitude ;
- contenir les DTO externes ;
- modifier un client ou un compte bancaire ;
- gérer un abonnement ;
- exécuter lui-même une écriture bancaire ;
- considérer une notification comme preuve de posting ;
- considérer un posting immédiat comme finalité TFJ ;
- construire des écrans ou rapports ;
- gérer les retries HTTP.

## 8. Responsabilités du module Customer

### 8.1 Responsabilités possédées

Dans le MVP, Customer est un domaine de support.

Il possède :

- le langage canonique de vérification du client et du compte ;
- les règles de cohérence des faits bancaires reçus ;
- les ports applicatifs de vérification ;
- le snapshot `BankingCustomerVerification` ;
- la projection `ObservedCustomer` ;
- les handlers de projection associés ;
- les queries de lecture d’ObservedCustomer ;
- les règles d’identité et de non-fusion interbancaire arrêtées en IA-0R.

### 8.2 Entrées

- demande de vérification émise par Payment ;
- faits normalisés retournés par l’adaptateur Amplitude ;
- événements Payment nécessaires à la projection.

### 8.3 Sorties

- résultat de vérification frais et horodaté ;
- motifs normalisés de non-conformité ;
- projection ObservedCustomer actualisée ;
- vues de lecture masquées.

### 8.4 Responsabilités interdites

Customer ne doit pas :

- créer un Aggregate Root Customer maître ;
- mettre à jour Amplitude ;
- stocker un solde courant comme vérité locale ;
- décider seul de l’acceptation finale du Payment ;
- gérer un abonnement ou un marchand ;
- fusionner automatiquement des clients de banques différentes ;
- exposer des identifiants de compte en clair.

## 9. Responsabilités du module Integration

### 9.1 Responsabilités possédées

Integration constitue l’Anti-Corruption Layer avec les systèmes externes.

Il possède :

- l’adaptateur entrant TRESOR PAY ;
- l’authentification Token + Subscription Key à la frontière ;
- la validation des headers et schémas de transport ;
- le mapping du payload TRESOR PAY vers une commande canonique ;
- les clients REST Amplitude ;
- les adaptateurs des ports Customer et Accounting ;
- le mapping des erreurs externes vers les erreurs canoniques ;
- les timeouts, retries, backoff et circuit breakers techniques ;
- la réception technique de la confirmation TFJ ;
- la signature et l’envoi technique des webhooks TRESOR PAY ;
- l’audit technique minimisé des échanges ;
- la propagation de la corrélation.

### 9.2 Entrées

- requêtes REST TRESOR PAY ;
- réponses et erreurs Amplitude ;
- callback ou résultat TFJ Amplitude ;
- demandes de transport émises par les modules internes.

### 9.3 Sorties

- commandes canoniques vers Payment ;
- faits bancaires normalisés vers Customer/Accounting ;
- résultats de transport ;
- preuves techniques minimisées ;
- appels et notifications externes.

### 9.4 Responsabilités interdites

Integration ne doit pas :

- décider si un paiement est accepté ;
- modifier directement l’état de l’Aggregate Payment ;
- porter des règles de solde, d’opposition ou de reversal ;
- posséder le registre métier d’idempotence ;
- créer une vérité locale Customer/Account ;
- interpréter une réponse TFJ comme transition métier sans passer par
  Accounting et Payment ;
- journaliser tokens, clés ou payloads sensibles complets.

## 10. Responsabilités du module Notification

### 10.1 Responsabilités possédées

Notification possède le processus fiable de livraison.

Il possède :

- `NotificationDelivery` et son état ;
- la consommation idempotente des demandes de notification ;
- la construction du message canonique à partir de l’événement ;
- la sélection du canal autorisé ;
- la planification des tentatives ;
- retry, backoff et bascule DLQ ;
- les identifiants de livraison ;
- la preuve de livraison ou d’échec terminal ;
- les opérations de replay autorisées et auditées.

### 10.2 Interaction avec l’Outbox

- Payment enregistre l’intention de notification dans la même transaction que
  son changement d’état ;
- l’infrastructure Outbox publie cette intention ;
- Notification crée ou actualise la livraison ;
- Integration exécute l’appel HTTP/webhook vers TRESOR PAY ;
- Notification interprète le résultat de transport et planifie la suite ;
- Payment ne revient pas à un état financier antérieur à cause d’un échec de
  notification.

### 10.3 Responsabilités interdites

Notification ne doit pas :

- déterminer le résultat métier à annoncer ;
- produire une confirmation TFJ ;
- modifier une écriture bancaire ;
- générer la quittance ;
- réexécuter un paiement ;
- inclure des données non nécessaires au message ;
- considérer un HTTP `2xx` comme preuve de finalité bancaire.

## 11. Responsabilités du module Accounting

### 11.1 Responsabilités possédées

Accounting porte le langage canonique des écritures et du rapprochement, sans
devenir propriétaire de Payment.

Il possède :

- `PaymentPostingInstruction` ;
- le mapping métier validé vers le schéma de posting bancaire ;
- `PaymentPostingOutcome` ;
- les références et statuts de posting observés ;
- la recherche du résultat d’une écriture inconnue ;
- les cas de rapprochement comptable ;
- la demande et le suivi d’extourne ;
- le modèle canonique de résultat TFJ ;
- le rapprochement d’une confirmation TFJ avec une écriture connue ;
- la détection des confirmations non rapprochées ;
- les contrôles nécessaires au rapport opérationnel TFJO.

### 11.2 Entrées

- demande de posting émise par Payment ;
- réponses de posting Amplitude via Integration ;
- confirmation TFJ normalisée ;
- demande de recherche ou d’extourne.

### 11.3 Sorties

- résultat de posting normalisé ;
- résultat de recherche après outcome inconnu ;
- événement de rapprochement ou d’anomalie ;
- confirmation TFJ rapprochée ;
- demande d’alerte opérationnelle.

### 11.4 Responsabilités interdites

Accounting ne doit pas :

- décider des préconditions Customer ;
- accepter ou rejeter directement le Payment ;
- posséder le cycle de vie Payment ;
- écrire directement dans Amplitude sans adaptateur Integration ;
- produire une écriture locale présentée comme vérité bancaire ;
- déclarer un succès TFJ sans confirmation Amplitude ;
- rejouer aveuglément une écriture financière ;
- masquer une anomalie de posting.

## 12. Responsabilités du module Reporting

### 12.1 Responsabilités possédées

Reporting porte les vues transverses de lecture nécessaires au MVP.

Il possède :

- les projections de recherche multi-critères des paiements ;
- la timeline opérationnelle consolidée ;
- les vues de statut de posting, notification et TFJ ;
- les indicateurs opérationnels indispensables ;
- les exports autorisés par la matrice RBAC ;
- les queries de consultation et d’investigation ;
- la reconstruction de ses projections depuis les événements conservés.

### 12.2 Sources de données

Reporting consomme :

- événements Payment ;
- événements Accounting ;
- états Notification ;
- références ObservedCustomer ;
- événements d’audit autorisés.

Il ne lit pas directement les tables internes des autres domaines comme
contrat d’intégration.

### 12.3 Responsabilités interdites

Reporting ne doit pas :

- modifier Payment ou ses transitions ;
- déclencher un posting ou une extourne ;
- modifier ObservedCustomer ;
- appeler Amplitude ou TRESOR PAY ;
- recalculer une vérité bancaire ;
- exposer des données non masquées sans autorisation ;
- devenir un data warehouse ou un reporting réglementaire complet dans le MVP.

## 13. Modules explicitement sans responsabilité Payment MVP

### Souscription externe TRESOR PAY (hors MVP)

La souscription externe TRESOR PAY peut être référencée pour la traçabilité,
mais elle n’est ni appelée, ni consultée, ni alimentée par le parcours
Payment MVP.

La capacité locale CustomerSubscription est portée par customer et exposée
par documentation/contracts/internal/customer-subscription-management-api-v1.yaml.
Elle est hors du bounded context Payment : Payment ne la possède pas et ne
dépend pas de son cycle de vie.

### Merchant

Aucun module, Aggregate Root ou repository Merchant ne doit être créé pour ce
parcours.

### Partner

Le partenaire TRESOR PAY peut être référencé par configuration technique et
politique d’accès existante. Partner ne possède ni l’ordre ni le Payment et ne
réintroduit pas la gestion d’abonnement.

### Administration

Administration peut gérer les configurations autorisées, rôles et paramètres.
Il ne modifie pas directement les agrégats Payment, Customer ou Accounting.

## 14. Matrice RACI du parcours MVP

Légende :

- `A` : autorité/propriétaire de la décision ;
- `R` : exécute la responsabilité ;
- `C` : consulté ou fournisseur ;
- `I` : informé ;
- `—` : aucune responsabilité.

| Activité | TRESOR PAY | Payment | Customer | Integration | Accounting | Notification | Reporting | Amplitude |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Créer l’ordre demandé | A/R | I | — | I | — | — | — | — |
| Authentifier l’appel système | C | I | — | A/R | — | — | — | — |
| Enregistrer le Payment | C | A/R | — | C | — | — | I | — |
| Appliquer l’idempotence métier | C | A/R | — | C | — | — | I | — |
| Vérifier client et compte | — | A | R | R | — | — | I | A/R |
| Contrôler blocage/opposition | — | A | R | R | — | — | I | A/R |
| Contrôler fonds disponibles | — | A | C | R | R | — | I | A/R |
| Décider de demander le posting | — | A/R | C | — | C | — | I | — |
| Exécuter débit/crédit CUT | — | C | — | R | R | — | I | A/R |
| Interpréter l’outcome métier | I | A/R | — | C | R | I | I | C |
| Livrer le résultat immédiat | I | A | — | R | C | R | I | — |
| Produire le résultat TFJ | — | I | — | R | C | — | I | A/R |
| Rapprocher le résultat TFJ | I | A | — | C | R | I | I | C |
| Livrer le résultat définitif | I | A | — | R | C | R | I | — |
| Générer la quittance | A/R | I | — | — | — | — | — | — |
| Maintenir ObservedCustomer | — | C | A/R | C | — | — | I | C |
| Maintenir la timeline de lecture | — | C | C | C | C | C | A/R | — |
| Conserver l’audit d’intégration | I | A | C | R | C | C | I | I |

Dans cette matrice, `A` côté Payment signifie autorité sur la décision SIXPAY,
jamais autorité sur la vérité bancaire détenue par Amplitude.

## 15. Contrats internes candidats

Les noms suivants structurent les interactions sans figer encore les classes :

| Port ou message | Producteur | Consommateur | Nature |
| --- | --- | --- | --- |
| `ReceivePaymentRequest` | Integration | Payment | Commande entrante |
| `VerifyBankingCustomer` | Payment | Customer | Port applicatif |
| `BankingCustomerVerification` | Customer | Payment | Résultat canonique |
| `ExecutePaymentPosting` | Payment | Accounting | Port applicatif |
| `PaymentPostingOutcome` | Accounting | Payment | Résultat canonique |
| `FindPaymentPostingOutcome` | Payment/Accounting | Integration/Amplitude | Port de réconciliation |
| `EndOfDayConfirmationReceived` | Integration | Accounting | Message entrant normalisé |
| `EndOfDayConfirmationOutcome` | Accounting | Payment | Résultat rapproché |
| `PaymentResultNotificationRequested` | Payment | Notification | Événement/outbox |
| `NotificationDeliveryUpdated` | Notification | Reporting/Operations | Événement |
| `PaymentEvent` | Payment | Customer/Accounting/Notification/Reporting | Événement métier |

Ces noms seront confirmés lors des étapes modèle, événements et Contract Pack.

## 16. Règles de dépendance entre modules

1. Le domaine Payment ne dépend d’aucune classe Infrastructure.
2. Les DTO TRESOR PAY et Amplitude restent dans Integration.
3. Les modules communiquent par ports applicatifs et événements versionnés.
4. Aucun module ne lit directement les tables privées d’un autre module.
5. Une projection peut être reconstruite sans modifier les agrégats sources.
6. Les appels financiers passent par une clé d’idempotence stable.
7. Les erreurs externes sont traduites avant d’entrer dans le domaine.
8. Les retries techniques restent à la frontière Integration/Notification.
9. Les décisions de retry métier restent dans Payment/Accounting.
10. Les événements ne contiennent ni token, ni clé de souscription, ni numéro
    de compte complet.
11. La souscription externe TRESOR PAY ne figure dans aucune dépendance du
parcours Payment MVP ; CustomerSubscription est une capacité customer hors du
bounded context Payment.
12. Reporting et Notification ne peuvent pas commander une transition
    financière.

## 17. Enchaînements autorisés

### 17.1 Demande et contrôles

```text
TRESOR PAY
  → Integration.inbound
  → Payment.application
  → Customer.application
  → Customer.port.out
  → Integration.amplitude
  → Amplitude
```

### 17.2 Posting

```text
Payment.application
  → Accounting.application
  → Accounting.port.out
  → Integration.amplitude
  → Amplitude
  → Integration.amplitude
  → Accounting.application
  → Payment.application
```

### 17.3 Notification

```text
Payment transaction
  → Transactional Outbox
  → Notification.application
  → Notification.port.out
  → Integration.tresorpay
  → TRESOR PAY
```

### 17.4 Confirmation TFJ

```text
Amplitude
  → Integration.amplitude
  → Accounting.application
  → Payment.application
  → Transactional Outbox
  → Notification
  → Integration.tresorpay
  → TRESOR PAY
```

## 18. Interactions interdites

```text
Payment       -X→ Amplitude HTTP direct
Payment       -X→ TRESOR PAY HTTP direct
Payment       -X→ CustomerSubscription lifecycle
Accounting    -X→ tables Payment
Reporting     -X→ commande de posting
Notification  -X→ transition financière
Integration   -X→ décision d’acceptation Payment
Souscription TRESOR PAY -X→ parcours Payment MVP
Merchant      -X→ modèle Payment MVP
```

## 19. Impacts sur les étapes suivantes

### 0P.4 — Parcours

Les parcours devront respecter les enchaînements autorisés et attribuer chaque
action au propriétaire défini ici.

### 0P.5 — Modèle Payment

Le modèle Payment ne devra contenir ni entité Customer maître, ni
CustomerSubscription, ni entité de souscription externe TRESOR PAY, ni DTO
externe, ni logique de livraison.

### 0P.8 — Événements

Les événements devront indiquer clairement leur producteur et leurs
consommateurs sans transférer l’autorité métier.

### 0P.9 — Exigences contractuelles

Les OpenAPI externes seront possédés par Integration, mais leurs règles métier
seront imposées par Payment, Customer et Accounting.

## 20. Critères de sortie de l’étape 0P.3

- [x] Les huit autorités principales sont attribuées.
- [x] La distinction ordre demandé / Payment traité / écriture bancaire est
  formalisée.
- [x] Les responsabilités des six modules demandés sont définies.
- [x] Les responsabilités de TRESOR PAY et Amplitude sont explicites.
- [x] La souscription externe TRESOR PAY est absente du parcours Payment MVP.
- [x] CustomerSubscription est explicitement attribuée à customer et exclue
  du modèle Payment.
- [x] Merchant ne possède aucun modèle SIXPAY dans le MVP.
- [x] ObservedCustomer est une projection possédée par Customer.
- [x] Payment reste l’unique propriétaire du cycle de vie métier.
- [x] Accounting ne devient pas propriétaire de Payment.
- [x] Integration ne contient aucune décision métier.
- [x] Notification ne confond pas livraison et finalité bancaire.
- [x] Reporting reste en lecture seule.
- [x] Les ports et sens d’échange candidats sont identifiés.
- [x] Les dépendances et interactions interdites sont documentées.

## 21. Verdict 0P.3

```text
PAYMENT CONTEXT MAP: ESTABLISHED
SYSTEMS OF RECORD: ASSIGNED
MODULE OWNERSHIP: ASSIGNED
FORBIDDEN DEPENDENCIES: EXPLICIT
RESPONSIBILITY AMBIGUITIES: 0
CODE GENERATION: FORBIDDEN
NEXT STEP: 0P.4 — PAYMENT BUSINESS FLOWS
```
