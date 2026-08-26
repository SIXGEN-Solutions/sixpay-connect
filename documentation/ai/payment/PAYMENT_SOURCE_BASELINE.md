# SIXPAY CONNECT — Payment Source Baseline

## 1. Identification

| Propriété | Valeur |
| --- | --- |
| Gate | `IA-1 — Payment Domain Brief` |
| Lot courant | `Lot 1 — Langage ubiquitaire et frontière du domaine` |
| Branche | `feat/payment-domain-generation-brief` |
| Baseline initiale | `IA-0P — Payment Preflight` |
| Domaine pilote | `payment` |
| Statut | `LOT_1_SOURCE_BASELINE_NORMALIZED` |
| Génération de code | **Interdite** |
| Étape suivante | `Lot 2 — Value Objects and Identifiers` |

## 2. Objectif

Cette baseline détermine quelles sources gouvernent le domaine Payment et relie
chaque exigence connue :

- soit à une source externe ou documentaire identifiable ;
- soit à une décision normative SIXPAY ;
- soit à une exigence dérivée explicitement signalée ;
- soit à une décision ouverte qui ne peut pas être résolue silencieusement.

Elle ne constitue ni un contrat OpenAPI ni une autorisation de génération de
code. Elle sert de règle d’arbitrage pour les livrables IA-1.

## 3. Périmètre de l’analyse

L’analyse couvre :

- les décisions IA-0R ;
- les cahiers des charges et spécifications fonctionnelles ;
- la documentation d’API TRESOR PAY ;
- le registre des contrats ;
- les contrats TRESOR PAY et Amplitude ;
- les documents d’architecture métier, applicative, technique et logicielle ;
- les user stories ;
- les artefacts IA Payment ;
- l’état du module `backend/payment`.

Les scénarios SFTP, Sandbox, applications mobiles et souscription externe
TRESOR PAY restent hors MVP. La capacité locale CustomerSubscription est
portée par customer, mais reste hors du modèle et du flux Payment IA-1.

## 4. Modèle d’autorité

### 4.1 Niveaux

| Niveau | Autorité | Usage |
| --- | --- | --- |
| `A1` | Décisions IA-0R et baseline IA-1 approuvées | Gouvernent le périmètre, les systèmes maîtres et les décisions figées |
| `A2` | Cahiers d’interopérabilité et documentation API TRESOR PAY | Portent le besoin externe et le sens des opérations source |
| `A3` | Registre des contrats et contrats classés | Gouvernent les capacités réellement contractualisées |
| `A4` | Spécifications fonctionnelles et Product Blueprint | Fournissent les règles encore compatibles avec A1 à A3 |
| `A5` | Blueprints applicatif/technique, SDS et guide | Gouvernent les contraintes de conception |
| `A6` | User stories | Fournissent scénarios et critères candidats |
| `A7` | Code Payment existant | Décrit l’état technique actuel, jamais une nouvelle règle métier |

### 4.2 Règles d’arbitrage

1. Une décision IA-0R ou `PAY-BASE-*` supplante une ancienne description
   contradictoire.
2. Une exigence TRESOR PAY est conservée sauf si elle concerne un scénario
   différé.
3. La présence d’un OpenAPI ne signifie pas qu’il est actif :
   `CONTRACT_REGISTRY.yaml` gouverne son usage.
4. Le contrat versionné normalise l’interprétation d’une documentation API
   externe.
5. Les architectures définissent le comment structurant, pas de nouveaux
   statuts ou invariants métier.
6. Les user stories ne peuvent ni réactiver un domaine différé, ni figer un SLA
   non approuvé.
7. Le code existant ne peut pas inventer une règle absente des autorités.
8. Toute exigence nouvelle sans source est marquée `SIXPAY_DECISION`,
   `DERIVED_REQUIREMENT` ou `OPEN-*`.
9. Les définitions de `PAYMENT_UBIQUITOUS_LANGUAGE.md` sont obligatoires.
10. Les frontières de `PAYMENT_DOMAIN_BOUNDARIES.md` gouvernent la propriété
    des concepts.

## 5. Catalogue des sources

### 5.1 Sources de niveau A1 — Décisions IA

#### `SRC-A1-01` — Gate IA-0R

Chemin :
`documentation/ai/customer/GATE_IA_0_CUSTOMER_PREFLIGHT.md`

Apports :

- Payment devient le domaine pilote ;
- TRESOR PAY reste maître de la souscription externe TRESOR PAY ;
- CustomerSubscription est une capacité locale portée par customer, hors du
  bounded context Payment ;
- SIXPAY persiste la demande, contrôle, exécute, notifie et suit TFJ ;
- `ObservedCustomer` est un modèle de lecture et d’audit ;
- la génération de code reste interdite avant les Gates prévus.

#### `SRC-A1-02` — Registre des décisions bloquantes

Chemins :

- `documentation/ai/customer/IA_0R_BLOCKING_DECISIONS.yaml`
- `documentation/ai/customer/IA_0R_BLOCKING_DECISIONS.md`

Apports normatifs :

- identité et rapprochement d’`ObservedCustomer` ;
- Token + Subscription Key pour le MVP ;
- interdiction du rejeu financier aveugle ;
- traitement des écritures partielles et reversals ;
- confirmation TFJ asynchrone et rapprochement de secours.

#### `SRC-A1-03` — Baseline IA-1 Payment

Chemin :
`documentation/ai/payment/PAYMENT_IA1_BASELINE.md`

Apports :

- périmètre autorisé ;
- classification des contrats ;
- décisions `PAY-BASE-001` à `PAY-BASE-030` ;
- décisions ouvertes `OPEN-BASE-*` ;
- règles de traceabilité.

#### `SRC-A1-04` — Langage ubiquitaire et frontières

Chemins :

- `documentation/ai/payment/PAYMENT_UBIQUITOUS_LANGUAGE.md`
- `documentation/ai/payment/PAYMENT_DOMAIN_BOUNDARIES.md`
- `documentation/ai/payment/PAYMENT_BANK_POSTING_REFERENCE_DECISION.md`

Apports :

- sens unique des termes majeurs ;
- propriété des concepts ;
- séparation Payment, Subscription, Customer, Accounting, Integration et
  Notification ;
- décision `PAY-DEC-IA1-001`.

### 5.2 Sources de niveau A2 — Besoin externe et documentation API

#### `SRC-A2-01` — Interopérabilité TRESOR PAY/Core Banking

Chemin :
`documentation/requirements/cdc/Cahier des Charges interopérabilité entre TRESOR PAY et le core banking_final_schema_final.docx`

Apports :

- automatisation des ordres initiés dans TRESOR PAY ;
- référence commune de recouvrement ;
- vérification et débit du compte ;
- notification immédiate ;
- confirmation distincte des fonds dans le CUT ;
- réconciliation et quittance ;
- SLA à convenir ultérieurement.

Restrictions :

- REST retenu pour le MVP ;
- SFTP/Sandbox et applications mobiles différés ;
- termes « prélèvement » et « virement » normalisés sans changer l’effet métier.

#### `SRC-A2-02` — Cahier de charges Encaissement des paiements

Chemin :
`documentation/requirements/cdc/SIXPAY_CONNECT_CDC.pdf`

Apports applicables :

- traiter et enregistrer un Payment ;
- vérifier compte, opposition et solde ;
- comptabiliser les transactions exécutées ;
- produire les informations TFJO ;
- tracer les actions.

Éléments supplantés :

- gestion locale de la souscription dans le flux Payment IA-1 ;
- génération locale d’une clé ;
- réservation comme résultat final.

#### `SRC-A2-03` — Spécifications fonctionnelles SIXPAY CONNECT

Chemin :
`documentation/requirements/cdc/SIXPAY_CONNECT_Specifications_Fonctionnelles.pdf`

Apports :

- authentification, idempotence et référence unique ;
- contrôles client, compte, solde et oppositions ;
- cycle de vie, événements, journalisation et rejet motivé ;
- absence de comptabilisation d’un Payment rejeté ;
- corrélation ;
- replay contrôlé.

Éléments supplantés :

- CustomerSubscription local dans le flux Payment IA-1 ;
- Customer local comme référentiel maître ;
- SLA historiques non approuvés.

#### `SRC-A2-04` — Documentation API essentielle TRESOR PAY

Chemin :
`documentation/architecture/tresorpay/API_ESSENTIEL_TRESORPAY.pdf`

Opération applicable :
`API d'Ordre de Virement (InitiateDebit)`

Apports normalisés :

- TRESOR PAY soumet une intention de paiement ;
- `endToEndId` est la référence externe unique de bout en bout ;
- le nom `InitiateDebit` ne prouve pas qu’un débit bancaire existe ;
- admission, vérification bancaire, posting et finalité TFJ sont distincts ;
- les credentials historiques ne sont pas des données de domaine Payment ;
- les comptes bénéficiaires source ne remplacent pas la configuration CUT
  protégée.

Interprétation normative :
`documentation/contracts/tresorpay/tresorpay-payment-request-api-v1.yaml`

### 5.3 Sources de niveau A3 — Registre et contrats

#### `SRC-A3-01` — Registre des contrats

Chemin :
`documentation/contracts/CONTRACT_REGISTRY.yaml`

Usage :

- gouverne lifecycle, approval et generation policy ;
- distingue active MVP, reference only, deferred, intentionally absent et
  missing.

#### `SRC-A3-02` — Contrat Amplitude Customer Verification

Chemin :
`documentation/contracts/amplitude/amplitude-customer-verification-api-v1.yaml`

Capacités :

- recherche client ;
- identité/KYC ;
- comptes et propriété ;
- statut, blocage et opposition.

Limites :

- pas de contrôle de fonds ;
- pas de posting ;
- pas de reversal ;
- pas de TFJ.

#### `SRC-A3-03` — Contrats TRESOR PAY différés

Chemins :

- `documentation/contracts/tresorpay/tresorpay-authorization-request-api-v1.yaml`
- `documentation/contracts/tresorpay/tresorpay-authorization-decision-webhook-v1.yaml`

Usage :

- référence de style uniquement ;
- aucune capacité de souscription externe TRESOR PAY dans le MVP ;
- la capacité locale CustomerSubscription relève de customer, pas de Payment ;
- aucune génération à partir de ces fichiers.

#### `SRC-A3-04` — Contrat TRESOR PAY Payment Request

Chemin :
`documentation/contracts/tresorpay/tresorpay-payment-request-api-v1.yaml`

Apports :

- admission durable asynchrone ;
- `endToEndId` normalisé en `ExternalPaymentReference` ;
- JWT signé localement validé ;
- règles d’idempotence et de conflit ;
- absence de credentials dans le domaine ;
- bénéficiaires résolus contre configuration bancaire protégée.

#### `SRC-A3-05` — Contrat Amplitude Payment Posting

Chemin :
`documentation/contracts/amplitude/amplitude-payment-posting-api-v1.yaml`

Apports :

- contrôle de fonds frais et read-only ;
- posting atomique débit + crédit CUT préféré ;
- outcome incertain après timeout ;
- lookup autoritatif ;
- interdiction du rejeu aveugle ;
- reversal explicite et audité ;
- références de posting et résultats par jambe possibles.

#### `SRC-A3-06` — Contrats notification et TFJ

Chemins :

- `documentation/contracts/tresorpay/tresorpay-payment-status-webhook-v1.yaml`
- `documentation/contracts/amplitude/amplitude-end-of-day-confirmation-api-v1.yaml`
- `documentation/contracts/tresorpay/tresorpay-treasury-integration-webhook-v1.yaml`

Apports :

- notification immédiate distincte du résultat final ;
- TFJ comme confirmation autoritative distincte ;
- finalité Treasury uniquement après rapprochement réussi.

### 5.4 Sources de niveaux A4 et A5 — Architecture SIXPAY

#### `SRC-A4-01` — Product Blueprint

Sources :

- `documentation/architecture/SIXPAY_CONNECT_Product_Blueprint.docx`
- `documentation/architecture/Product_SIXPAY_CONNECT.docx`

Règles retenues :

- Payment orchestre son cycle ;
- Customer fournit les faits bancaires ;
- Accounting porte posting, rapprochement et TFJ ;
- Integration isole les systèmes externes ;
- Notification, audit et reporting restent des capacités de support.

#### `SRC-A5-01` — Application Architecture Blueprint

Chemin :
`documentation/architecture/SIXPAY_CONNECT_Application_Architecture_Blueprint.docx`

Usage :

- frontières applicatives ;
- composants et interactions ;
- canonical flow ;
- modèles d’état candidats.

#### `SRC-A5-02` — Technical Architecture

Chemin :
`documentation/architecture/SIXPAY_CONNECT_Technical_Architecture.docx`

Contraintes :

- modular monolith ;
- DDD et CQRS ;
- REST/OpenAPI ;
- `Idempotency-Key` ;
- Transactional Outbox ;
- consommateurs idempotents ;
- reprise, replay contrôlé, observabilité et corrélation.

#### `SRC-A5-03` — Software Design Specification

Chemin :
`documentation/architecture/SIXPAY_CONNECT-SDS.docx`

Usage :

- Aggregate Roots ;
- Value Objects ;
- Domain Services ;
- commandes, queries, handlers ;
- repositories, événements et exceptions.

#### `SRC-A5-04` — Reference Implementation Repository

Sources :

- `documentation/architecture/SIXPAY_CONNECT_SDS.docx`
- `documentation/architecture/SIXPAY_CONNECT_Guide_Implementation.docx`

Usage :

- organisation Maven/packages ;
- persistance et Flyway ;
- API, sécurité, événements et observabilité ;
- structure frontend et tests.

### 5.5 Sources de niveaux A6 et A7

#### `SRC-A6-01` — User stories

Chemin :
`documentation/requirements/user-stories/SIXPAY_CONNECT_USER_STORIES.docx`

Stories principales :

- `US-07` à `US-13` : réception, contrôles, référence, idempotence, résultat,
  timeline et résilience ;
- `US-14` à `US-19` : posting, comptabilisation, TFJO et idempotence comptable.

Restrictions :

- délais historiques non contractuels ;
- Celery interprété comme worker asynchrone fiable, pas choix technologique ;
- vérification locale de souscription dans Payment supprimée ; la capacité
  CustomerSubscription reste hors du périmètre Payment ;
- Customer local remplacé par vérifications fraîches et `ObservedCustomer`.

#### `SRC-A7-01` — Module Payment existant

Chemin :
`backend/payment/`

Usage :

- état technique courant uniquement ;
- aucun statut, champ ou invariant ne devient normatif par sa seule présence.

## 6. Normalisation IA-1

1. La branche autoritaire est `feat/payment-domain-generation-brief`.
2. Toute référence historique à `feat/customer-foundation-contract` ou
   `feat/payment-contract-pack` est une métadonnée antérieure.
3. Les identifiants `SRC-*` et `PAY-SRC-*` existants restent stables.
4. La classification contractuelle vient du Contract Registry.
5. Le langage ubiquitaire du Lot 1 est obligatoire.
6. Les frontières de propriété du Lot 1 sont obligatoires.
7. `InitiateDebit` signifie soumission d’ordre, pas débit confirmé.
8. `endToEndId` signifie `ExternalPaymentReference`.
9. Les payloads externes bruts restent hors aggregate.
10. Toute règle sans source ou décision explicite est invalide.

## 7. Traçabilité requise

Chaque futur élément Payment doit citer au moins un identifiant parmi :

```text
PAY-BASE-*
PAY-BOUND-*
PAY-POSTREF-*
PAY-DEC-*
PAY-CONTRACT-*
PAY-AI-*
PAY-SRC-*
SRC-*
OPEN-*
```

Cette obligation s’applique aux :

- propriétés ;
- Value Objects ;
- états ;
- transitions ;
- invariants ;
- failures ;
- commandes ;
- événements ;
- tests.

## 8. Conclusion

La baseline source est désormais normalisée pour IA-1 Lot 1.

```text
STATUS: LOT_1_SOURCE_BASELINE_NORMALIZED
CODE GENERATION: FORBIDDEN
NEXT: LOT 2 — VALUE OBJECTS AND IDENTIFIERS
```
