# SIXPAY CONNECT — Gate IA-0P Payment Preflight

## 1. Identification du Gate

| Propriété | Valeur |
| --- | --- |
| Gate | `IA-0P — Payment Preflight` |
| Branche | `feat/payment-contract-pack` |
| Commit de référence | `e7746b0de32f6a660b2b06250f68f9c1305e14dd` |
| Domaine pilote | `payment` |
| Statut du Gate | `IN_PROGRESS` |
| Dernière étape terminée | `0P.5 — Payment Domain Model` |
| Génération de code | **Interdite** |
| Gate suivant | `IA-0.5P — Payment Contract Pack` |

## 2. Objectif

Le Gate IA-0P ferme les ambiguïtés fonctionnelles et architecturales du
domaine Payment avant la production des contrats d’interface.

Il doit établir :

- le périmètre exact du MVP ;
- les responsabilités des systèmes et domaines ;
- les parcours nominaux et alternatifs ;
- le modèle métier et la machine à états ;
- les invariants et événements ;
- les exigences des futurs contrats ;
- les règles de sécurité, de résilience, d’audit et de test.

Ce document ne constitue pas encore une autorisation d’implémenter Payment.

## 3. Sources normatives

La baseline des sources est définie dans :

`documentation/ai/payment/PAYMENT_SOURCE_BASELINE.md`

Elle est applicable dans son intégralité. Toute exigence ajoutée au Gate doit :

- citer un identifiant `PAY-SRC-*` ;
- ou citer une source `SRC-*` ;
- ou être déclarée comme nouvelle décision SIXPAY.

En cas de contradiction, les décisions IA-0R priment conformément à la
hiérarchie définie dans la baseline.

---

# 4. Scope / Out of Scope

## 4.1 Définition du MVP Payment

Le MVP Payment couvre le traitement d’un ordre de paiement bancaire initié
dans TRESOR PAY et transmis à SIXPAY CONNECT pour exécution par LA RÉGIONALE
BANK au moyen du Core Banking Amplitude.

Le parcours commence lorsque SIXPAY reçoit la demande authentifiée et se
termine lorsque :

- le paiement a atteint un état final métier ;
- les notifications exigibles ont été durablement prises en charge ;
- la confirmation TFJ a été rapprochée ou l’absence de confirmation a été
  placée sous suivi opérationnel ;
- la demande et toutes ses traces sont consultables dans SIXPAY.

Le MVP ne commence pas lors de la création de l’abonnement TRESOR PAY et ne
comprend pas la gestion de cet abonnement.

## 4.2 Chaîne fonctionnelle couverte

```text
TRESOR PAY
    │
    │ Ordre de paiement REST
    ▼
SIXPAY — Integration
    │ Authentification, validation, idempotence, corrélation
    ▼
SIXPAY — Payment
    │ Persistance immédiate de la demande
    │ Création / actualisation d’ObservedCustomer
    │ Orchestration des contrôles bancaires
    ▼
Amplitude
    │ Vérification client et compte
    │ Contrôle statut, blocage, opposition et fonds disponibles
    │ Débit client et crédit comptable du CUT
    ▼
SIXPAY — Payment / Notification
    │ Résultat immédiat durablement notifié à TRESOR PAY
    │ Attente et rapprochement de la confirmation TFJ
    ▼
SIXPAY — Payment / Accounting / Notification
    │ Confirmation définitive d’intégration au Trésor
    │ Notification définitive à TRESOR PAY
    ▼
SIXPAY — Read Models / Audit
      Consultation du paiement, du client observé et de la timeline
```

Le crédit comptable du CUT constaté lors du posting bancaire ne doit pas être
confondu avec la confirmation définitive d’intégration des fonds après TFJ.

## 4.3 Capacités incluses dans le MVP

### `MVP-PAY-01` — Réception de l’ordre de paiement

SIXPAY reçoit un ordre provenant de TRESOR PAY par API REST.

Le point d’entrée doit prendre en charge :

- Token d’autorisation et Subscription Key ;
- référence TRESOR PAY ;
- `Idempotency-Key` ;
- `X-Correlation-ID` ;
- validation syntaxique et sémantique du message ;
- identification de l’institution financière ;
- montant, devise et références bancaires nécessaires ;
- accusé de réception et erreurs structurées.

Sources : `PAY-SRC-001` à `PAY-SRC-009`.

### `MVP-PAY-02` — Enregistrement immédiat de la demande

La demande est enregistrée avant tout appel vers Amplitude.

L’enregistrement doit conserver :

- l’identité technique du Payment ;
- la référence externe ;
- l’empreinte canonique du payload ;
- les identifiants d’idempotence et de corrélation ;
- l’horodatage de réception ;
- le statut initial ;
- les données métier minimales nécessaires ;
- une trace de l’authentification réussie sans conserver les credentials.

Une demande invalide ou rejetée après authentification doit rester traçable
selon la politique d’audit, sans persister de secret.

Sources : `PAY-SRC-007` à `PAY-SRC-010`, `PAY-SRC-041`, `PAY-SRC-046`.

### `MVP-PAY-03` — Création ou actualisation d’ObservedCustomer

SIXPAY crée une observation dès la première demande de paiement, que celle-ci
aboutisse ou échoue.

`ObservedCustomer` :

- est une projection de lecture et d’audit ;
- ne devient jamais le référentiel bancaire du client ;
- ne représente pas un abonnement ;
- conserve les informations observées sous forme minimisée et masquée ;
- est actualisé à partir des résultats frais de vérification Amplitude ;
- ne fusionne pas automatiquement les identités de banques différentes.

Sources : `PAY-SRC-013`, `PAY-SRC-014`, `PAY-SRC-016`, `PAY-SRC-019`.

### `MVP-PAY-04` — Vérification du client et du compte

SIXPAY interroge Amplitude afin de vérifier :

- l’existence du client bancaire ;
- le NIU comme attribut de rapprochement ;
- l’identité bancaire nécessaire au traitement ;
- l’existence du compte ;
- l’appartenance du RIB/IBAN au client ;
- les champs KYC nécessaires au paiement ;
- la cohérence entre la demande TRESOR PAY et les faits bancaires.

Le contrat
`amplitude-customer-verification-api-v1.yaml` est une référence réutilisable
mais reste soumis à approbation et ne couvre pas l’exécution financière.

Sources : `PAY-SRC-014` à `PAY-SRC-016`, `PAY-SRC-027`.

### `MVP-PAY-05` — Contrôle du statut, des blocages et oppositions

Avant toute écriture, SIXPAY obtient une décision bancaire fraîche confirmant
que :

- le compte est actif ;
- le compte permet l’opération demandée ;
- aucune opposition applicable n’empêche le paiement ;
- aucun blocage applicable n’empêche le paiement.

Tout résultat défavorable provoque un rejet motivé sans écriture financière.

Sources : `PAY-SRC-017`, `PAY-SRC-023`.

### `MVP-PAY-06` — Contrôle de la disponibilité des fonds

Amplitude vérifie la disponibilité des fonds avant l’exécution.

Le contrôle doit tenir compte, selon les capacités confirmées par la banque :

- du solde disponible et non du seul solde comptable ;
- du montant et de la devise ;
- des restrictions ou limites applicables au compte ;
- de la fraîcheur du résultat ;
- de l’impossibilité de réutiliser une vérification obsolète.

Les seuils, catégories et limites métier restent paramétrables.

Sources : `PAY-SRC-018`, `PAY-SRC-027` ; décision SIXPAY pour le caractère
paramétrable.

### `MVP-PAY-07` — Débit du compte client

Après validation de tous les contrôles, SIXPAY demande à Amplitude d’exécuter
le débit.

L’appel d’écriture doit :

- être protégé contre le double posting ;
- transporter une clé d’idempotence bancaire ;
- produire ou restituer une référence bancaire stable ;
- permettre la recherche du résultat après timeout ou réponse inconnue ;
- ne jamais être rejoué aveuglément.

Sources : `PAY-SRC-020` à `PAY-SRC-026`.

### `MVP-PAY-08` — Crédit du CUT

L’effet financier cible comprend le crédit du Compte Unique du Trésor.

La capacité cible est une opération bancaire atomique associant :

- le débit du compte client ;
- le crédit comptable du CUT ;
- une référence de posting commune ou des références corrélables.

Si Amplitude ne peut pas garantir l’atomicité, le Contract Pack devra appliquer
la politique `ACCOUNTING_OUTCOME_UNKNOWN` / `REVERSAL_REQUIRED` arrêtée dans
`IA_0R_BLOCKING_DECISIONS.yaml`.

Le compte CUT et son paramétrage appartiennent à la configuration bancaire ;
ils ne sont jamais fournis librement par TRESOR PAY.

Sources : `PAY-SRC-020` à `PAY-SRC-027` ; décision SIXPAY sur la provenance du
compte CUT.

### `MVP-PAY-09` — Notification du résultat immédiat

SIXPAY transmet à TRESOR PAY le résultat disponible après les contrôles et la
tentative d’écriture.

La notification distingue au minimum :

- paiement rejeté ;
- paiement comptabilisé côté banque et en attente de confirmation TFJ ;
- traitement encore en cours après résultat bancaire inconnu ;
- échec technique sans effet financier confirmé ;
- extourne requise ou en cours, si applicable.

La livraison est fiable, idempotente, corrélée et gérée par outbox avec retry,
backoff et DLQ.

Cette notification ne constitue pas la confirmation définitive de réception
des fonds par le Trésor.

Sources : `PAY-SRC-028` à `PAY-SRC-030`, `PAY-SRC-037` à `PAY-SRC-040`.

### `MVP-PAY-10` — Attente et suivi de la confirmation TFJ

Après le résultat immédiat favorable, Payment reste dans un état non final de
confirmation Trésor.

Le MVP couvre :

- la réception asynchrone du résultat TFJ provenant d’Amplitude ;
- une interrogation planifiée de rapprochement en fallback ;
- la corrélation par institution, date métier, référence Payment et référence
  bancaire ;
- l’idempotence de la confirmation ;
- la quarantaine d’une confirmation non rapprochée ;
- l’alerte en cas d’absence de confirmation au cut-off ;
- le maintien de `PENDING_END_OF_DAY_CONFIRMATION` tant que la finalité n’est
  pas établie.

Sources : `PAY-SRC-030` à `PAY-SRC-033`.

### `MVP-PAY-11` — Notification du résultat définitif

Une notification définitive est adressée à TRESOR PAY uniquement après
persistance durable d’une confirmation TFJ rapprochée.

Elle doit permettre à TRESOR PAY :

- de reconnaître le paiement ;
- de connaître la date métier et la référence bancaire ;
- de déclencher son propre processus de réconciliation ;
- de générer ou rendre disponible la quittance ;
- de traiter un rejeu sans produire un second effet.

SIXPAY ne génère pas la quittance.

Sources : `PAY-SRC-034` à `PAY-SRC-036`.

### `MVP-PAY-12` — Consultation et audit dans SIXPAY

Le MVP fournit aux utilisateurs internes autorisés :

- une liste des clients observés ayant présenté au moins un paiement ;
- une liste des paiements réussis, rejetés, échoués ou en attente ;
- une recherche par référence, NIU, institution, période, statut et motif ;
- le détail d’un Payment ;
- une timeline horodatée des états, contrôles, écritures et notifications ;
- l’état de la confirmation TFJ ;
- les identifiants de corrélation ;
- une piste d’audit immuable ;
- des données sensibles masquées selon le rôle.

Cette interface sert au suivi opérationnel et à l’audit. Elle ne permet pas de
modifier un client Amplitude ni un abonnement TRESOR PAY.

Sources : `PAY-SRC-041` à `PAY-SRC-046`.

## 4.4 Capacités transverses obligatoires

Les capacités suivantes font partie du MVP même si elles ne constituent pas
des étapes visibles du parcours :

- idempotence entrante et bancaire ;
- corrélation de bout en bout ;
- Transactional Outbox ;
- événements et consommateurs idempotents ;
- retry contrôlé avec backoff et jitter ;
- circuit breaker ;
- DLQ et replay opérationnel autorisé ;
- chiffrement et masquage ;
- RBAC et audit des consultations ;
- métriques, traces et alertes ;
- conservation conforme à la baseline IA-0R ;
- erreurs externes structurées selon RFC 7807 ;
- paramétrage des timeouts, retries, seuils et catégories.

Sources : `PAY-SRC-037` à `PAY-SRC-055`.

## 4.5 Données minimales couvertes

Le MVP doit pouvoir représenter au minimum :

- identifiant SIXPAY du Payment ;
- référence TRESOR PAY ;
- référence de recouvrement ;
- clé d’idempotence et identifiant de corrélation ;
- institution financière ;
- NIU et identité observée ;
- compte donneur d’ordre sous forme protégée ;
- compte CUT sous forme de référence de configuration ;
- montant et devise ;
- résultats des contrôles ;
- décision et motif de rejet ;
- référence(s) de posting ;
- état de notification immédiate ;
- date métier et confirmation TFJ ;
- état de notification définitive ;
- dates de création, modification et finalisation ;
- historique des transitions.

Les credentials, tokens complets et clés de souscription ne font jamais partie
des données métier persistées.

## 4.6 Frontière de début

Le MVP commence à la réception de la requête Payment par SIXPAY.

SIXPAY considère que :

- l’utilisateur est authentifié dans TRESOR PAY ;
- le parcours d’abonnement requis par TRESOR PAY a déjà été réalisé ;
- TRESOR PAY est autorisé à transmettre une demande ;
- les informations reçues restent soumises aux contrôles bancaires.

SIXPAY ne reprend pas les responsabilités d’authentification de l’utilisateur
final dans TRESOR PAY.

## 4.7 Frontière de fin

Un Payment atteint une fin métier lorsque l’un des résultats suivants est
durablement établi :

- rejet avant écriture ;
- échec technique confirmé sans effet financier ;
- intégration au Trésor confirmée après TFJ ;
- extourne confirmée ;
- résolution opérationnelle d’un résultat comptable initialement inconnu.

La simple réception, l’acceptation technique, le débit du client ou le posting
initial ne suffisent pas à déclarer le parcours complet terminé.

---

# 5. Out of Scope

## `OOS-PAY-01` — Gestion locale des abonnements

Sont exclus :

- création d’un abonnement dans SIXPAY ;
- persistance d’un Aggregate Root `Subscription` pour ce parcours ;
- recherche d’un abonnement local avant paiement ;
- génération d’une clé locale d’abonnement ;
- activation, suspension, réactivation ou expiration locale ;
- synchronisation autoritative des statuts d’abonnement.

TRESOR PAY reste le système maître.

## `OOS-PAY-02` — Validation d’abonnement depuis SIXPAY

L’interface permettant au gestionnaire bancaire de rechercher une demande
TRESOR PAY et d’en retourner la décision est une évolution ultérieure.

Les contrats d’autorisation présents dans le dépôt restent
`DEFERRED_FUTURE / EXCLUDED`.

## `OOS-PAY-03` — KYC numérique avec document et selfie

Sont exclus :

- téléversement de pièce d’identité ;
- capture de selfie ;
- reconnaissance faciale ;
- preuve de vie ;
- OCR documentaire ;
- décision KYC automatique fondée sur ces médias ;
- conservation de données biométriques.

Le MVP utilise les faits KYC bancaires nécessaires obtenus auprès d’Amplitude.

## `OOS-PAY-04` — Compensation interbancaire

SIXPAY :

- ne pilote pas SYSTAC ou SYGMA ;
- ne calcule pas les positions de compensation ;
- ne remplace pas le moteur de compensation bancaire ;
- ne détermine pas la finalité légale du règlement ;
- ne gère pas les comptes des autres banques.

Le MVP se limite à recevoir ou rechercher le résultat TFJ produit par la banque,
à le rapprocher du Payment et à notifier TRESOR PAY.

## `OOS-PAY-05` — Gestion des marchands TRESOR PAY

SIXPAY ne crée ni ne maintient :

- marchand ;
- catalogue marchand ;
- relation client-marchand ;
- moyen d’encaissement marchand ;
- règlement marchand ;
- commission ou tarification marchand.

Les institutions financières sélectionnées par le client sont représentées
comme institutions et sources de comptes, pas comme Aggregate Roots
`Merchant`.

## `OOS-PAY-06` — Scénarios d’intégration différés

Sont exclus du MVP :

- fichiers CSV/XML via SFTP ;
- portail ou Sandbox de traitement manuel ;
- initiation depuis une application mobile bancaire ;
- paiement par carte ;
- monnaie mobile ;
- terminaux de paiement électroniques.

Le seul canal du MVP est l’API REST TRESOR PAY → SIXPAY.

## `OOS-PAY-07` — Services bancaires généraux

Ne font pas partie du parcours Payment MVP :

- consultation générale du solde par TRESOR PAY ;
- exposition des dernières opérations du compte ;
- tenue d’un référentiel client maître dans SIXPAY ;
- modification du client ou du compte dans Amplitude ;
- synchronisation bidirectionnelle générale avec Amplitude.

Les consultations nécessaires à la décision Payment restent incluses comme
appels internes contrôlés.

## `OOS-PAY-08` — Quittance et notification directe non contractualisée

TRESOR PAY reste responsable de générer et rendre disponible la quittance.

La notification directe du client par SMS ou e-mail n’est pas considérée comme
une responsabilité obligatoire du domaine Payment tant que le futur Contract
Pack n’a pas attribué formellement cette responsabilité. SIXPAY conserve
cependant les événements permettant au module Notification d’être étendu sans
modifier le domaine.

## `OOS-PAY-09` — Reporting et règlement avancés

Sont différés :

- reporting réglementaire complet ;
- data warehouse ou analytique avancée ;
- rapprochement multi-banques ;
- gestion des commissions ;
- règlement avec des marchands ou partenaires privés ;
- tableaux de bord de direction hors indicateurs opérationnels indispensables.

La timeline, la recherche, l’audit et les alertes nécessaires à l’exploitation
du MVP restent inclus.

## 5.1 Règle anti-extension

Une capacité exclue ne peut être réintroduite :

- par une ancienne user story ;
- par un diagramme historique ;
- par la présence d’un module Maven ;
- par la présence d’un contrat différé ;
- par une génération IA ;
- par une interprétation implicite du terme « Payment ».

Sa réintroduction nécessite une décision de périmètre versionnée et un nouveau
passage de Gate.

---

# 6. Répartition fonctionnelle du périmètre

La Context Map normative est définie dans :

`documentation/ai/payment/PAYMENT_CONTEXT_MAP.md`

| Domaine ou système | Responsabilité dans le MVP |
| --- | --- |
| TRESOR PAY | Initier l’ordre, fournir les références, recevoir les résultats, générer la quittance |
| Integration | Authentifier TRESOR PAY, adapter les contrats et isoler Amplitude |
| Payment | Porter le cycle de vie, les invariants, l’idempotence et l’orchestration |
| Customer | Fournir les ports de vérification et alimenter ObservedCustomer |
| ObservedCustomer | Fournir la lecture clients/paiements et la trace opérationnelle |
| Accounting | Porter les références d’écriture, le rapprochement et le suivi TFJ sans devenir propriétaire de Payment |
| Notification | Livrer les résultats avec outbox, retry, backoff et DLQ |
| Reporting | Fournir seulement les vues opérationnelles indispensables au MVP |
| Amplitude | Rester maître du client, du compte, du solde, des écritures et du résultat TFJ |
| Subscription | Aucune responsabilité dans le parcours Payment MVP |
| Merchant | Aucun domaine ou modèle dans SIXPAY pour le MVP |

Cette répartition est détaillée et validée par l’étape `0P.3`. En cas de
divergence, `PAYMENT_CONTEXT_MAP.md` constitue la référence.

# 7. Impacts sur le Contract Pack

Le périmètre impose au minimum les futurs contrats suivants :

1. demande Payment TRESOR PAY → SIXPAY ;
2. contrôles client, compte et fonds SIXPAY → Amplitude ;
3. débit client/crédit CUT SIXPAY → Amplitude ;
4. recherche du résultat d’une écriture inconnue ;
5. notification du résultat immédiat SIXPAY → TRESOR PAY ;
6. confirmation TFJ Amplitude → SIXPAY ;
7. consultation de rapprochement TFJ SIXPAY → Amplitude ;
8. notification définitive SIXPAY → TRESOR PAY ;
9. APIs internes de consultation Payment/ObservedCustomer et d’audit.

Cette liste exprime des besoins de contrats. Leur découpage OpenAPI définitif
sera décidé à `0P.9` puis produit à `IA-0.5P`.

# 8. Critères de sortie de l’étape 0P.2

- [x] Le début et la fin du parcours MVP sont définis.
- [x] Les douze capacités fonctionnelles demandées sont incluses.
- [x] Les capacités transverses indispensables sont incluses.
- [x] Les cinq exclusions obligatoires sont formalisées.
- [x] Les exclusions complémentaires empêchent les extensions implicites.
- [x] TRESOR PAY reste maître de l’abonnement et de la quittance.
- [x] Amplitude reste maître des comptes et écritures bancaires.
- [x] `ObservedCustomer` reste un modèle de lecture.
- [x] Résultat immédiat et confirmation TFJ sont distingués.
- [x] Chaque capacité incluse cite une source ou une décision SIXPAY.
- [x] Les impacts sur le futur Contract Pack sont identifiés.

## Verdict 0P.2

```text
MVP PAYMENT SCOPE: CLOSED
OUT OF SCOPE: EXPLICIT
SCOPE AMBIGUITIES: 0
CODE GENERATION: FORBIDDEN
NEXT STEP: 0P.3 — PAYMENT CONTEXT MAP
```

---

# 9. Résultat de l’étape 0P.3 — Payment Context Map

L’étape 0P.3 fixe les responsabilités sans transférer les vérités externes :

- TRESOR PAY reste maître de l’abonnement, de l’ordre demandé et de la
  quittance ;
- Amplitude reste maître du client bancaire, du compte, du solde, des
  écritures et du résultat TFJ ;
- Payment devient l’unique propriétaire du Payment traité et de ses
  transitions ;
- Customer porte les vérifications canoniques et ObservedCustomer ;
- Integration porte les protocoles et l’Anti-Corruption Layer ;
- Accounting porte les instructions, outcomes et rapprochements comptables ;
- Notification porte la livraison fiable ;
- Reporting porte les vues transverses de lecture.

Les dépendances autorisées, ports candidats, interactions interdites et la
matrice RACI sont définis dans `PAYMENT_CONTEXT_MAP.md`.

## Critères de sortie 0P.3

- [x] Systèmes maîtres attribués.
- [x] Propriétaires de processus attribués.
- [x] Six modules internes détaillés.
- [x] Ports et sens d’échange identifiés.
- [x] Responsabilités interdites explicites.
- [x] Subscription et Merchant absents du parcours MVP.
- [x] Payment reste propriétaire du cycle de vie.
- [x] Amplitude reste propriétaire de la vérité bancaire.

## Verdict 0P.3

```text
PAYMENT CONTEXT MAP: ESTABLISHED
RESPONSIBILITY AMBIGUITIES: 0
CODE GENERATION: FORBIDDEN
NEXT STEP: 0P.4 — PAYMENT BUSINESS FLOWS
```

---

# 10. Résultat de l’étape 0P.4 — Payment Business Flows

L’étape 0P.4 formalise dans
`documentation/ai/payment/PAYMENT_BUSINESS_FLOWS.md` :

- le parcours nominal complet, depuis la réception TRESOR PAY jusqu’à la
  notification définitive après TFJ ;
- les quinze parcours alternatifs demandés ;
- la persistance immédiate et l’alimentation d’ObservedCustomer ;
- la différence entre rejet métier, échec technique, traitement en cours,
  posting en attente TFJ et finalité Trésor ;
- le traitement sans rejeu aveugle des outcomes comptables inconnus ;
- le rapprochement et l’extourne explicite des effets partiels ;
- l’indépendance entre résultat financier et livraison des notifications ;
- la quarantaine des confirmations TFJ non rapprochables ;
- les exigences de reprise, d’idempotence et d’audit.

Les états utilisés dans les parcours restent candidats jusqu’à la fermeture de
la machine à états à l’étape 0P.6.

## Critères de sortie 0P.4

- [x] Parcours nominal complet.
- [x] Quinze parcours alternatifs documentés.
- [x] Responsabilités conformes à `PAYMENT_CONTEXT_MAP.md`.
- [x] Aucun rejet avant écriture ne produit d’effet bancaire.
- [x] Aucun doublon ne produit un second posting.
- [x] Aucun outcome inconnu ne déclenche de rejeu aveugle.
- [x] Résultat immédiat et finalité TFJ restent distincts.
- [x] Confirmation non rapprochable mise en quarantaine.
- [x] Extourne explicite, idempotente et auditée.
- [x] Audit et reprise définis pour chaque famille de parcours.

## Verdict 0P.4

```text
PAYMENT BUSINESS FLOWS: ESTABLISHED
NOMINAL FLOW: COMPLETE
ALTERNATIVE FLOWS: 15/15
FLOW AMBIGUITIES: 0
CODE GENERATION: FORBIDDEN
NEXT STEP: 0P.5 — PAYMENT BUSINESS MODEL
```

---

# 11. Résultat de l’étape 0P.5 — Payment Domain Model

L’étape 0P.5 formalise dans
`documentation/ai/payment/PAYMENT_DOMAIN_MODEL.md` :

- `Payment` comme Aggregate Root unique du modèle d’écriture Payment ;
- les identifiants, références et montants comme Value Objects ;
- un état d’agrégat compact, protégé par des opérations métier nommées ;
- les snapshots minimaux nécessaires aux décisions Payment ;
- les frontières avec Customer, Accounting, Notification et Integration ;
- les projections et journaux d’audit hors agrégat ;
- les contraintes d’unicité, de concurrence et d’Outbox ;
- les concepts explicitement interdits dans le modèle Payment.

Le modèle conserve la décision SIXPAY sans transférer l’autorité externe :

- Customer possède `BankingVerification` et `ObservedCustomer` ;
- Accounting possède le posting, les confirmations TFJ, les rapprochements et
  les extournes ;
- Notification possède `NotificationDelivery` et ses tentatives ;
- Integration possède les DTO, protocoles, credentials et politiques de
  transport ;
- Payment conserve seulement les snapshots immuables et références nécessaires
  à ses invariants.

## Critères de sortie 0P.5

- [x] Aggregate Root et frontière transactionnelle définis.
- [x] Quinze concepts candidats classifiés.
- [x] Value Objects et snapshots Payment identifiés.
- [x] Autorités Customer, Accounting, Notification et Integration préservées.
- [x] Projections de lecture et audit séparés du modèle d’écriture.
- [x] Repository et contraintes d’unicité définis.
- [x] Agrégat géant explicitement interdit.
- [x] Impacts sur machine à états, invariants, événements et contrats tracés.

## Verdict 0P.5

```text
PAYMENT DOMAIN MODEL: ESTABLISHED
AGGREGATE BOUNDARY: CLOSED
CONCEPT OWNERSHIP AMBIGUITIES: 0
CODE GENERATION: FORBIDDEN
NEXT STEP: 0P.6 — PAYMENT STATE MACHINE
```
