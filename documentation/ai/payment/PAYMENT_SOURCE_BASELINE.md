# SIXPAY CONNECT — Payment Source Baseline

## 1. Identification

| Propriété | Valeur |
| --- | --- |
| Gate | `IA-0P — Payment Preflight` |
| Étape | `0P.1 — Consolider les sources d’autorité` |
| Branche | `feat/customer-foundation-contract` |
| Commit analysé | `2d42e8c817068a94d0df46c7c29620c649e8b9dd` |
| Domaine pilote | `payment` |
| Statut | `BASELINE_ESTABLISHED` |
| Génération de code | **Interdite** |
| Étape suivante | `0P.2 — Définir le périmètre exact du MVP Payment` |

## 2. Objectif

Cette baseline détermine quelles sources gouvernent le Payment Preflight et
relie chaque exigence Payment connue :

- soit à une source externe ou documentaire identifiable ;
- soit à une décision normative prise par SIXPAY ;
- soit à une exigence dérivée, explicitement signalée comme telle.

Elle ne constitue ni un contrat OpenAPI, ni une spécification prête à générer du
code. Elle sert de règle d’arbitrage pour tous les livrables IA-0P suivants.

## 3. Périmètre de l’analyse

L’analyse couvre :

- les trois cahiers des charges et spécifications fonctionnelles ;
- les décisions IA-0R ;
- le registre des contrats ;
- les documents d’architecture métier, applicative, technique et logicielle ;
- le backlog de user stories ;
- le module Maven `backend/payment` ;
- les contrats TRESOR PAY et Amplitude présents dans le dépôt.

Les scénarios SFTP, Sandbox et applications mobiles ont été lus mais restent
hors MVP conformément aux décisions de cadrage. Ils ne peuvent pas introduire
de contraintes d’implémentation dans le premier Contract Pack Payment.

## 4. Modèle d’autorité

### 4.1 Niveaux

| Niveau | Autorité | Usage |
| --- | --- | --- |
| `A1` | Décisions IA-0R approuvées pour le cadrage | Gouvernent le périmètre MVP et arbitrent les contradictions historiques |
| `A2` | Cahier d’interopérabilité TRESOR PAY/Core Banking | Porte le besoin externe et le parcours bancaire cible |
| `A3` | Registre des contrats et contrats classés | Gouvernent l’applicabilité et les capacités réellement contractualisées |
| `A4` | Spécifications fonctionnelles et Product Blueprint | Fournissent les règles Payment encore compatibles avec IA-0R |
| `A5` | Blueprints applicatif et technique, SDS et guide | Gouvernent la structure, les patterns et les contraintes de conception |
| `A6` | User stories | Fournissent des scénarios et critères candidats, sous réserve des niveaux supérieurs |
| `A7` | Code du module Payment | Décrit uniquement l’état d’implémentation actuel, jamais le besoin métier |

### 4.2 Règles d’arbitrage

1. Une décision IA-0R explicite supplante toute ancienne description du
   périmètre Customer ou Subscription.
2. Une exigence externe TRESOR PAY est conservée, sauf si elle concerne un
   scénario formellement différé.
3. La présence d’un OpenAPI dans le dépôt ne signifie pas qu’il est actif :
   `CONTRACT_REGISTRY.yaml` gouverne son usage.
4. Les spécifications fonctionnelles restent applicables seulement lorsqu’elles
   ne contredisent ni IA-0R, ni le cahier d’interopérabilité.
5. Les documents d’architecture définissent le « comment structurant », pas le
   système maître ni le périmètre métier.
6. Les user stories ne peuvent ni réactiver un domaine différé, ni figer un SLA
   non approuvé.
7. Le code existant ne peut pas être utilisé pour inventer une règle absente
   des sources d’autorité.
8. Toute nouvelle exigence du Preflight sans source est étiquetée
   `SIXPAY_DECISION` ou `DERIVED_REQUIREMENT`.

## 5. Catalogue des sources

### 5.1 Sources de niveau A1 — Décisions IA-0R

#### `SRC-A1-01` — Gate IA-0R

Chemin :
`documentation/ai/customer/GATE_IA_0_CUSTOMER_PREFLIGHT.md`

Apports :

- Payment devient le domaine pilote ;
- TRESOR PAY reste maître des abonnements pour le MVP ;
- SIXPAY persiste la demande, effectue les contrôles, exécute le paiement,
  notifie le résultat immédiat puis suit la confirmation TFJ ;
- `ObservedCustomer` est un modèle de lecture et d’audit ;
- la génération de code reste interdite avant les Gates prévus.

#### `SRC-A1-02` — Registre des décisions bloquantes

Chemins :

- `documentation/ai/customer/IA_0R_BLOCKING_DECISIONS.yaml`
- `documentation/ai/customer/IA_0R_BLOCKING_DECISIONS.md`

Apports normatifs :

- identité et rapprochement d’`ObservedCustomer` ;
- matrice d’accès et politique de rétention ;
- Token + Subscription Key pour le MVP ;
- defaults non contractuels de timeout/retry/backoff ;
- interdiction du rejeu aveugle après résultat comptable inconnu ;
- traitement des écritures partielles et extournes ;
- confirmation TFJ asynchrone et rapprochement de secours.

#### `SRC-A1-03` — Contexte IA consolidé

Chemins :

- `documentation/ai/customer/AI_CONTEXT_MANIFEST.yaml`
- `documentation/ai/customer/CUSTOMER_DOMAIN_GENERATION_BRIEF.md`

Usage :

- fournir les contraintes lisibles par l’IA ;
- confirmer `codeGenerationAllowed: false` ;
- limiter Customer à un support de vérification et à `ObservedCustomer`.

### 5.2 Sources de niveau A2 — Cahiers des charges

#### `SRC-A2-01` — Interopérabilité TRESOR PAY/Core Banking

Chemin :
`documentation/requirements/cdc/Cahier des Charges interopérabilité entre TRESOR PAY et le core banking_final_schema_final.docx`

État documentaire : `DRAFT 1.0`.

Sections Payment déterminantes :

- `III. PRÉREQUIS GÉNÉRAUX` ;
- `IV.1 Scénario 1 : Banques avec API Moderne` ;
- `IV.1.2 Flux de communication`.

Exigences apportées :

- automatisation des ordres initiés dans TRESOR PAY ;
- sécurité et traçabilité des échanges ;
- référence de recouvrement TRESOR PAY commune aux plateformes ;
- API bancaire sécurisée ;
- référence, montant, token d’autorisation, compte donneur d’ordre et compte
  bénéficiaire dans l’ordre ;
- vérification et débit du compte client ;
- notification du débit ;
- confirmation distincte de la réception effective des fonds dans le CUT ;
- déclenchement de la réconciliation et mise à disposition de la quittance ;
- accord ultérieur sur les temps de réponse et SLA.

Restrictions de baseline :

- le scénario REST est retenu pour le MVP ;
- SFTP/Sandbox et applications mobiles sont différés ;
- les termes « prélèvement » et « virement » devront être normalisés au
  Contract Pack sans changer l’effet métier attendu.

#### `SRC-A2-02` — Cahier de charges Encaissement des paiements

Chemin : `documentation/requirements/cdc/SIXPAY_CONNECT_CDC.pdf`

Sections utiles :

- `3. Périmètre fonctionnel` ;
- `Gestion des transactions` ;
- `Traitement de la comptabilisation` ;
- `10.2 Processus de traitement des transactions de paiement`.

Exigences encore applicables :

- traiter et enregistrer une transaction de paiement ;
- vérifier compte, opposition et solde ;
- rechercher et consulter les transactions ;
- comptabiliser les transactions exécutées ;
- produire les informations de contrôle TFJO ;
- tracer les actions et réduire l’intervention humaine.

Éléments supplantés :

- la gestion locale de l’abonnement ;
- la génération locale d’une clé d’abonnement ;
- la simple réservation comme résultat final du paiement.

#### `SRC-A2-03` — Spécifications fonctionnelles SIXPAY CONNECT

Chemin :
`documentation/requirements/cdc/SIXPAY_CONNECT_Specifications_Fonctionnelles.pdf`

Sections utiles :

- `BP-03 — Payment Processing` ;
- `BP-05 — Accounting & Settlement` ;
- `CBF-03 — Payment Processing` ;
- `CBF-04 — Accounting & Settlement` ;
- `Catalogue BR-CUS`, `BR-PAY`, `BR-PLT` ;
- `Exigences transverses`.

Exigences conservées :

- authentification et validation de la requête ;
- idempotence et référence unique ;
- vérification du client, du compte, du solde et des oppositions ;
- cycle de vie, événements, journalisation et réponse motivée ;
- absence de comptabilisation d’une transaction rejetée ;
- traçabilité par corrélation ;
- notification après établissement du résultat métier ;
- replay contrôlé et détection des pertes.

Éléments supplantés ou à requalifier :

- appel au Subscription Domain pour contrôler un abonnement local ;
- réservation de fonds comme seul effet bancaire ;
- gestion locale de Customer comme référentiel ;
- valeurs SLA historiques non approuvées pour l’intégration actuelle.

### 5.3 Sources de niveau A3 — Registre et contrats

#### `SRC-A3-01` — Registre des contrats

Chemin : `documentation/contracts/CONTRACT_REGISTRY.yaml`

Décisions :

- Amplitude Customer Verification est `REFERENCE_MVP`,
  `PENDING_APPROVAL`, `REFERENCE_ONLY` ;
- les deux contrats TRESOR PAY d’autorisation sont `DEFERRED_FUTURE`,
  `DRAFT`, `EXCLUDED` ;
- quatre contrats Payment restent à produire.

#### `SRC-A3-02` — Contrat Amplitude de vérification

Chemin :
`documentation/contracts/amplitude/amplitude-customer-verification-api-v1.yaml`

Capacités réutilisables :

- recherche du client ;
- identité bancaire et KYC ;
- recherche des comptes ;
- appartenance du compte ;
- statut, blocage et opposition ;
- corrélation, erreurs et résilience configurables.

Limites explicites :

- pas de contrôle de disponibilité des fonds ;
- pas de posting ;
- pas de débit ;
- pas de crédit CUT ;
- pas de confirmation TFJ.

#### `SRC-A3-03` — Contrats TRESOR PAY différés

Chemins :

- `documentation/contracts/tresorpay/tresorpay-authorization-request-api-v1.yaml`
- `documentation/contracts/tresorpay/tresorpay-authorization-decision-webhook-v1.yaml`

Usage autorisé :

- référence de style pour sécurité, corrélation, idempotence, signature,
  erreurs et rejeu ;
- aucun usage fonctionnel dans le parcours Payment MVP ;
- aucune génération d’API Subscription à partir de ces fichiers.

### 5.4 Sources de niveaux A4 et A5 — Architecture SIXPAY

#### `SRC-A4-01` — Product Blueprint

Sources :

- `documentation/architecture/SIXPAY_CONNECT_Product_Blueprint.docx` ;
- `documentation/architecture/Product_SIXPAY_CONNECT.docx`.

Règles retenues :

- Payment orchestre le cycle de vie ;
- Customer fournit les faits bancaires ;
- Accounting porte comptabilisation, rapprochement et fin de journée ;
- Integration isole les systèmes externes ;
- Notification, audit et reporting restent des capacités de support.

Restriction :

Les passages imposant Subscription dans le flux Payment sont supplantés par
IA-0R.

#### `SRC-A5-01` — Application Architecture Blueprint

Chemin :
`documentation/architecture/SIXPAY_CONNECT_Application_Architecture_Blueprint.docx`

Usage :

- frontières applicatives ;
- services, composants et interactions ;
- canonical flow ;
- modèles d’état et préoccupations transverses.

#### `SRC-A5-02` — Technical Architecture

Chemin :
`documentation/architecture/SIXPAY_CONNECT_Technical_Architecture.docx`

Contraintes retenues :

- modular monolith initial ;
- modules métier indépendants ;
- PostgreSQL ;
- DDD et CQRS ;
- API REST versionnées et documentées en OpenAPI 3.x ;
- `Idempotency-Key` pour les opérations financières ;
- Transactional Outbox ;
- consommateurs idempotents ;
- Internal Event Dispatcher avant une éventuelle migration Kafka ;
- reprise, replay contrôlé, observabilité et corrélation.

#### `SRC-A5-03` — Software Design Specification

Chemin :
`documentation/architecture/SIXPAY_CONNECT-SDS.docx`

Usage :

- Aggregate Roots, Value Objects, Domain Services ;
- commandes, queries et handlers ;
- repositories ;
- événements, exceptions et state diagrams ;
- conventions logicielles à appliquer après le Contract Pack.

#### `SRC-A5-04` — Reference Implementation Repository

Sources :

- `documentation/architecture/SIXPAY_CONNECT_SDS.docx` ;
- `documentation/architecture/SIXPAY_CONNECT_Guide_Implementation.docx`.

Usage :

- organisation Maven et packages ;
- persistance et Flyway ;
- API, sécurité, événements et observabilité ;
- structure frontend et tests.

Observation :

Les deux fichiers couvrent un contenu de repository très proche. Ils ne
constituent pas deux autorités concurrentes ; le Preflight les traite comme une
même famille de conventions techniques.

### 5.5 Sources de niveaux A6 et A7

#### `SRC-A6-01` — User stories

Chemin :
`documentation/requirements/user-stories/SIXPAY_CONNECT_USER_STORIES.docx`

Stories Payment principales :

- `US-07` : réception sécurisée d’une demande ;
- `US-08` : contrôles préalables ;
- `US-09` : référence unique ;
- `US-10` : idempotence ;
- `US-11` : résultat ou rejet motivé ;
- `US-12` : timeline et investigation ;
- `US-13` : résilience/outbox ;
- `US-14` à `US-19` : écritures, posting, TFJO et idempotence comptable.

Restrictions :

- mTLS/HMAC n’est pas obligatoire dans le MVP actuel ;
- les délais de 2 s, 3 s ou 30 s sont des objectifs historiques, pas des SLA
  contractuels ;
- Celery est incompatible avec la stack Java retenue et doit être interprété
  comme « worker asynchrone fiable », pas comme choix technologique ;
- la vérification locale de l’abonnement est supprimée ;
- la synchronisation bidirectionnelle Customer/Amplitude est remplacée par des
  vérifications fraîches et une projection `ObservedCustomer`.

#### `SRC-A7-01` — Module Payment existant

Chemin : `backend/payment`

État constaté :

- `pom.xml` Maven présent ;
- dépendance au module `common` ;
- aucun code métier ;
- aucun Aggregate Root ;
- aucun service applicatif ;
- aucun repository ;
- aucune migration ;
- aucun contrat Payment ;
- uniquement un placeholder de tests.

Conclusion :

Le module confirme l’emplacement d’implémentation, mais ne fournit actuellement
aucune règle Payment à préserver.

## 6. Matrice de traçabilité des exigences Payment

### 6.1 Entrée, sécurité et identité de requête

| ID | Exigence normalisée | Origine | Source principale | Statut |
| --- | --- | --- | --- | --- |
| `PAY-SRC-001` | TRESOR PAY initie le paiement et SIXPAY expose le point d’entrée bancaire | Externe | `SRC-A2-01`, scénario API moderne | Confirmée |
| `PAY-SRC-002` | Le canal MVP est une API REST sécurisée | Décision SIXPAY | IA-0R + cadrage utilisateur ; `SRC-A2-01` autorise REST/SOAP | Confirmée |
| `PAY-SRC-003` | La référence TRESOR PAY accompagne le paiement de bout en bout | Externe | `SRC-A2-01`, prérequis général 6 | Confirmée |
| `PAY-SRC-004` | La requête contient au minimum référence, montant, token, compte débité et bénéficiaire/CUT | Externe | `SRC-A2-01`, flux API étape 3 | Confirmée |
| `PAY-SRC-005` | Token Bearer et Subscription Key sont obligatoires pour le MVP | Décision SIXPAY | `SRC-A1-02`, `IA0R-D05` | Confirmée |
| `PAY-SRC-006` | OAuth2 et mTLS sont différés sans bloquer le MVP | Décision SIXPAY | `SRC-A1-02`, `IA0R-D05` | Confirmée |
| `PAY-SRC-007` | Chaque requête transporte une clé d’idempotence | Architecture SIXPAY | `SRC-A5-02`, ADR API/idempotence ; `US-10` | Confirmée |
| `PAY-SRC-008` | Chaque traitement transporte un identifiant de corrélation | Architecture SIXPAY | `SRC-A2-03`, `BR-PLT-002` ; `SRC-A5-02` | Confirmée |
| `PAY-SRC-009` | Le message entrant est validé avant traitement | Fonctionnel | `SRC-A2-03`, `BP-03` | Confirmée |
| `PAY-SRC-010` | La demande est persistée avant tout appel externe | Décision SIXPAY | `SRC-A1-01` et `SRC-A1-02` | Confirmée |

### 6.2 Abonnement, client et compte

| ID | Exigence normalisée | Origine | Source principale | Statut |
| --- | --- | --- | --- | --- |
| `PAY-SRC-011` | TRESOR PAY est maître de l’abonnement MVP | Décision SIXPAY | `SRC-A1-01` | Confirmée |
| `PAY-SRC-012` | Payment ne consulte ni ne modifie un abonnement local | Décision SIXPAY | `SRC-A1-01` ; contrats Subscription classés `EXCLUDED` | Confirmée |
| `PAY-SRC-013` | Une projection ObservedCustomer est créée dès la première demande, succès ou échec | Décision SIXPAY | `SRC-A1-01`, `SRC-A1-02` | Confirmée |
| `PAY-SRC-014` | Amplitude reste maître du client et du compte bancaires | Architecture/fonctionnel | `SRC-A2-03` et `SRC-A3-02` | Confirmée |
| `PAY-SRC-015` | SIXPAY vérifie l’existence et l’identité du client bancaire | Externe/fonctionnel | `SRC-A2-02`, `SRC-A2-03`, `SRC-A3-02` | Confirmée |
| `PAY-SRC-016` | SIXPAY vérifie NIU, appartenance du compte et faits KYC nécessaires | Décision + contrat de référence | `SRC-A1-01`, `SRC-A3-02` | Confirmée |
| `PAY-SRC-017` | Le compte doit être actif et non bloqué/non opposé | Fonctionnel | `SRC-A2-02`, fonction 21 ; `SRC-A2-03`, `BR-CUS-002/003` | Confirmée |
| `PAY-SRC-018` | Le solde disponible doit être contrôlé avant l’écriture | Externe/fonctionnel | `SRC-A2-01`, vérification de solde ; `BR-PAY-003` | Confirmée |
| `PAY-SRC-019` | Le rapprochement interbancaire d’ObservedCustomer n’est jamais automatique | Décision SIXPAY | `SRC-A1-02`, `IA0R-D01/D02` | Confirmée |

### 6.3 Exécution financière

| ID | Exigence normalisée | Origine | Source principale | Statut |
| --- | --- | --- | --- | --- |
| `PAY-SRC-020` | Amplitude exécute les écritures bancaires ; SIXPAY les orchestre | Externe + architecture | `SRC-A2-01`, étape 4 ; `SRC-A4-01` | Confirmée |
| `PAY-SRC-021` | L’effet cible est le débit du client et le crédit du CUT | Externe | `SRC-A2-01`, objectif et flux API | Confirmée |
| `PAY-SRC-022` | L’opération Core Banking cible est atomique | Décision SIXPAY | `SRC-A1-02`, `IA0R-D07` | Confirmée |
| `PAY-SRC-023` | Une demande rejetée ne produit aucune écriture | Fonctionnel | `SRC-A2-03`, `BR-PAY-005` | Confirmée |
| `PAY-SRC-024` | Un résultat inconnu n’est jamais rejoué aveuglément | Décision SIXPAY | `SRC-A1-02`, `IA0R-D07` | Confirmée |
| `PAY-SRC-025` | Un résultat partiel ouvre un rapprochement et, si nécessaire, une extourne explicite | Décision SIXPAY | `SRC-A1-02`, `IA0R-D07` | Confirmée |
| `PAY-SRC-026` | Le posting comptable est idempotent et ne peut créer un double débit/crédit | Architecture + backlog | `SRC-A5-02`, `US-19` | Confirmée |
| `PAY-SRC-027` | Le contrat actuel de vérification Amplitude ne suffit pas à exécuter le paiement | Registre | `SRC-A3-01`, `SRC-A3-02` | Confirmée |

### 6.4 Résultat immédiat, TFJ et quittance

| ID | Exigence normalisée | Origine | Source principale | Statut |
| --- | --- | --- | --- | --- |
| `PAY-SRC-028` | TRESOR PAY reçoit un résultat immédiat après l’effet bancaire connu | Externe | `SRC-A2-01`, étape 5 ; `US-11` | Confirmée |
| `PAY-SRC-029` | Le résultat immédiat distingue succès, rejet métier, échec technique et traitement en cours | Décision dérivée SIXPAY | `IA0R-D07` + exigences de rejet motivé `SRC-A2-03` | Confirmée |
| `PAY-SRC-030` | Le débit/crédit immédiat ne vaut pas confirmation finale TFJ | Externe + décision | `SRC-A2-01`, étapes 5 et 6 ; `IA0R-D08` | Confirmée |
| `PAY-SRC-031` | Amplitude fournit la confirmation TFJ, avec interrogation de rapprochement en fallback | Décision SIXPAY | `SRC-A1-02`, `IA0R-D08` | Confirmée |
| `PAY-SRC-032` | Une confirmation TFJ doit être corrélée et traitée de manière idempotente | Décision SIXPAY | `SRC-A1-02`, `IA0R-D08` | Confirmée |
| `PAY-SRC-033` | Une confirmation non rapprochée est mise en quarantaine et n’actualise pas Payment | Décision SIXPAY | `SRC-A1-02`, `IA0R-D08` | Confirmée |
| `PAY-SRC-034` | SIXPAY notifie TRESOR PAY après confirmation d’intégration au CUT | Externe + décision | `SRC-A2-01`, étape 6 ; `IA0R-D08` | Confirmée |
| `PAY-SRC-035` | TRESOR PAY produit et met à disposition la quittance | Externe | `SRC-A2-01`, étape 7 | Confirmée |
| `PAY-SRC-036` | SIXPAY ne génère pas la quittance et conserve seulement sa notification/trace | Décision dérivée SIXPAY | Dérivée de `PAY-SRC-035` et des frontières IA-0R | Confirmée |

### 6.5 Fiabilité, audit et consultation

| ID | Exigence normalisée | Origine | Source principale | Statut |
| --- | --- | --- | --- | --- |
| `PAY-SRC-037` | Les événements et notifications utilisent une Transactional Outbox | Architecture SIXPAY | `SRC-A5-02`, ADR Outbox ; `US-13` requalifiée | Confirmée |
| `PAY-SRC-038` | Les consommateurs et projections sont idempotents | Architecture SIXPAY | `SRC-A5-02`, ADR-070 | Confirmée |
| `PAY-SRC-039` | Retry, backoff, circuit breaker et DLQ sont configurables | Architecture + décision | `SRC-A1-02`, `SRC-A3-02`, `SRC-A5-02` | Confirmée |
| `PAY-SRC-040` | Les defaults techniques ne constituent pas un SLA contractuel | Décision SIXPAY | `SRC-A1-02`, `IA0R-D06` | Confirmée |
| `PAY-SRC-041` | Chaque demande, y compris échouée, reste consultable et auditée | Décision + backlog | `SRC-A1-01`, `US-12` | Confirmée |
| `PAY-SRC-042` | SIXPAY présente une timeline des états et appels corrélés | Backlog compatible | `US-12`, architecture d’audit | Confirmée |
| `PAY-SRC-043` | Les identifiants de compte sont masqués dans l’interface et les logs | Décision SIXPAY | `SRC-A1-02`, `IA0R-D03` | Confirmée |
| `PAY-SRC-044` | Les recherches, détails et exports sont soumis au RBAC et audités | Décision SIXPAY | `SRC-A1-02`, `IA0R-D03` | Confirmée |
| `PAY-SRC-045` | La rétention suit la baseline IA-0R et accepte une politique légale plus stricte | Décision SIXPAY | `SRC-A1-02`, `IA0R-D04` | Confirmée |
| `PAY-SRC-046` | Aucun token ou secret n’est persisté ou journalisé | Décision SIXPAY | `SRC-A1-02`, `IA0R-D03/D05` | Confirmée |

### 6.6 Architecture et contrats à produire

| ID | Exigence normalisée | Origine | Source principale | Statut |
| --- | --- | --- | --- | --- |
| `PAY-SRC-047` | Le MVP reste un modular monolith Maven multi-modules | Architecture SIXPAY | `SRC-A5-02` | Confirmée |
| `PAY-SRC-048` | Payment est un module autonome respectant DDD/CQRS | Architecture SIXPAY | `SRC-A4-01`, `SRC-A5-01/02/03` | Confirmée |
| `PAY-SRC-049` | PostgreSQL porte le modèle d’écriture, l’outbox et les projections V1 | Architecture SIXPAY | `SRC-A5-02` | Confirmée |
| `PAY-SRC-050` | Les interfaces sont versionnées et décrites en OpenAPI 3.x | Architecture SIXPAY | `SRC-A5-02`, ADR-072 à ADR-075 | Confirmée |
| `PAY-SRC-051` | Le Contract Pack doit définir l’API de demande Payment TRESOR PAY → SIXPAY | Registre | `SRC-A3-01` | À produire en IA-0.5P |
| `PAY-SRC-052` | Le Contract Pack doit définir contrôle des fonds et débit/crédit CUT vers Amplitude | Registre | `SRC-A3-01`, limite `SRC-A3-02` | À produire en IA-0.5P |
| `PAY-SRC-053` | Le Contract Pack doit définir la notification immédiate vers TRESOR PAY | Registre | `SRC-A3-01` | À produire en IA-0.5P |
| `PAY-SRC-054` | Le Contract Pack doit définir la confirmation TFJ et la notification finale | Registre + décision | `SRC-A3-01`, `IA0R-D08` | À produire en IA-0.5P |
| `PAY-SRC-055` | Les erreurs externes utilisent RFC 7807 et des codes métier stables | Architecture + contrats existants | `SRC-A3-02/03`, `SRC-A5-02` | À formaliser en IA-0.5P |

## 7. Registre des contradictions et arbitrages

| ID | Contradiction historique | Arbitrage applicable |
| --- | --- | --- |
| `CONFLICT-001` | Les anciens CDC, blueprints et user stories placent l’abonnement dans SIXPAY | TRESOR PAY est maître ; aucun Subscription local dans le MVP (`SRC-A1-01`) |
| `CONFLICT-002` | Certains documents imposent une simple réservation de fonds | Le parcours courant exige débit client + crédit CUT ; une réservation éventuelle n’est qu’une étape interne Amplitude |
| `CONFLICT-003` | Les user stories imposent mTLS + HMAC | Le MVP exige Token + Subscription Key ; OAuth2/mTLS sont différés (`IA0R-D05`) |
| `CONFLICT-004` | `US-13` cite Celery | La stack est Java/Spring ; conserver le besoin de worker fiable et d’outbox, supprimer le choix Celery |
| `CONFLICT-005` | Des SLA de 2 s, 3 s et 30 s apparaissent dans le backlog | Ils sont non contractuels ; les valeurs restent configurables jusqu’à validation bancaire (`IA0R-D06`) |
| `CONFLICT-006` | `US-17` demande une synchronisation bidirectionnelle Customer/Amplitude | Amplitude reste maître ; SIXPAY effectue des lectures fraîches et maintient `ObservedCustomer` |
| `CONFLICT-007` | Des documents confondent comptabilisation immédiate, TFJO et réception CUT | Le résultat immédiat et la finalité TFJ sont deux faits distincts (`IA0R-D08`) |
| `CONFLICT-008` | Le cahier d’interopérabilité décrit TRESOR PAY → API Banque sans nommer SIXPAY | SIXPAY est l’implémentation de la façade/API bancaire et l’orchestrateur interne de la banque |
| `CONFLICT-009` | Les contrats TRESOR PAY existants semblent activables car présents dans le dépôt | Ils sont `DEFERRED_FUTURE` et `EXCLUDED` selon le registre |
| `CONFLICT-010` | Le contrat Amplitude existant peut sembler couvrir tout le paiement | Il est `REFERENCE_ONLY` et exclut solde disponible, posting, débit, CUT et TFJ |

## 8. Lacunes identifiées pour les étapes suivantes

Les éléments suivants n’ont pas encore de contrat Payment et devront être
fermés progressivement par IA-0P puis IA-0.5P :

1. schéma exact de la demande Payment TRESOR PAY ;
2. sémantique précise de la référence de recouvrement et de
   `Idempotency-Key` ;
3. modèle synchrone/asynchrone de l’accusé de réception ;
4. catalogue final des rejets métier et erreurs techniques ;
5. opération Amplitude de contrôle du solde disponible ;
6. opération Amplitude atomique de débit client/crédit CUT ;
7. protocole exact de recherche d’une écriture au résultat inconnu ;
8. payload de notification du résultat immédiat ;
9. payload et canal de confirmation TFJ Amplitude ;
10. payload de notification finale TRESOR PAY ;
11. dates de cut-off, calendrier bancaire et notion de `businessDate` ;
12. devise(s), précision monétaire et règles d’arrondi ;
13. source et paramétrage du compte CUT ;
14. seuils de paiement et éventuelles validations supplémentaires ;
15. responsabilités exactes de notification directe du client par la banque.

Une lacune ne doit pas être comblée silencieusement par l’IA. Elle doit être
traitée comme décision dans une étape ultérieure ou marquée configurable dans le
contrat.

## 9. Sources exclues de l’autorité Payment MVP

Ne peuvent pas servir à générer le Payment MVP :

- contrats TRESOR PAY d’autorisation d’abonnement ;
- workflows locaux de création/validation/suspension d’abonnement ;
- scénarios SFTP/Sandbox ;
- paiements initiés depuis une application bancaire mobile ;
- KYC numérique par pièce d’identité et selfie ;
- technologies citées historiquement mais absentes de la stack retenue,
  notamment Celery ;
- valeurs de SLA non approuvées ;
- exemples ou diagrammes contredisant IA-0R.

## 10. Contrôle de couverture

| Catégorie | Exigences recensées | Sans source | Décisions SIXPAY explicites |
| --- | ---: | ---: | ---: |
| Entrée, sécurité, identité | 10 | 0 | 4 |
| Abonnement, client, compte | 9 | 0 | 5 |
| Exécution financière | 8 | 0 | 3 |
| Résultat, TFJ, quittance | 9 | 0 | 5 |
| Fiabilité, audit, consultation | 10 | 0 | 7 |
| Architecture et contrats | 9 | 0 | 0 |
| **Total** | **55** | **0** | **24** |

Les exigences dérivées sont explicitement identifiées dans la matrice et
rattachées à leurs prémisses. Aucune exigence Payment recensée n’est dépourvue
de source ou de décision SIXPAY.

## 11. Verdict 0P.1

```text
PAYMENT SOURCE BASELINE: ESTABLISHED
REQUIREMENTS WITHOUT SOURCE OR SIXPAY DECISION: 0
LEGACY CONFLICTS: IDENTIFIED AND ARBITRATED
PAYMENT CODE GENERATION: FORBIDDEN
NEXT STEP: 0P.2 — MVP PAYMENT SCOPE
```

Cette baseline devient la référence obligatoire de toutes les étapes IA-0P.
Toute nouvelle exigence devra citer un identifiant `PAY-SRC-*`, une source
cataloguée `SRC-*` ou être enregistrée comme nouvelle décision SIXPAY.
