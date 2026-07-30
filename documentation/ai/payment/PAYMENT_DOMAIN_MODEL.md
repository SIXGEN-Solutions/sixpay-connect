# SIXPAY CONNECT — Payment Domain Model

## 1. Identification

| Propriété | Valeur |
| --- | --- |
| Gate | `IA-0P — Payment Preflight` |
| Étape | `0P.5 — Définir le modèle métier Payment` |
| Branche | `feat/payment-contract-pack` |
| Commit GitHub analysé | `e7746b0de32f6a660b2b06250f68f9c1305e14dd` |
| Dépendance locale | `0P.4 — PAYMENT_BUSINESS_FLOWS.md` |
| Domaine pilote | `payment` |
| Statut | `DOMAIN_MODEL_ESTABLISHED` |
| Génération de code | **Interdite** |
| Étape suivante | `0P.6 — Fermer la machine à états Payment` |

## 2. Objectif

Ce document formalise le modèle métier du domaine Payment avant toute
génération de code. Il fixe :

- la frontière de l’Aggregate Root `Payment` ;
- les identifiants et Value Objects ;
- les snapshots externes nécessaires aux décisions Payment ;
- les concepts possédés par Customer, Accounting, Notification et
  Integration ;
- les projections de lecture et données d’audit ;
- les opérations métier candidates de l’agrégat ;
- les dépendances et modèles interdits.

Il complète sans les remplacer :

- `PAYMENT_SOURCE_BASELINE.md` ;
- `PAYMENT_CONTEXT_MAP.md` ;
- `PAYMENT_BUSINESS_FLOWS.md` ;
- `GATE_IA_0P_PAYMENT_PREFLIGHT.md` ;
- `IA_0R_BLOCKING_DECISIONS.yaml`.

La machine à états et les transitions exactes seront fermées à l’étape 0P.6.
Les invariants seront consolidés à l’étape 0P.7.

## 3. Principes de modélisation

### 3.1 Frontière transactionnelle

`Payment` est l’unique Aggregate Root du modèle d’écriture Payment MVP.

Une transaction portant sur Payment :

- charge au plus un agrégat Payment ;
- applique une opération métier nommée ;
- protège ses invariants ;
- persiste l’agrégat et ses événements Outbox de manière atomique ;
- ne réalise aucun appel réseau dans la transaction du domaine ;
- ne modifie aucun agrégat Customer, Accounting ou Notification.

### 3.2 Vérité locale et faits externes

Payment est maître de sa décision et de son état SIXPAY, mais n’est pas maître :

- du client ou du compte bancaire ;
- du solde disponible ;
- de l’écriture réellement exécutée ;
- de la confirmation TFJ ;
- de l’état de livraison HTTP d’une notification.

Il conserve des **snapshots immuables et datés** de ces faits lorsque ceux-ci
ont justifié une décision. Le snapshot prouve ce que Payment a observé ; il ne
remplace jamais la vérité courante du système source.

### 3.3 Taille de l’agrégat

L’agrégat conserve son état décisionnel courant et les références nécessaires
à ses invariants. Les listes non bornées restent hors agrégat :

- tentatives d’intégration ;
- tentatives de notification ;
- historique complet des transitions ;
- confirmations TFJ non rapprochées ;
- investigations et commentaires opérateur ;
- événements d’audit.

Ces données appartiennent aux journaux append-only ou aux projections.

### 3.4 Immutabilité

Les Value Objects :

- sont immuables ;
- valident leurs invariants à la construction ;
- utilisent une égalité par valeur ;
- ne contiennent aucune dépendance Spring, JPA, HTTP ou Kafka ;
- ne permettent pas de représenter un état invalide.

Les dates techniques utilisent `Instant` UTC, les dates bancaires calendaires
utilisent `LocalDate`, et les règles temporelles reçoivent un `Clock`.

Sources : règles DDD SIXPAY ; `PAY-SRC-014`, `PAY-SRC-020`,
`PAY-SRC-032`, `PAY-SRC-037`, `PAY-SRC-047` à `PAY-SRC-049`.

---

# 4. Vue conceptuelle

```text
┌─────────────────────────────────────────────────────────────┐
│ Aggregate Root: Payment                                     │
│                                                             │
│ Identity                                                    │
│   PaymentId                                                 │
│                                                             │
│ External intent                                             │
│   TresorPayRequestId, PaymentReference                      │
│   IdempotencyKey, RequestFingerprint, CorrelationId         │
│                                                             │
│ Requested payment                                           │
│   PayerSnapshot, BankAccountReference                       │
│   FinancialInstitutionCode, Money                           │
│   TreasuryAccountReference                                  │
│                                                             │
│ Decision state                                              │
│   PaymentStatus, PaymentDecision                            │
│   BankingVerificationSnapshot                               │
│   PostingOutcomeSnapshot / PostingReference                 │
│   EndOfDayConfirmationSnapshot                              │
│   ReversalSnapshot                                          │
│   NotificationIntentReference(s)                            │
│                                                             │
│ Technical consistency                                       │
│   aggregateVersion, createdAt, updatedAt                    │
└─────────────────────────────────────────────────────────────┘
           │ domain events / commands / canonical outcomes
           │
           ├── Customer: BankingVerification + ObservedCustomer
           ├── Accounting: posting, TFJ, reconciliation, reversal
           ├── Notification: NotificationDelivery
           ├── Integration: external DTOs and protocols
           └── Reporting: Payment views, timeline and audit
```

## 4.1 Relations d’autorité

```text
TRESOR PAY order
      │ translated command
      ▼
Payment ── requests ──► Customer verification
   │                         │ immutable result
   │◄────────────────────────┘
   │
   ├── requests ──────► Accounting posting / reconciliation
   │                         │ immutable outcome
   │◄────────────────────────┘
   │
   └── emits intent ──► NotificationDelivery
                              │ delivery status for read/operations
                              ▼
                         Reporting views
```

---

# 5. Aggregate Root `Payment`

## 5.1 Responsabilité

`Payment` représente le traitement par SIXPAY d’une intention de paiement
unique reçue de TRESOR PAY.

Il est responsable de :

- garantir l’unicité métier de l’intention traitée ;
- conserver les références et l’empreinte de la demande ;
- décider quand des contrôles bancaires sont requis ;
- accepter ou rejeter le résultat des contrôles ;
- décider si un posting peut être demandé ;
- interpréter l’outcome comptable normalisé ;
- empêcher tout rejeu financier aveugle ;
- distinguer posting immédiat et finalité TFJ ;
- exiger une extourne lorsqu’un effet partiel le nécessite ;
- produire les événements et intentions de notification ;
- protéger son cycle de vie et ses invariants.

Il ne contient aucune logique de transport, de retry HTTP, de mapping externe,
de persistance ou de présentation.

## 5.2 État conceptuel

| Attribut conceptuel | Type | Cardinalité | Rôle |
| --- | --- | ---: | --- |
| `id` | `PaymentId` | 1 | Identité SIXPAY |
| `tresorPayRequestId` | `TresorPayRequestId` | 1 | Identité de l’ordre dans TRESOR PAY |
| `paymentReference` | `PaymentReference` | 1 | Référence métier SIXPAY exposable |
| `idempotencyKey` | `IdempotencyKey` | 1 | Protection de l’intention entrante |
| `requestFingerprint` | `RequestFingerprint` | 1 | Détection du conflit de payload |
| `correlationId` | `CorrelationId` | 1 | Corrélation de bout en bout |
| `financialInstitutionCode` | `FinancialInstitutionCode` | 1 | Institution bancaire ciblée |
| `payerSnapshot` | `PayerSnapshot` | 1 | Données minimales observées dans la demande |
| `debtorAccount` | `BankAccountReference` | 1 | Référence protégée du compte débité |
| `amount` | `Money` | 1 | Montant et devise de l’intention |
| `treasuryAccount` | `TreasuryAccountReference` | 1 | Référence de configuration CUT résolue |
| `status` | `PaymentStatus` | 1 | État métier SIXPAY candidat |
| `decision` | `PaymentDecision` | 0..1 | Résultat et motif métier courant |
| `bankingVerification` | `BankingVerificationSnapshot` | 0..1 | Preuve minimale des contrôles utilisés |
| `postingOutcome` | `PostingOutcomeSnapshot` | 0..1 | Fait comptable normalisé observé |
| `endOfDayConfirmation` | `EndOfDayConfirmationSnapshot` | 0..1 | TFJ favorable rapprochée ou résultat final pertinent |
| `reversal` | `ReversalSnapshot` | 0..1 | État métier observé de l’extourne |
| `notificationIntents` | ensemble borné de `NotificationIntentReference` | 0..n borné | Intentions logiques immédiate, finale ou extourne |
| `createdAt` | `Instant` | 1 | Réception durable |
| `updatedAt` | `Instant` | 1 | Dernière transition |
| `finalizedAt` | `Instant` | 0..1 | Fin métier durable |
| `aggregateVersion` | version optimiste | 1 | Contrôle de concurrence |

`notificationIntents` est borné par les catégories métier autorisées, pas par
le nombre de tentatives de livraison. Les tentatives appartiennent à
Notification.

## 5.3 Création

La factory métier candidate `Payment.receive(...)` :

1. reçoit uniquement une commande canonique validée au niveau transport ;
2. exige les identifiants, le montant, l’institution et les références
   protégées ;
3. génère ou reçoit `PaymentId` et `PaymentReference` selon la politique
   applicative ;
4. calcule ou reçoit l’empreinte canonique vérifiée ;
5. fixe l’état initial candidat `RECEIVED` ;
6. horodate la réception au moyen d’un `Clock` fourni ;
7. produit le fait métier de réception ;
8. ne déclenche aucun appel externe.

Une demande authentifiée mais sémantiquement invalide peut être représentée par
un Payment reçu puis rejeté. Un appel non authentifié reste un événement de
sécurité Integration et ne crée pas d’agrégat Payment.

## 5.4 Opérations métier candidates

Les signatures définitives dépendront des étapes 0P.6 et 0P.7.

| Opération | Entrée canonique | Effet conceptuel |
| --- | --- | --- |
| `startBankingVerification` | instant | Autorise l’orchestration des contrôles |
| `recordBankingVerification` | `BankingVerificationSnapshot` | Conserve le fait observé et décide de la suite |
| `reject` | `RejectionReason`, instant | Termine sans effet financier |
| `approveForPosting` | instant | Autorise une instruction comptable unique |
| `recordPostingOutcome` | `PostingOutcomeSnapshot` | Interprète succès, échec, inconnu ou partiel |
| `markAccountingOutcomeUnknown` | référence disponible, instant | Interdit un second posting et ouvre la résolution |
| `requireReversal` | `ReversalReason`, instant | Interdit le succès et exige une extourne explicite |
| `recordReversalRequested` | `ReversalReference`, instant | Trace l’instruction autorisée |
| `recordReversalOutcome` | `ReversalSnapshot` | Constate l’extourne ou son outcome inconnu |
| `recordMatchedEndOfDayConfirmation` | `EndOfDayConfirmationSnapshot` | Établit la finalité TFJ si le rapprochement est valide |
| `requestImmediateNotification` | `NotificationIntentReference` | Produit l’intention logique correspondante |
| `requestFinalNotification` | `NotificationIntentReference` | Autorisé uniquement après TFJ favorable persistée |
| `failWithoutFinancialEffect` | `FailureReason`, instant | Termine uniquement si l’absence d’effet est prouvée |

Payment n’expose aucun setter générique. Une opération invalide échoue sans
modifier partiellement l’agrégat.

## 5.5 Éléments explicitement absents

L’agrégat ne contient pas :

- `Subscription`, statut d’abonnement ou clé d’abonnement locale ;
- entité Customer ou Account maître ;
- solde courant ;
- token, Subscription Key ou secret ;
- DTO TRESOR PAY ou Amplitude ;
- client HTTP, politique de retry ou circuit breaker ;
- liste de toutes les tentatives de notification ;
- confirmation TFJ non rapprochée ;
- commentaires et pièces d’investigation ;
- historique non borné des transitions ;
- données de quittance ;
- modèle Merchant.

---

# 6. Identifiants et Value Objects

## 6.1 Catalogue principal

| Concept candidat | Classification retenue | Appartenance | Règles principales |
| --- | --- | --- | --- |
| `PaymentId` | Value Object identifiant | Aggregate Payment | UUID/ULID non nul, stable, sans sémantique externe |
| `TresorPayRequestId` | Value Object identifiant externe | Aggregate Payment | Non vide, normalisé, immuable, scoped par TRESOR PAY |
| `PaymentReference` | Value Object référence métier SIXPAY | Aggregate Payment | Unique, stable, exposable et non réutilisable |
| `IdempotencyKey` | Value Object | Aggregate + registre d’unicité | Non vide, longueur bornée, comparée dans un scope défini |
| `CorrelationId` | Value Object | Aggregate Payment | Non vide ; accepté ou généré selon le futur contrat |
| `PayerSnapshot` | Value Object composite | Aggregate Payment | Données minimales de la demande, immuables et non autoritatives |
| `BankAccountReference` | Value Object protégé | Aggregate Payment | Référence tokenisée/chiffrée + valeur masquée ; jamais de numéro clair exposé |
| `Money` | Value Object | Aggregate Payment | `BigDecimal`, devise ISO explicite, montant strictement positif |
| `FinancialInstitutionCode` | Value Object | Aggregate Payment | Code normalisé issu du référentiel autorisé |
| `BankingVerification` | Résultat canonique Customer | Hors agrégat Payment | Contrat interne détaillé ; traduit en snapshot Payment |
| `PostingResult` | Résultat canonique Accounting | Hors agrégat Payment | Outcome détaillé ; traduit en snapshot Payment |
| `TreasuryAccount` | Configuration bancaire/Accounting | Hors agrégat Payment | Secret de configuration contrôlée, jamais fourni librement par TRESOR PAY |
| `NotificationDelivery` | Modèle de processus Notification | Hors agrégat Payment | Tentatives, retry, backoff, DLQ et preuve de livraison |
| `EndOfDayConfirmation` | Résultat canonique Accounting | Hors agrégat Payment | Confirmation Amplitude normalisée et rapprochée avant usage |
| `Reversal` | Processus Accounting | Hors agrégat Payment | Instruction/outcome distincts, clé d’idempotence propre et audit |

Les concepts externes détaillés ne deviennent pas des enfants de Payment.
L’agrégat en conserve des représentations décisionnelles minimales décrites à
la section 7.

## 6.2 `PaymentId`

`PaymentId` identifie exclusivement le paiement traité par SIXPAY.

- il n’est ni la référence TRESOR PAY ni la référence bancaire ;
- il est généré une seule fois ;
- il ne change jamais après création ;
- il est utilisé par `PaymentRepository` ;
- il peut être exposé aux APIs internes selon le RBAC.

## 6.3 `TresorPayRequestId`

`TresorPayRequestId` identifie l’ordre dans le système maître TRESOR PAY.

Son unicité est évaluée dans un scope comprenant au minimum le partenaire et,
si nécessaire, la version du canal. La définition contractuelle exacte sera
fermée à 0P.9.

## 6.4 `PaymentReference`

`PaymentReference` est la référence métier SIXPAY stable utilisée pour :

- les échanges futurs avec TRESOR PAY ;
- la corrélation avec Accounting ;
- le rapprochement TFJ ;
- la recherche opérationnelle.

Elle ne doit pas encoder une donnée personnelle ou un secret.

## 6.5 `IdempotencyKey` et `RequestFingerprint`

`IdempotencyKey` seule ne suffit pas à distinguer un rejeu valide d’un conflit.

Le registre utilise :

```text
IdempotencyScope
    + IdempotencyKey
    + RequestFingerprint
```

`RequestFingerprint` est une empreinte cryptographique d’une représentation
canonique des champs métier pertinents. Elle :

- ne remplace pas le snapshot métier nécessaire ;
- n’inclut pas les credentials ;
- est comparée en temps constant lorsque cela est pertinent ;
- produit un rejeu si elle est identique ;
- produit un conflit si elle diffère pour la même clé et le même scope.

La contrainte d’unicité doit aussi être garantie en persistance afin de résister
aux requêtes concurrentes.

## 6.6 `CorrelationId`

`CorrelationId` sert à relier commandes, événements, appels externes et audit.
Il n’est :

- ni une clé d’idempotence ;
- ni une preuve d’identité ;
- ni une référence financière.

Il est propagé, mais ne décide jamais de l’unicité du Payment.

## 6.7 `PayerSnapshot`

`PayerSnapshot` représente uniquement les informations reçues ou observées au
moment de l’intention, par exemple :

- NIU normalisé ;
- nom ou raison sociale observé ;
- type de payeur si requis ;
- coordonnées minimales nécessaires au rapprochement ;
- origine et instant d’observation.

Il ne constitue ni un KYC approuvé, ni un client bancaire maître. Les champs
doivent être minimisés ; les valeurs inutiles au traitement ne sont pas
conservées.

L’identité bancaire fraîche retournée par Customer est conservée séparément
dans `BankingVerificationSnapshot`.

## 6.8 `BankAccountReference`

`BankAccountReference` représente le compte donneur d’ordre sans exposer sa
valeur complète.

Il peut contenir :

- une référence technique sécurisée ou tokenisée ;
- une empreinte de rapprochement ;
- une forme masquée pour l’affichage ;
- le type de référence ;
- l’institution associée.

Le chiffrement ou tokenisation appartient à l’infrastructure. Le Value Object
du domaine ne contient aucune clé cryptographique.

## 6.9 `Money`

`Money` associe :

- un montant `BigDecimal` normalisé ;
- une devise explicite ;
- une règle d’échelle contrôlée par devise.

Pour une intention de paiement :

- le montant est strictement positif ;
- aucune conversion implicite de devise n’est autorisée ;
- l’égalité compare montant normalisé et devise ;
- `float` et `double` sont interdits.

## 6.10 `FinancialInstitutionCode`

Ce code identifie l’institution bancaire ciblée et participe :

- au scope des contrôles ;
- à la résolution du compte CUT ;
- à l’idempotence bancaire ;
- au rapprochement TFJ ;
- à la séparation des identités ObservedCustomer interbancaires.

Il provient d’un référentiel de configuration approuvé et ne peut pas être une
chaîne libre interprétée par le domaine.

---

# 7. Snapshots décisionnels appartenant à Payment

## 7.1 `BankingVerificationSnapshot`

Customer possède le résultat canonique détaillé `BankingVerification`.
Payment en conserve un snapshot minimal contenant :

| Donnée | Rôle |
| --- | --- |
| `verificationId` | Référence stable du contrôle |
| `verifiedAt` | Fraîcheur du résultat |
| `sourceInstitution` | Autorité ayant fourni les faits |
| `customerExists` | Résultat d’existence |
| `niuMatch` | Résultat de concordance |
| `accountExists` | Résultat d’existence du compte |
| `accountOwnershipMatch` | Résultat d’appartenance |
| `accountOperationalStatus` | Éligibilité normalisée |
| `blockingStatus` | Blocage/opposition normalisé |
| `fundsAvailabilityDecision` | Suffisant, insuffisant ou inconnu |
| `decisionReasons` | Codes canoniques minimisés |
| `evidenceReference` | Référence vers la preuve détaillée auditée |

Le snapshot :

- est immuable une fois utilisé pour décider ;
- ne contient pas de solde maître ;
- ne peut pas être réutilisé au-delà de sa fraîcheur autorisée ;
- n’autorise pas Payment à recalculer les règles Amplitude ;
- peut être remplacé par une nouvelle vérification avant posting seulement
  selon une opération métier auditée.

## 7.2 `PostingOutcomeSnapshot`

Accounting possède `PostingResult` / `PaymentPostingOutcome`. Payment conserve :

- l’identifiant de l’instruction ;
- la clé d’idempotence bancaire ou sa référence sécurisée ;
- le statut canonique `SUCCEEDED`, `FAILED`, `UNKNOWN` ou `PARTIAL` ;
- la référence bancaire commune ou les références corrélables ;
- le résultat du débit client ;
- le résultat du crédit CUT ;
- la date métier bancaire ;
- l’instant d’observation ;
- les codes de résultat normalisés ;
- la référence du dossier de rapprochement si nécessaire.

Il ne contient ni payload Amplitude brut ni détails de transport.

Un snapshot `UNKNOWN` ou `PARTIAL` interdit à l’agrégat d’autoriser un nouveau
posting de la même intention.

## 7.3 `TreasuryAccountReference`

`TreasuryAccount` est une configuration bancaire gérée hors Payment.

Payment conserve seulement `TreasuryAccountReference`, qui identifie :

- la configuration CUT résolue ;
- l’institution ;
- la version de configuration utilisée ;
- éventuellement une valeur masquée.

TRESOR PAY ne peut jamais fournir ou substituer directement ce compte.

## 7.4 `EndOfDayConfirmationSnapshot`

Accounting possède `EndOfDayConfirmation` et son rapprochement. Payment
n’accepte qu’un snapshot :

- authentifié et normalisé par Integration ;
- dédupliqué par Accounting ;
- rapproché de manière univoque ;
- associé à l’institution, la date métier, `PaymentReference` et la référence
  bancaire attendues.

Le snapshot contient au minimum :

- `confirmationId` ;
- la clé d’idempotence TFJ ;
- l’institution ;
- la date métier ;
- la référence bancaire ;
- le statut TFJ normalisé ;
- `matchedAt` ;
- la référence de preuve.

Une confirmation non rapprochable reste en quarantaine Accounting et
n’apparaît pas dans l’état de l’agrégat.

## 7.5 `ReversalSnapshot`

Accounting possède le processus `Reversal`. Payment conserve le fait métier
nécessaire à son cycle de vie :

- motif exigeant l’extourne ;
- référence du posting original ;
- `ReversalReference` ;
- état observé `REQUIRED`, `REQUESTED`, `UNKNOWN`, `FAILED` ou `CONFIRMED` ;
- référence bancaire d’extourne ;
- instant d’autorisation et acteur technique audité par référence ;
- instant et date métier de confirmation ;
- référence vers le dossier d’audit.

La liste des tentatives, l’instruction bancaire détaillée et les appels restent
dans Accounting/Integration.

## 7.6 `NotificationIntentReference`

Payment conserve l’intention logique de notifier, pas la livraison.

Une référence d’intention contient :

- `eventId` ;
- la catégorie `IMMEDIATE_RESULT`, `FINAL_TFJ_RESULT` ou
  `REVERSAL_RESULT` ;
- le résultat métier immuable à communiquer ;
- l’instant de création ;
- la version du message canonique.

`NotificationDelivery`, ses tentatives, son statut HTTP, son backoff et sa DLQ
restent dans Notification. Les vues de lecture peuvent joindre ces informations
sans les incorporer à l’agrégat.

---

# 8. Concepts hors Aggregate Payment

## 8.1 Matrice de propriété

| Concept | Module propriétaire | Représentation dans Payment | Justification |
| --- | --- | --- | --- |
| `BankingVerification` détaillée | Customer | `BankingVerificationSnapshot` | Customer porte le langage de vérification |
| `ObservedCustomer` | Customer/read model | `ObservedCustomerId` éventuel dans les événements/projections, pas requis par l’agrégat | Projection non transactionnelle |
| `PaymentPostingInstruction` | Accounting | Identifiant/référence de décision de posting | Accounting porte le langage comptable |
| `PostingResult` | Accounting | `PostingOutcomeSnapshot` | Amplitude reste maître de l’effet |
| `TreasuryAccount` | Accounting/configuration | `TreasuryAccountReference` | Configuration bancaire contrôlée |
| `EndOfDayConfirmation` | Accounting | Snapshot seulement après rapprochement | TFJ est un fait Amplitude |
| `ReconciliationCase` | Accounting/Operations | Référence de dossier | Historique non borné hors agrégat |
| `Reversal` | Accounting | `ReversalSnapshot` | Processus financier distinct |
| `NotificationDelivery` | Notification | `NotificationIntentReference` | Livraison indépendante de l’état financier |
| DTO et erreurs TRESOR PAY | Integration | Commande/résultat canonique | Anti-Corruption Layer |
| DTO et erreurs Amplitude | Integration | Résultats canoniques Customer/Accounting | Anti-Corruption Layer |
| `PaymentTimeline` | Reporting | Aucune | Projection reconstruisible |
| `PaymentAuditRecord` | Audit/Reporting | Références minimales | Journal append-only |

## 8.2 Modèles Integration

Restent exclusivement dans Integration :

- schémas OpenAPI et DTO TRESOR PAY ;
- headers HTTP et credentials ;
- DTO Amplitude ;
- codes HTTP, payloads RFC 7807 et erreurs protocolaires ;
- sérialisation, signature et vérification de signature ;
- timeouts, retry, backoff et circuit breaker ;
- endpoints et configuration réseau ;
- payloads bruts autorisés par la politique d’audit technique.

Le domaine reçoit uniquement des commandes et résultats canoniques.

## 8.3 Modèles Customer

Restent dans Customer :

- ports de vérification ;
- résultat détaillé `BankingVerification` ;
- règles de cohérence des faits reçus ;
- `ObservedCustomer` et ses projections ;
- vues masquées du client observé ;
- règles de non-fusion interbancaire.

Payment ne crée pas d’entité Customer enfant.

## 8.4 Modèles Accounting

Restent dans Accounting :

- instruction de posting ;
- résultat détaillé de posting ;
- recherches par clé d’idempotence/référence ;
- dossiers de rapprochement ;
- confirmations TFJ reçues et mises en quarantaine ;
- instructions et outcomes d’extourne ;
- références comptables détaillées ;
- règles de rapprochement TFJO.

Payment ne tient aucun grand livre local et n’exécute aucune écriture.

## 8.5 Modèles Notification

Restent dans Notification :

- `NotificationDelivery` ;
- `NotificationDeliveryAttempt` ;
- statut de transport ;
- échéance de prochaine tentative ;
- compteur de tentatives ;
- erreur de livraison minimisée ;
- statut DLQ ;
- replay autorisé et preuve de livraison.

Payment n’attend pas un succès HTTP pour reconnaître un fait financier.

---

# 9. Lecture, projections et audit

## 9.1 Projections

Les modèles suivants ne sont pas des membres de l’agrégat :

| Projection | Propriétaire | Contenu |
| --- | --- | --- |
| `ObservedCustomer` | Customer | Identité observée, comptes masqués, premières/dernières demandes, compteurs et dernière décision |
| `PaymentSearchView` | Reporting | Références, montant, institution, état, motif, dates et indicateurs |
| `PaymentDetailView` | Reporting | Synthèse Payment, contrôles, posting, TFJ, extourne et notifications |
| `PaymentTimeline` | Reporting | Suite horodatée des faits métier et techniques autorisés |
| `NotificationStatusView` | Reporting/Notification | Livraison immédiate, finale et extourne |
| `AccountingReconciliationView` | Reporting/Accounting | Outcome inconnu/partiel, dossiers et alertes |
| `UnmatchedEndOfDayConfirmationView` | Accounting/Reporting | Éléments TFJ en quarantaine |

Ces projections :

- sont alimentées par événements idempotents ;
- peuvent être reconstruites ;
- ne commandent aucune transition financière ;
- ne sont jamais utilisées comme source de vérité pour autoriser un posting.

## 9.2 Audit append-only

L’audit conserve hors agrégat :

- chaque requête authentifiée et sa décision de validation ;
- les appels externes minimisés et corrélés ;
- les résultats de contrôle ;
- les transitions et événements Payment ;
- les instructions et outcomes Accounting ;
- les tentatives de notification ;
- les confirmations TFJ et quarantaines ;
- les actions manuelles, replays et extournes ;
- les consultations sensibles.

Les événements d’audit référencent `PaymentId`, `PaymentReference` et
`CorrelationId`, mais ne contiennent ni secret ni numéro de compte complet.

## 9.3 Données de lecture uniquement

Restent calculés dans les projections :

- nombre total de paiements d’un client observé ;
- taux de succès ou d’échec ;
- durée dans chaque état ;
- âge d’une attente TFJ ;
- nombre de tentatives de notification ;
- indicateurs et SLA observés ;
- libellés d’affichage ;
- filtres, tri, pagination et exports ;
- agrégations par institution, période ou motif.

Ces valeurs ne sont pas persistées dans l’agrégat Payment.

---

# 10. Repository et cohérence

## 10.1 `PaymentRepository`

L’interface de domaine manipule uniquement `Payment`.

Contrat candidat minimal :

```text
save(Payment)
findById(PaymentId)
findByPaymentReference(PaymentReference)
findByTresorPayRequestId(TresorPayRequestId, IdempotencyScope)
findByIdempotencyKey(IdempotencyKey, IdempotencyScope)
```

Les requêtes complexes, listes, statistiques et timelines appartiennent au
modèle de lecture.

## 10.2 Contraintes d’unicité

La persistance doit garantir au minimum :

- unicité de `PaymentId` ;
- unicité de `PaymentReference` ;
- unicité de `TresorPayRequestId` dans son scope ;
- unicité de `IdempotencyKey` dans son scope ;
- cohérence entre clé d’idempotence et empreinte canonique ;
- concurrence optimiste sur la version de l’agrégat ;
- unicité logique de chaque intention de notification ;
- absence de second posting pour la même intention.

Les index et contraintes SQL seront définis dans l’implémentation, sans exposer
JPA au domaine.

## 10.3 Transactional Outbox

Dans une même transaction locale :

```text
mutation Payment
    + append DomainEvent
    + append OutboxMessage
```

La publication, la livraison et le retry surviennent après commit.

Une publication dupliquée est tolérée par des consommateurs idempotents. Une
mutation Payment ne dépend jamais d’un broker ou endpoint disponible.

---

# 11. Invariants structurants du modèle

Ces invariants seront détaillés et numérotés à 0P.7 :

1. un Payment représente une seule intention TRESOR PAY ;
2. ses identifiants, son montant, sa devise, son institution et son compte
   débiteur ne changent pas après réception ;
3. une même clé et une même empreinte retournent le Payment existant ;
4. une même clé avec une empreinte différente constitue un conflit ;
5. aucun posting n’est autorisé sans vérification bancaire favorable et
   fraîche ;
6. un rejet avant écriture ne possède aucun outcome financier ;
7. une intention ne peut produire qu’une instruction de posting logique ;
8. un outcome `UNKNOWN` ou `PARTIAL` interdit tout rejeu aveugle ;
9. le crédit CUT ciblé provient d’une configuration bancaire approuvée ;
10. une livraison de notification ne prouve aucun effet financier ;
11. un posting favorable ne prouve pas la finalité TFJ ;
12. `TREASURY_INTEGRATED` exige une TFJ favorable, rapprochée et persistée ;
13. une TFJ non rapprochée ne peut pas entrer dans l’agrégat ;
14. un échec explicite du crédit CUT exige une extourne avant toute clôture
    compatible ;
15. une extourne utilise une identité et une idempotence distinctes ;
16. aucun secret ou numéro de compte complet ne fait partie de l’état métier ;
17. toute transition produit un fait auditable ;
18. une erreur concurrente ne peut pas créer deux effets financiers.

---

# 12. Classification finale des éléments candidats

| Élément | Dans l’agrégat ? | Nature retenue | Autorité source |
| --- | ---: | --- | --- |
| `Payment` | Oui | Aggregate Root | SIXPAY Payment |
| `PaymentId` | Oui | Value Object identifiant | SIXPAY Payment |
| `TresorPayRequestId` | Oui | Value Object identifiant externe | TRESOR PAY pour la valeur |
| `PaymentReference` | Oui | Value Object référence métier | SIXPAY Payment |
| `IdempotencyKey` | Oui | Value Object + contrainte de registre | TRESOR PAY fournit, SIXPAY gouverne |
| `CorrelationId` | Oui | Value Object | Propagé/généré selon contrat SIXPAY |
| `PayerSnapshot` | Oui | Value Object composite non autoritatif | Intention TRESOR PAY |
| `BankAccountReference` | Oui | Value Object protégé | Amplitude pour le compte |
| `Money` | Oui | Value Object | Intention TRESOR PAY |
| `FinancialInstitutionCode` | Oui | Value Object de référentiel | Configuration approuvée |
| `BankingVerification` | Non | Résultat canonique Customer | Amplitude via Customer |
| `BankingVerificationSnapshot` | Oui | Snapshot Value Object | Payment observe Customer |
| `PostingResult` | Non | Résultat canonique Accounting | Amplitude via Accounting |
| `PostingOutcomeSnapshot` | Oui | Snapshot Value Object | Payment observe Accounting |
| `TreasuryAccount` | Non | Configuration Accounting | Banque/configuration |
| `TreasuryAccountReference` | Oui | Value Object de référence | Payment observe la configuration résolue |
| `NotificationDelivery` | Non | Processus/entité Notification | SIXPAY Notification |
| `NotificationIntentReference` | Oui, borné | Value Object de référence | SIXPAY Payment |
| `EndOfDayConfirmation` | Non | Résultat et rapprochement Accounting | Amplitude |
| `EndOfDayConfirmationSnapshot` | Oui, si rapproché | Snapshot Value Object | Payment observe Accounting |
| `Reversal` | Non | Processus Accounting | Amplitude/Accounting |
| `ReversalSnapshot` | Oui | Snapshot Value Object | Payment observe Accounting |
| `ObservedCustomer` | Non | Projection de lecture | SIXPAY Customer |
| `PaymentTimeline` | Non | Projection de lecture/audit | SIXPAY Reporting |

## 12.1 Décision de modélisation

Le modèle retient **un agrégat Payment compact**, entouré de résultats
canoniques et processus appartenant aux bounded contexts spécialisés.

Il rejette un « agrégat géant » contenant :

- le client bancaire ;
- le compte bancaire ;
- l’écriture comptable complète ;
- toutes les tentatives de notification ;
- les confirmations non rapprochées ;
- les dossiers d’extourne ;
- l’historique d’audit.

Cette séparation protège les invariants Payment sans transférer l’autorité
d’Amplitude ni coupler la transaction Payment aux processus asynchrones.

---

# 13. Impacts sur les étapes suivantes

## 13.1 0P.6 — Machine à états

La machine à états devra :

- s’appuyer sur les opérations métier candidates ;
- distinguer état financier, intention de notification et livraison ;
- définir les transitions produites par chaque snapshot ;
- fermer les états terminaux et récupérables ;
- traiter `UNKNOWN`, `PARTIAL`, attente TFJ et extourne.

## 13.2 0P.7 — Invariants

Les invariants structurants de la section 11 devront être numérotés, associés
aux opérations qui les protègent et traduits en scénarios testables.

## 13.3 0P.8 — Événements

Les événements devront publier des faits minimisés. Ils pourront exposer les
identifiants et snapshots strictement nécessaires, jamais l’agrégat, une
entité JPA, un secret ou un payload bancaire brut.

## 13.4 0P.9 — Contrats

Le Contract Pack devra permettre de construire les résultats canoniques sans
faire entrer les DTO externes dans Payment. Il devra fixer les formats,
scopes, longueurs et règles d’exposition des identifiants et références.

---

# 14. Critères de sortie de l’étape 0P.5

- [x] `Payment` est établi comme Aggregate Root unique du modèle d’écriture.
- [x] La frontière transactionnelle de l’agrégat est explicite.
- [x] Les quinze concepts candidats sont classifiés.
- [x] Les identifiants importants sont des Value Objects dédiés.
- [x] `Money` impose montant décimal et devise explicite.
- [x] Le compte bancaire est représenté par une référence protégée.
- [x] Customer reste propriétaire de `BankingVerification`.
- [x] Accounting reste propriétaire du posting, de la TFJ et de l’extourne.
- [x] Notification reste propriétaire de `NotificationDelivery`.
- [x] Payment conserve uniquement les snapshots nécessaires à ses décisions.
- [x] `ObservedCustomer`, timelines et audits restent hors agrégat.
- [x] Les DTO, protocoles et retries restent dans Integration.
- [x] Le repository d’écriture manipule uniquement Payment.
- [x] Les listes non bornées sont exclues de l’agrégat.
- [x] Les dépendances interdites sont explicites.
- [x] Les impacts sur états, invariants, événements et contrats sont tracés.

# 15. Verdict 0P.5

```text
PAYMENT AGGREGATE ROOT: ESTABLISHED
VALUE OBJECTS: CLASSIFIED
EXTERNAL FACTS: SNAPSHOTTED
BOUNDARY OWNERSHIP: PRESERVED
READ AND AUDIT MODELS: SEPARATED
GIANT AGGREGATE: FORBIDDEN
DOMAIN MODEL AMBIGUITIES: 0
CODE GENERATION: FORBIDDEN
NEXT STEP: 0P.6 — PAYMENT STATE MACHINE
```
