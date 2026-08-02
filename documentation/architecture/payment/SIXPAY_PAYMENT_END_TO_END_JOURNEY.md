# SIXPAY CONNECT — Parcours de paiement de bout en bout

## 1. Objet du document

Ce document décrit le parcours complet d’un paiement entre :

```text
TresorPay
    ↓
SIXPAY CONNECT
    ↓
Core Banking / Amplitude
    ↓
SIXPAY CONNECT
    ↓
TresorPay
```

Il présente :

- le rôle de chaque système ;
- le parcours nominal d’un paiement ;
- les contrôles réalisés par SIXPAY ;
- le lifecycle métier du domaine Payment ;
- les mécanismes d’idempotence, d’audit et d’outbox ;
- les interactions attendues avec le core banking ;
- la notification asynchrone de TresorPay ;
- les principaux scénarios d’erreur ;
- l’état réel de l’implémentation à la clôture de la Phase 3.

> **Principe fondamental**
>
> TresorPay ne contacte jamais directement le core banking.
>
> TresorPay appelle uniquement les API exposées par SIXPAY. SIXPAY orchestre ensuite toutes les opérations avec le core banking, conserve l’état métier du paiement et notifie TresorPay des évolutions ultérieures.

---

## 2. Vue d’ensemble du parcours

```text
TresorPay
    |
    | POST /v1/payments/initiate
    | Authorization: Bearer <JWT>
    | Idempotency-Key: <clé unique>
    | X-Correlation-ID: <identifiant optionnel>
    v
SIXPAY Payment API
    |
    | validation du contrat
    | contrôle de l’identité partenaire
    | contrôle d’idempotence
    | protection des références bancaires
    | création du Payment
    | audit + outbox
    v
PENDING_CONFIRMATION
    |
    | réponse HTTP initiale à TresorPay
    v
Core Banking / Amplitude
    |
    | confirmation du client
    | vérification du compte
    | contrôle des fonds
    | résolution des comptes bénéficiaires
    | posting bancaire
    v
SIXPAY Payment
    |
    | mise à jour du lifecycle
    | audit
    | outbox
    | réconciliation / TFJ
    v
TresorPay
    |
    | callback asynchrone signé
    v
Statut final du paiement
```

---

## 3. Initiation du paiement par TresorPay

TresorPay appelle l’endpoint suivant :

```http
POST /v1/payments/initiate
Authorization: Bearer <JWT>
Idempotency-Key: <clé unique>
X-Correlation-ID: <UUID optionnel>
Content-Type: application/json
```

Le scope attendu est :

```text
SCOPE_payment.initiate
```

### 3.1 Exemple de requête

```json
{
  "LoginName": "TRESOR_PAY",
  "AppID": "TP_APP_001",
  "endToEndId": "AVI-2025-00045678",
  "montantTotal": 600000,
  "devise": "XAF",
  "ribDebiteur": "10005-00001-12345678901-12",
  "nomDebiteur": "Société ABC SARL",
  "typeCreance": "AVI",
  "NUI": "100200300",
  "dateExecution": "2026-08-03T10:30:00Z",
  "beneficiaires": [
    {
      "rib": "10005-00001-000000TRESDGI-97",
      "montant": 300000
    },
    {
      "rib": "10005-00001-000000TRESDOUANE-11",
      "montant": 200000
    },
    {
      "rib": "10005-00001-000000TRESDOMAINE-22",
      "montant": 100000
    }
  ],
  "callbackURL": "https://tresorpay.cm/v1/callbacks/payment-status"
}
```

### 3.2 Sécurité d’entrée

Les éléments de sécurité sont traités hors du domaine Payment :

```text
Bearer JWT
    → authentification et autorisation Spring Security

APIKey / PIN
    → non propagés dans les objets métier

OTP
    → jamais stocké en clair dans Payment
```

Le `LoginName` transmis dans la requête doit correspondre à l’identité du partenaire authentifié.

---

## 4. Validation de la requête

La requête est validée avant toute création du paiement.

Les contrôles principaux sont :

```text
LoginName non vide
endToEndId présent
montantTotal positif
devise sur trois caractères
RIB débiteur présent
nom du débiteur présent
type de créance présent
NUI présent
date d’exécution présente
callbackURL en HTTPS
au moins un bénéficiaire
maximum de bénéficiaires respecté
chaque montant bénéficiaire strictement positif
```

La règle financière principale est :

```text
sum(beneficiaires.montant) == montantTotal
```

L’`Idempotency-Key` et l’`endToEndId` sont indépendants.

```text
Idempotency-Key
    → identifie une tentative d’appel HTTP idempotente

endToEndId
    → identifie la référence métier externe du paiement
```

Ils peuvent avoir la même valeur, mais aucune égalité n’est imposée.

---

## 5. Orchestration idempotente

Après validation, le Controller appelle :

```java
PaymentInitiationUseCase.initiateDebit(command)
```

L’implémentation applicative est portée par :

```java
PaymentInitiationOrchestrationService
```

Le parcours d’idempotence est le suivant :

```text
InitiateDebitCommand
    ↓
canonicalisation déterministe du contenu métier
    ↓
calcul SHA-256
    ↓
verrou PostgreSQL associé à l’Idempotency-Key
    ↓
réservation ou lecture de l’enregistrement d’idempotence
```

### 5.1 Nouvelle requête

```text
Idempotency-Key inconnue
    ↓
création d’un nouveau Payment
```

### 5.2 Rejeu idempotent

```text
même Idempotency-Key
+
même contenu métier
    ↓
retour de la réponse persistée
```

Aucun second paiement n’est créé.

### 5.3 Conflit d’idempotence

```text
même Idempotency-Key
+
contenu métier différent
    ↓
rejet de la requête
```

### 5.4 Requête déjà en cours

Lorsqu’une exécution précédente est toujours active :

```text
PaymentInitiationInProgressException
```

est retournée afin d’éviter deux traitements concurrents de la même clé.

---

## 6. Préparation locale du Payment

Avant tout appel au core banking, SIXPAY prépare les objets métier grâce à :

```java
PaymentInitiationPreparationAdapter
```

Cet adaptateur ne contacte aucun système bancaire.

Il réalise notamment :

```text
génération du PaymentId
génération de la PublicPaymentReference
construction de PaymentRequestIdentity
construction du PaymentInitiationContext
protection du RIB débiteur
construction des allocations bénéficiaires
construction de NewPaymentIntent
```

### 6.1 Protection du RIB

Le RIB clair n’est pas conservé directement dans le domaine Payment.

SIXPAY construit :

```text
integrationAccountToken
maskedDisplay
bindingFingerprint
```

Exemple d’affichage masqué :

```text
RIB-****-0112
```

Le domaine conserve donc une référence protégée et non le RIB complet.

### 6.2 Contexte d’initiation

Le `PaymentInitiationContext` contient notamment :

```text
partnerLoginName
applicationId
debtorName
claimType
taxpayerIdentifier
requestedExecutionAt
callbackEndpoint
```

Ce contexte permettra ensuite :

- de confronter les données avec le core banking ;
- de retrouver le callback de TresorPay ;
- de construire les notifications de statut.

---

## 7. Création du Payment

L’orchestration transmet les objets préparés à :

```java
PaymentReceptionService.receive(...)
```

Le service vérifie d’abord qu’un paiement n’existe pas déjà pour :

```text
PaymentSource
+
ExternalPaymentReference
```

Dans le cas TresorPay :

```text
TRESOR_PAY
+
endToEndId
```

Ensuite, l’agrégat est créé :

```java
Payment.receive(...)
```

puis le statut de confirmation client est demandé :

```java
payment.requestCustomerConfirmation(receivedAt)
```

Le paiement passe donc par :

```text
RECEIVED
    ↓
PENDING_CONFIRMATION
```

---

## 8. Persistance atomique

La persistance est coordonnée afin d’enregistrer dans une même transaction :

```text
Payment
+
Payment Audit
+
Payment Outbox Events
+
résultat d’idempotence
```

Cette atomicité évite notamment :

```text
Payment enregistré sans événement
événement enregistré sans Payment
audit incomplet
réponse d’idempotence enregistrée sans Payment
```

Le document métier est stocké dans :

```text
payments.state_payload JSONB
```

Les données relationnelles essentielles restent disponibles séparément :

```text
payment_id
public_payment_reference
payment_source
external_payment_reference
status
business_version
persistence_version
received_at
updated_at
finalized_at
```

La version métier et la version de persistence sont distinctes.

---

## 9. Réponse initiale à TresorPay

Lorsque la transaction réussit, SIXPAY retourne immédiatement une réponse de type :

```json
{
  "OK": "200",
  "Description": "Payment order initiated successfully",
  "Result": "Success",
  "paymentReference": "PAY-...",
  "endToEndId": "AVI-2025-00045678",
  "montantTotal": {
    "amount": 600000,
    "currency": "XAF"
  },
  "Date": "2026-08-03T10:30:00Z",
  "Status": "PENDING_CONFIRMATION",
  "NextStep": "Awaiting customer confirmation via OTP/SMS"
}
```

Le statut initial est :

```text
PENDING_CONFIRMATION
```

Cette réponse signifie :

> SIXPAY a accepté, identifié et persisté durablement la demande.  
> Le débit bancaire définitif n’est pas encore confirmé.

Les données suivantes restent optionnelles tant que les contrats core banking ne sont pas validés :

```text
bankOperationId
frais
montantNet
ValidityInMinutes
TransactionNumber
TransactionQRCode
```

SIXPAY ne doit jamais fabriquer ces valeurs.

---

## 10. Confirmation du client

Le paiement reste en :

```text
PENDING_CONFIRMATION
```

jusqu’à réception d’une preuve de confirmation bancaire.

Cette preuve est représentée par :

```java
CustomerConfirmationEvidence
```

Elle contient notamment :

```text
confirmationReference
confirmationFingerprint
confirmedAt
EvidenceMetadata
```

La preuve doit provenir du système bancaire autorisé, par exemple :

```text
ExternalSystem.AMPLITUDE
```

Lorsqu’elle est acceptée :

```java
payment.recordCustomerConfirmation(evidence)
```

le paiement passe à :

```text
AUTHORIZATION_CHECKING
```

et produit notamment :

```text
PaymentCustomerConfirmationRecorded
PaymentAuthorizationCheckingStarted
```

### 10.1 Élément encore en attente

Le contrat autoritaire de confirmation bancaire n’est pas encore finalisé.

Les éléments suivants restent à définir :

```text
URL bancaire
méthode HTTP
payload de confirmation
référence de confirmation
mécanisme OTP
timeouts
codes d’erreur
signature
replay
```

Le domaine Payment est prêt à consommer la preuve, mais l’adaptateur bancaire correspondant reste à implémenter.

---

## 11. Vérifications bancaires

Après confirmation, SIXPAY orchestre seul les échanges avec le core banking.

Le lifecycle cible est :

```text
AUTHORIZATION_CHECKING
    ↓
BANKING_VERIFICATION_PENDING
    ↓
FUNDS_CONTROL_PENDING
    ↓
TREASURY_ACCOUNT_RESOLUTION_PENDING
    ↓
APPROVED_FOR_POSTING
```

### 11.1 Authorization Checking

SIXPAY vérifie notamment :

```text
partenaire autorisé
abonnement actif
application autorisée
type de créance autorisé
date d’exécution valide
cohérence des données de la demande
```

En cas de rejet :

```text
REJECTED
```

### 11.2 Banking Verification

SIXPAY demande au core banking de vérifier :

```text
existence du compte
état du compte
institution
identité du titulaire
nom du débiteur
NUI
éligibilité aux opérations
```

### 11.3 Funds Control

SIXPAY demande ensuite :

```text
solde disponible
provisions
plafonds
restrictions
frais
montant mobilisable
```

Des fonds insuffisants entraînent un rejet métier.

### 11.4 Treasury Account Resolution

SIXPAY résout les comptes bénéficiaires correspondant aux allocations :

```text
AVI
IM7
RNF
autres types autorisés
```

---

## 12. Gateways core banking

Les frontières bancaires prévues sont :

```text
VerificationGateway
FundsGateway
PostingGateway
LookupGateway
ReversalGateway
```

Ces gateways sont invoqués uniquement par SIXPAY.

TresorPay n’a aucune visibilité sur :

```text
les URLs du core banking
les contrats Amplitude
les références techniques bancaires
les mécanismes de retry internes
les erreurs techniques bancaires
```

Les implémentations finales restent dépendantes des contrats d’accès au core banking.

---

## 13. Posting bancaire

Lorsque toutes les vérifications sont positives :

```text
APPROVED_FOR_POSTING
    ↓
POSTING_PENDING
```

SIXPAY demande au core banking d’exécuter :

```text
débit du compte débiteur
+
crédit du ou des comptes bénéficiaires
```

Les règles principales sont :

```text
commande bancaire idempotente
référence bancaire durable
aucun double débit
aucun double crédit
aucune supposition de succès sans preuve
```

### 13.1 Succès explicite

```text
POSTING_PENDING
    ↓
DEBIT_CONFIRMED
    ↓
POSTED_PENDING_TFJ
```

SIXPAY conserve alors :

```text
bankOperationId
référence de posting
preuve de débit
montants
timestamps
```

### 13.2 Échec explicite

Le paiement peut évoluer vers :

```text
FAILED
```

ou :

```text
REVERSAL_REQUIRED
```

si une compensation financière devient nécessaire.

### 13.3 Résultat inconnu

En cas de timeout ou d’erreur réseau après l’envoi :

```text
POSTING_OUTCOME_UNKNOWN
```

SIXPAY ne doit pas conclure immédiatement à un échec.

Il doit effectuer un lookup ou une réconciliation avant tout retry afin d’éviter :

```text
double débit
double crédit
double posting
```

---

## 14. Séparation Posting et Réconciliation

La séparation suivante est fondamentale :

```text
Posting != Reconciliation
```

### 14.1 Posting

Le Posting représente l’action immédiate :

```text
envoyer l’ordre financier
recevoir un résultat
conserver la référence bancaire
```

### 14.2 Réconciliation

La réconciliation représente un traitement ultérieur :

```text
comparer l’état SIXPAY avec Amplitude
résoudre les résultats inconnus
contrôler les écritures comptabilisées
traiter les résultats ou fichiers TFJ
```

Après le posting bancaire :

```text
POSTED_PENDING_TFJ
```

Le paiement n’est pas encore terminal.

Après confirmation TFJ :

```text
POSTED_PENDING_TFJ
    ↓
TREASURY_INTEGRATED
```

`TREASURY_INTEGRATED` est un statut terminal.

---

## 15. Reversal

Lorsqu’un débit a été réalisé mais que le traitement global ne peut pas être finalisé :

```text
REVERSAL_REQUIRED
    ↓
REVERSAL_PENDING
```

SIXPAY demande au core banking d’exécuter l’opération inverse.

### 15.1 Reversal confirmé

```text
REVERSAL_PENDING
    ↓
REVERSED
```

### 15.2 Résultat de reversal inconnu

```text
REVERSAL_PENDING
    ↓
REVERSAL_OUTCOME_UNKNOWN
```

Comme pour le Posting, SIXPAY doit consulter l’état réel du core banking avant tout nouvel essai.

`REVERSED` est terminal.

---

## 16. Callback asynchrone vers TresorPay

TresorPay ne reste pas connecté pendant tout le traitement.

Après la réponse initiale, les changements de statut sont envoyés à :

```text
callbackURL
```

Le callback n’est jamais exécuté dans la transaction HTTP initiale.

### 16.1 Flux Outbox

```text
mutation Payment
    ↓
audit + outbox
    ↓
commit
    ↓
relay asynchrone
    ↓
construction du callback
    ↓
signature
    ↓
POST callbackURL
```

Le relay est porté par :

```java
PaymentCallbackOutboxRelay
```

### 16.2 Exemple de callback

```json
{
  "eventId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "eventType": "PAYMENT_STATUS_CHANGED",
  "occurredAt": "2026-08-03T10:31:00Z",
  "paymentReference": "PAY-1234567890ABCDEFGHJKMNPQRS",
  "endToEndId": "AVI-2025-00045678",
  "bankOperationId": null,
  "previousStatus": "PENDING_CONFIRMATION",
  "status": "AUTHORIZATION_CHECKING",
  "reasonCode": null,
  "description": "Payment status changed from PENDING_CONFIRMATION to AUTHORIZATION_CHECKING",
  "transactionNumber": null
}
```

### 16.3 Sécurité du callback

Le transport prévoit :

```text
HTTPS
X-Correlation-ID
X-SIXPAY-Signature
JWS détachée RS256
```

La clé privée n’est jamais stockée dans le repository.

Le mTLS devra être configuré dans l’environnement de déploiement.

### 16.4 Sémantique de livraison

La livraison est :

```text
at least once
```

TresorPay doit donc dédupliquer les événements grâce à :

```text
eventId
```

Un même `eventId` peut être reçu plusieurs fois.

### 16.5 Retry et Dead Letter

En cas d’échec :

```text
FAILED
    ↓
retry exponentiel
    ↓
nouvelle tentative
    ↓
DEAD après le maximum autorisé
```

---

## 17. API Query Payment

SIXPAY expose aussi les endpoints internes suivants :

```http
GET /internal/api/v1/payments
GET /internal/api/v1/payments/{paymentId}
```

Ils permettent :

```text
recherche de paiements
consultation détaillée
filtre par référence
filtre par statut
filtre par institution
filtre par montant
filtre par dates
```

Ils utilisent des projections de lecture et ne chargent pas l’agrégat complet.

Ils appliquent :

```text
politiques d’accès
scope payment.read
isolation du partenaire
object access
masquage des données sensibles
```

---

## 18. Lifecycle Payment complet

Le lifecycle métier officiel est :

```text
RECEIVED
PENDING_CONFIRMATION
AUTHORIZATION_CHECKING
BANKING_VERIFICATION_PENDING
FUNDS_CONTROL_PENDING
TREASURY_ACCOUNT_RESOLUTION_PENDING
APPROVED_FOR_POSTING
POSTING_PENDING
POSTING_OUTCOME_UNKNOWN
DEBIT_CONFIRMED
POSTED_PENDING_TFJ
REVERSAL_REQUIRED
REVERSAL_PENDING
REVERSAL_OUTCOME_UNKNOWN
REJECTED
FAILED
TREASURY_INTEGRATED
REVERSED
```

Les statuts terminaux sont :

```text
REJECTED
FAILED
TREASURY_INTEGRATED
REVERSED
```

---

## 19. Parcours nominal complet

```text
TresorPay
    |
    | POST /v1/payments/initiate
    v
SIXPAY
    |
    | validation
    | authentification
    | idempotence
    | protection des données
    | création Payment
    v
RECEIVED
    |
    v
PENDING_CONFIRMATION
    |
    | réponse initiale à TresorPay
    v
Confirmation client / Core Banking
    |
    v
AUTHORIZATION_CHECKING
    |
    v
BANKING_VERIFICATION_PENDING
    |
    v
FUNDS_CONTROL_PENDING
    |
    v
TREASURY_ACCOUNT_RESOLUTION_PENDING
    |
    v
APPROVED_FOR_POSTING
    |
    v
POSTING_PENDING
    |
    | posting dans Amplitude
    v
DEBIT_CONFIRMED
    |
    v
POSTED_PENDING_TFJ
    |
    | réconciliation / TFJ
    v
TREASURY_INTEGRATED
    |
    | callback signé
    v
TresorPay
```

---

## 20. Parcours d’erreur

### 20.1 Rejet avant débit

```text
AUTHORIZATION_CHECKING
ou
BANKING_VERIFICATION_PENDING
ou
FUNDS_CONTROL_PENDING
    ↓
REJECTED
```

Exemples :

```text
partenaire non autorisé
compte inexistant
compte bloqué
nom ou NUI incohérent
fonds insuffisants
type de créance interdit
```

### 20.2 Erreur technique avant opération financière

```text
état non récupérable
    ↓
FAILED
```

### 20.3 Résultat bancaire inconnu

```text
POSTING_PENDING
    ↓
POSTING_OUTCOME_UNKNOWN
    ↓
lookup / réconciliation
```

### 20.4 Compensation nécessaire

```text
débit confirmé
+
traitement aval impossible
    ↓
REVERSAL_REQUIRED
    ↓
REVERSAL_PENDING
    ↓
REVERSED
```

### 20.5 Reversal inconnu

```text
REVERSAL_PENDING
    ↓
REVERSAL_OUTCOME_UNKNOWN
    ↓
lookup / réconciliation
```

---

## 21. Responsabilités de TresorPay

TresorPay est responsable de :

```text
authentification auprès de SIXPAY
construction de la demande
gestion de endToEndId
gestion de Idempotency-Key
définition des bénéficiaires
fourniture de callbackURL
traitement idempotent des callbacks
affichage du statut au client
```

TresorPay n’est pas responsable de :

```text
validation directe du compte dans Amplitude
contrôle du solde
posting bancaire
réconciliation
TFJ
reversal
```

---

## 22. Responsabilités de SIXPAY

SIXPAY est responsable de :

```text
sécurité de l’API
isolation du partenaire
validation du contrat
idempotence
génération de la référence publique
protection des références bancaires
lifecycle métier
orchestration core banking
audit
outbox
posting
réconciliation
reversal
API Query
callback TresorPay
observabilité
```

---

## 23. Responsabilités du core banking

Le core banking reste l’autorité pour :

```text
existence du compte
état du compte
identité bancaire du titulaire
solde disponible
autorisation de débit
posting financier
référence bancaire
preuve de débit
reversal
état réel comptabilisé
confirmation TFJ
```

---

## 24. État réel à la clôture de la Phase 3

### 24.1 Éléments implémentés

```text
API InitiateDebit
validation du payload
authentification et scope
idempotence
préparation locale du Payment
protection du RIB
agrégat Payment
lifecycle métier
PENDING_CONFIRMATION
preuve de confirmation structurée
persistence JSONB
optimistic locking
audit
outbox
projection Query
sécurité de lecture
observabilité
tests de concurrence
replay
callback asynchrone
signature du callback
retry et dead-letter
Swagger Payment
```

### 24.2 Éléments dépendant des contrats core banking

```text
confirmation bancaire réelle
protocole OTP
vérification réelle du compte
contrôle réel des fonds
lookup bancaire
résolution réelle des comptes bénéficiaires
posting réel dans Amplitude
lookup après timeout
reversal réel
réconciliation TFJ réelle
frais bancaires définitifs
bankOperationId
TransactionNumber
TransactionQRCode
```

---

## 25. Conclusion

La Phase 3 fournit désormais un backend Payment structuré autour de SIXPAY comme orchestrateur central.

Le système dispose déjà de :

```text
son API TresorPay
son domaine métier
son lifecycle
son idempotence
sa persistence
son audit
son outbox
ses projections Query
ses mécanismes de sécurité
son callback asynchrone
ses tests
```

Les futures intégrations avec Amplitude pourront être branchées derrière les gateways existants sans modifier le contrat externe exposé à TresorPay ni reconstruire le domaine Payment.

> **Résultat architectural**
>
> TresorPay reste découplé du core banking.
>
> SIXPAY conserve l’autorité d’orchestration, de traçabilité et de cohérence du paiement de bout en bout.
