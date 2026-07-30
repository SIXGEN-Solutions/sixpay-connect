# SIXPAY CONNECT — Gate IA-0 Customer / Subscription

| Métadonnée | Valeur |
| --- | --- |
| Statut | **READY FOR CONTRACT PACK — décisions structurantes approuvées** |
| Type | Préflight métier et architectural, sans génération de code |
| Branche inspectée | `feat/industrialisation-documentation` |
| Commit de référence | `ffd942ade28fed4c938d628d5269ef373e1a0d24` |
| Date de l’analyse | 29 juillet 2026 |
| Master Prompt applicable au prochain Gate | `MASTER_ENGINEERING_PROMPT_V0.md` |
| Niveau d’autonomie | **A — Assistance** |
| Golden Module | `backend/partner` pour les conventions, jamais pour le métier |

## 1. Verdict

Le nouveau cahier des charges d’interopérabilité précise l’origine réelle de la
demande. Le parcours consolidé candidat est :

1. l’usager crée son compte dans TRESOR PAY et rattache une banque et un RIB ;
2. TRESOR PAY génère, pour chaque RIB, une Demande d’Autorisation Permanente de
   Prélèvement avec référence unique/QR code et la transmet à la banque ;
3. l’usager présente à la banque la demande signée et sa pièce d’identité ;
4. l’agent bancaire retrouve la demande, puis `Customer` consulte Amplitude et
   vérifie l’identité, le KYC, le compte et les restrictions ;
5. l’agent rejette la demande ou cosigne l’autorisation ;
6. `Subscription` enregistre la décision selon le niveau de validation configuré,
   active l’abonnement et notifie TRESOR PAY ;
7. seul un abonnement actif permet ensuite l’exécution d’un paiement.

Il n’existe donc ni création de client maître dans SIXPAY CONNECT, ni abonnement
implicite déclenché par une première demande de paiement. Un abonnement actif est une
précondition du paiement.

Les décisions fonctionnelles structurantes sont approuvées. Le Gate IA-0 autorise
désormais la production du Contract Pack, mais pas encore la génération du backend.
Le code pourra commencer lorsque les trois contrats TRESOR PAY, Subscription et
Amplitude auront été écrits, validés et référencés dans le manifeste.

Les éléments restant à préciser ne remettent pas en cause le modèle :

1. les valeurs de timeout, retry et SLA Amplitude seront définies avant les tests de
   performance et la production ;
2. le socle KYC est approuvé et pourra être étendu par configuration ;
3. les User Stories et critères d’acceptation doivent être complétés à partir du
   présent modèle ;
4. les OpenAPI et schémas de webhook doivent être produits au Gate IA-0.5.

## 1.1 Décisions approuvées

| Sujet | Décision |
| --- | --- |
| Scénario bancaire MVP | REST. SFTP/Sandbox et applications mobiles sont différés |
| Transmission de la demande | API push TRESOR PAY → SIXPAY |
| Notifications | Webhook SIXPAY → TRESOR PAY, signé et rejouable |
| Interface de l’agent | Frontend SIXPAY |
| Mandat signé | Stockage documentaire SIXPAY ; GED bancaire comme évolution |
| Marchand | Hors périmètre et hors modèle SIXPAY |
| Partner SIXPAY | TRESOR PAY |
| Institutions financières | Régionale, Afriland First Bank, First Bank, etc. ; contexte bancaire, pas des marchands |
| Identifiants d’autorisation | Token TRESOR PAY et Subscription Key SIXPAY |
| Validation | Seuils, catégories et nombre de validations paramétrables |
| KYC initial | NIU, raison sociale, téléphone, adresse e-mail |
| Extensibilité KYC | Champs additionnels configurables et versionnés |
| Amplitude | Intégration REST ; temps de réponse et SLA à préciser ultérieurement |
| Cycle public Payment | `PAYMENT_TO_COLLECT` → `RECEIPT_NOTIFIED` |

## 2. Sources d’autorité inventoriées

### 2.1 Corpus métier et architecture

| Niveau | Source | Portée Customer/Subscription | Observation |
| --- | --- | --- | --- |
| 0 | `documentation/requirements/cdc/Cahier des Charges interopérabilité entre TRESOR PAY et le core banking_final_schema_final.docx` | création/validation du compte TRESOR PAY, autorisation permanente, données, scénarios d’échange et schémas | **Besoin d’interopérabilité externe ; DRAFT 1.0** |
| 0 | `documentation/requirements/cdc/SIXPAY_CONNECT_Specifications_Fonctionnelles.pdf` | traduction SIXPAY : BP-02/CBF-02, BR-CUS/BR-SUB, acteurs, traçabilité et points ouverts | Référence fonctionnelle SIXPAY à aligner sur le cahier externe |
| 1 | `documentation/architecture/SIXPAY_CONNECT_Product_Blueprint.docx` | capacités, domaines, modèle d’information, règles, événements, cycles de vie | Référence métier la plus structurée |
| 1 | `documentation/architecture/SIXPAY_CONNECT_Application_Architecture_Blueprint.docx` | services, composants, interactions, API landscape, agrégats et états | Confirme que Customer est référencé depuis le Core Banking |
| 1 | `documentation/architecture/SIXPAY_CONNECT-SDS.docx` | commandes, queries, DTO, repositories, événements, specifications et erreurs | Contredit partiellement le statut de Customer comme donnée externe |
| 1 | `documentation/architecture/SIXPAY_CONNECT_Technical_Architecture.docx` | modules, sécurité, DDD, API, IAM, persistance, événements et NFR | Référence technique ; contient plusieurs catalogues génériques à préciser |
| 1 | `documentation/architecture/SIXPAY_CONNECT_SDS.docx` | structure de repository et implémentation de référence | Contenu extrait identique à `SIXPAY_CONNECT_Guide_Implementation.docx` |
| 1 | `documentation/architecture/SIXPAY_CONNECT_Guide_Implementation.docx` | structure de repository et implémentation de référence | Doublon textuel de `SIXPAY_CONNECT_SDS.docx` |
| 2 | `documentation/requirements/cdc/SIXPAY_CONNECT_CDC.pdf` | dix fonctionnalités Customer/abonnement et processus TRESOR PAY | Source fonctionnelle explicite |
| 2 | `documentation/requirements/user-stories/SIXPAY_CONNECT_USER_STORIES.docx` | critères d’acceptation, sécurité, audit et recherche | L’EPIC 1 ne contient que les stories Partner |
| 3 | `documentation/requirements/cdc/SIXPAY_CONNECT_CDC.pdf` | catalogue historique de dix fonctions Customer/abonnement | À interpréter conformément à la spécification fonctionnelle plus récente |

### 2.2 Gouvernance et génération

| Source | Rôle |
| --- | --- |
| `AI_GENERATION_STRATEGY.md` | ordre d’autorité, autonomie, Gates et interdiction d’inventer |
| `MASTER_ENGINEERING_PROMPT_V0.md` | protocole de génération backend |
| `DOMAIN_GENERATION_BRIEF_TEMPLATE.md` | structure du brief |
| `AI_CONTEXT_MANIFEST_TEMPLATE.yaml` | contexte reproductible de campagne |
| `backend/SIXPAY_BACKEND_TECHNOLOGY_MATRIX.md` | baseline Java 21, Spring Boot 4.1, Maven et PostgreSQL |
| `backend/SIXPAY_BACKEND_ENGINEERING_GENERATION_CONTRACT.md` | architecture, sécurité, persistance, tests et restrictions |
| `.github/workflows/backend-ci.yml` | Gate CI backend |
| `.github/CODEOWNERS` | autorités de review |

### 2.3 Références d’implémentation

| Source | Usage autorisé |
| --- | --- |
| `backend/partner/ARCHITECTURE.md` | architecture hexagonale, audit append-only, Outbox et auto-configuration |
| `backend/partner/ACCEPTANCE-TRACEABILITY.md` | format de traçabilité exigence → test → preuve |
| `backend/partner/src/main/resources/openapi/partner-api-v1.yaml` | conventions OpenAPI, RFC 7807, corrélation et idempotence |
| `backend/partner/**` | conventions structurelles et qualité seulement |
| `backend/customer/**` | squelette du module Customer ; aucun métier implémenté |
| `backend/subscription/**` | squelette du module Subscription ; aucun métier implémenté |
| `frontend/src/app/features/customers/**` | squelette frontend générique, non contractuel |
| `frontend/src/app/features/subscriptions/**` | squelette frontend générique, non contractuel |

### 2.4 Problèmes documentaires

- `SIXPAY_CONNECT_SDS.docx` et `SIXPAY_CONNECT_Guide_Implementation.docx`
  produisent le même contenu textuel, malgré des fichiers binaires différents.
- `SIXPAY_CONNECT-SDS.docx` et `SIXPAY_CONNECT_SDS.docx` désignent deux volumes
  différents avec des noms presque identiques.
- `Product_SIXPAY_CONNECT.docx` reprend le Product Blueprint mais contient aussi
  des ajouts issus du backlog et des notes de cadrage. Son statut par rapport à
  `SIXPAY_CONNECT_Product_Blueprint.docx` n’est pas déclaré.
- Les DOCX ne portent pas de version ni de référence d’approbation exploitable
  dans leur nom.
- La spécification fonctionnelle indique qu’elle doit faire l’objet d’un sign-off
  formel de LA RÉGIONALE BANK. Aucun sign-off complété n’apparaît dans le document.
- Le cahier d’interopérabilité porte la mention `DRAFT 1.0`, ne fournit pas de
  schémas OpenAPI/WSDL ni de formats de fichiers exécutables, et présente plusieurs
  scénarios alternatifs sans indiquer celui retenu pour LA RÉGIONALE BANK.

**Décision documentaire attendue :** désigner un fichier canonique par volume,
attribuer une version et archiver ou renommer les doublons.

## 3. Exigences Customer/Subscription consolidées

| ID | Exigence | Source principale | Domaine pressenti |
| --- | --- | --- | --- |
| CUS-REQ-001 | Rechercher le client bancaire à partir de son matricule/référence | CDC #2 | Customer |
| CUS-REQ-002 | Récupérer identité, KYC et comptes depuis le Core Banking | CDC #2, Volumes | Customer |
| CUS-REQ-003 | Permettre à l’agent de vérifier la conformité documentaire | CDC #2 | Customer + processus humain |
| CUS-REQ-004 | Vérifier existence, KYC, compte actif et absence de blocage/opposition | Volumes | Customer |
| INT-REQ-001 | Recevoir ou récupérer une demande d’autorisation générée par TRESOR PAY pour chaque RIB | CDC interop §IV.1 | Integration + Subscription |
| INT-REQ-002 | Transporter la référence unique, le QR code et le document d’autorisation signé | CDC interop §IV.1-2 | Integration + document management à décider |
| INT-REQ-003 | Identifier l’usager par NIU, nom/raison sociale, type, téléphone, e-mail et pièce/registre de commerce | CDC interop §IV.1 | Customer canonical model |
| INT-REQ-004 | Identifier le rattachement bancaire par banque, RIB et IBAN ou numéro client | CDC interop §IV.1 | Customer/Subscription |
| INT-REQ-005 | Retourner à TRESOR PAY la décision, le statut et l’identité professionnelle du gestionnaire | CDC interop §IV.2 | Subscription + Integration |
| INT-REQ-006 | Sécuriser les échanges par TLS et authentification forte OAuth2/JWT/mTLS | CDC interop §III | Integration/Security |
| INT-REQ-007 | Utiliser la référence de recouvrement TRESOR PAY comme clé transverse du paiement | CDC interop §III.6 | Payment/Integration |
| SUB-REQ-001 | Enregistrer une demande d’abonnement issue de TRESOR PAY et permettre son instruction par un agent | CDC interop + spécification BP-02/CBF-02 | Integration + Subscription |
| SUB-REQ-002 | Vérifier le client, son KYC et son compte dans Amplitude avant création/activation | Spécification CBF-02, BR-CUS-001..004 | Customer + Subscription |
| SUB-REQ-003 | Associer chaque abonnement à un seul Partner | Spécification BR-SUB-002 | Subscription |
| SUB-REQ-004 | Permettre à un client d’être abonné à plusieurs partenaires | Spécification BR-SUB-003 | Subscription |
| SUB-REQ-005 | Générer une Subscription Key unique | Spécification BP-02, BR-SUB-004 | Subscription |
| SUB-REQ-006 | Appliquer une validation simple ou double selon un seuil configurable | Spécification BP-02, §15.1 | Subscription |
| SUB-REQ-007 | Activer l’abonnement uniquement après validation et notifier le partenaire | Spécification BP-02/CBF-02 | Subscription + Notification/Integration |
| SUB-REQ-008 | Exiger un abonnement actif avant tout paiement | Spécification BP-03, CBF-03, BR-SUB-001 | Subscription + Payment |
| SUB-REQ-009 | Suspendre un abonnement et interdire toute nouvelle opération jusqu’à réactivation | Spécification BR-SUB-005 | Subscription |
| SUB-REQ-010 | Rechercher un abonnement par critères | CDC #7 | Subscription read model |
| SUB-REQ-011 | Lister les abonnements avec filtres et pagination | CDC #8 | Subscription read model |
| SUB-REQ-012 | Lister les clients abonnés à un partenaire | CDC #9 | Subscription read model |
| SUB-REQ-013 | Lister les partenaires auxquels un client est abonné | CDC #10 | Subscription read model |
| PAY-REQ-001 | Recevoir un ordre contenant référence de paiement, montant, token d’autorisation, RIB débiteur et RIB bénéficiaire | CDC interop scénario API | Payment/Integration |
| PAY-REQ-002 | Exécuter automatiquement l’ordre après contrôles et notifier le débit | CDC interop scénario API | Payment/Customer/Amplitude |
| PAY-REQ-003 | Exposer le cycle public `PAYMENT_TO_COLLECT` puis `RECEIPT_NOTIFIED`, tout en auditant les étapes techniques | Arbitrage Gate IA-0 + CDC interop | Payment/Accounting/Settlement |
| X-REQ-001 | Auditer création, consultation sensible, validation, suspension et appels partenaires | US-40, Volumes | Transverse |
| X-REQ-002 | Appliquer RBAC et autorisation au niveau objet | US-33, contrats | Backend |
| X-REQ-003 | Protéger données KYC, secrets et clés ; ne jamais les journaliser | US-35/36, Volumes | Backend/infrastructure |
| X-REQ-004 | Propager corrélation et garantir l’idempotence des mutations | Contrat backend | Backend |
| X-REQ-005 | Publier les faits métier de manière fiable via Outbox | Golden Partner, contrat backend | Backend |

## 4. Contradictions et écarts

| ID | Sujet | Source A | Source B | Conséquence |
| --- | --- | --- | --- | --- |
| GAP-001 | Couverture backlog | CDC : 10 fonctions Customer/abonnement | User Stories EPIC 1 : 6 stories Partner uniquement | Aucun critère d’acceptation approuvé pour le pilote |
| GAP-002 | Nature du Customer | Product/Application : Core Banking source de vérité, Customer seulement référencé | SDS : `RegisterCustomer`, `UpdateCustomer`, `SuspendCustomer` et événements associés | Risque de créer un faux référentiel client local |
| GAP-003 | Portée de Subscription | CDC historique distingue « abonner un client » et « abonner chez un partenaire » | Spécification : une Subscription relie toujours un client à un seul Partner | La spécification prime : un seul agrégat `Subscription`, sans enrôlement global séparé |
| GAP-004 | Secret métier | CDC historique parle de code secret client et de clé partenaire | Spécification ne retient qu’une `Subscription Key` unique | Le code secret client n’entre pas dans le modèle tant qu’il n’est pas réintroduit par une exigence approuvée |
| GAP-005 | Validation | CDC historique exige une double validation | Spécification : simple ou double selon seuil configurable | Seuils, catégories de partenaire et règle maker-checker à approuver |
| GAP-006 | Cycle de vie | Product : Requested, Pending Validation, Validated, Active, Suspended, Renewed, Revoked | Application : Requested, Pending Validation, Validated, Activated, Suspended, Reactivated, Expired | Contrat public et événements incompatibles |
| GAP-007 | Transitions manquantes | Product table ne définit pas `Pending Validation → Validated` | Catalogue publie `Activated`, mais pas toujours `Validated`/`Reactivated` | Machine à états incomplète |
| GAP-008 | Commandes | SDS : Create/Activate/Cancel/Renew | CDC : create, validate twice, suspend ; aucun cancel/renew | Risque de générer des fonctions hors périmètre |
| GAP-009 | Événements Customer | Product : Identified, Verified, AccountValidated | SDS : Registered, Updated, Suspended | Sémantique et propriété des données incompatibles |
| GAP-010 | Accès aux API bancaires | Application : Customer Banking APIs jamais exposées directement aux partenaires | CDC #6 : abonnement via API partenaire nécessitant vérification client | Orchestration et minimisation de données à préciser |
| GAP-011 | Rôles | Technical Architecture : Agent/Superviseur et `Subscription Manager` | Code : `OPS`, `MANAGER`, `ADMIN`, `AUDITOR`, etc. | Matrice d’autorisation non exécutable sans mapping |
| GAP-012 | Identifiant client | CDC utilise « matricule » | Volumes utilisent `CustomerNumber`, `CustomerId`, `CustomerReference` | Contrat de lookup et unicité indéterminés |
| GAP-013 | Contrats | Les volumes donnent des routes génériques | Aucun OpenAPI Customer/Subscription versionné | Génération contract-first bloquée |
| GAP-014 | NFR | Exigences globales de sécurité et audit présentes | Latence Core Banking, pagination, volumétrie et rétention non chiffrées | Tests d’acceptation non mesurables |
| GAP-015 | Sign-off fonctionnel | La spécification est déclarée préalable au développement | Tableau de signature non complété et points §15.1 ouverts | La source est exploitable pour le préflight, mais son approbation métier reste un Gate |
| GAP-016 | Initiateur de la demande | Spécification SIXPAY : demande formulée auprès de l’agent | CDC interop : TRESOR PAY génère la demande par RIB, puis l’agent l’instruit | Le contrat entrant et l’UI d’initiation ne peuvent pas être générés sans arbitrage ; recommandation : le CDC interop prime |
| GAP-017 | Poste de travail de l’agent | Spécification SIXPAY suggère un traitement SIXPAY | CDC interop : gestionnaire connecté à « l’espace banque de TRESOR PAY » | Décider si SIXPAY fournit son propre écran, s’intègre par API, ou si l’agent reste dans TRESOR PAY |
| GAP-018 | Objet juridique | CDC interop : Autorisation Permanente de Prélèvement signée, par RIB | Volumes : Subscription logique Client–Partner | Le modèle doit conserver référence, RIB masqué, preuve et version du mandat sans confondre mandat et abonnement |
| GAP-019 | Marchand/Partner | Schéma CDC : un client peut avoir plusieurs « marchands » par banque | SIXPAY : une Subscription est associée à un Partner | Mapping métier absent ; risque de confondre TRESOR PAY, banque, service public et marchand |
| GAP-020 | Clés et tokens | CDC interop : référence de recouvrement, référence d’autorisation, token TRESOR PAY, QR code | SIXPAY : Subscription Key et référence Payment | Catalogue, portée, confidentialité, unicité et durée de vie non définis |
| GAP-021 | Niveau de validation | Schéma CDC : niveaux/validateurs et durée de validité configurables | Spécification SIXPAY : validation simple ou double selon seuil | Le moteur de décision doit lire une politique versionnée ; valeurs métier absentes |
| GAP-022 | Scénario d’intégration | CDC interop propose REST/SOAP, SFTP/Sandbox et deux scénarios mobiles | Architecture SIXPAY vise API modernes | Le MVP LA RÉGIONALE BANK doit sélectionner explicitement le scénario |
| GAP-023 | Cycle Payment | CDC interop distingue débit, réception CUT, réconciliation, quittance | Product/Application utilisent des états Payment/Settlement plus génériques | Payment ne peut pas considérer le débit comme règlement final |
| GAP-024 | `MerchantRepository` | Volume Repository Design catalogue un `MerchantRepository` | Décision Gate : Marchand est géré exclusivement dans TRESOR PAY | Ne générer ni module, ni agrégat, ni repository Merchant dans SIXPAY ; corriger ultérieurement le catalogue documentaire |

## 5. Langage métier proposé

Ce langage est proposé pour arbitrage ; il ne devient normatif qu’après approbation.

| Terme | Définition recommandée | Terme à éviter |
| --- | --- | --- |
| Client bancaire | Personne ou organisation administrée par le Core Banking et référencée par SIXPAY CONNECT | client local, utilisateur |
| Référence client | Identifiant métier immuable fourni par le Core Banking | matricule, sauf si officiellement défini |
| Profil KYC | Vue des données de connaissance client nécessaires à une décision d’éligibilité | dossier KYC complet si non stocké |
| Compte client | Compte bancaire rattaché au client et administré par le Core Banking | wallet |
| Vérification client | Consultation du Core Banking suivie des contrôles identité/KYC/compte | création client |
| Demande d’autorisation | Demande générée par TRESOR PAY pour un RIB et instruite par la banque | création client |
| Autorisation Permanente de Prélèvement | Mandat juridique cosigné par l’usager et la banque, référencé par la demande | Subscription Key |
| Demande d’abonnement | Représentation SIXPAY de la demande d’autorisation reçue de TRESOR PAY | création client |
| Abonnement | Autorisation entre un client bancaire et un Partner unique, créée après vérification Customer et activée après validation | enrôlement global |
| Clé d’abonnement | Clé unique rattachée à une Subscription | code secret client |
| Référence de recouvrement | Identifiant unique généré par TRESOR PAY et propagé dans toute la chaîne de paiement | Subscription Key |
| Token d’autorisation TRESOR PAY | Jeton porté par un ordre de prélèvement ; nature cryptographique et durée à contractualiser | mot de passe client |
| Marchand TRESOR PAY | Institution financière choisie par l’usager dans TRESOR PAY ; concept externe non administré par SIXPAY | Aggregate Root `Merchant` |
| Partner SIXPAY | Plateforme externe intégrée à SIXPAY ; pour le MVP, TRESOR PAY | banque commerciale |
| Institution financière | Banque détenant le compte de l’usager : Régionale, Afriland First Bank, First Bank, etc. | marchand SIXPAY |
| Initiateur | Agent qui crée la demande | validateur |
| Validateur | Agent habilité qui rend une décision de validation | administrateur générique |
| Validation simple | Une décision positive d’un validateur habilité lorsque le seuil configuré le permet | auto-activation |
| Double validation | Deux décisions positives de validateurs distincts lorsque le seuil configuré l’exige | simple changement de statut |
| Suspension d’abonnement | Blocage des nouvelles opérations pour la relation Client–Partner concernée | suppression |
| Révocation | Fin définitive d’une autorisation | suspension |

## 6. Frontières recommandées

### 6.1 Customer Domain

**Responsabilités**

- rechercher un client dans le Core Banking ;
- exposer une vue normalisée et minimale de son identité, KYC et de ses comptes ;
- évaluer l’éligibilité bancaire nécessaire à une demande d’abonnement ;
- conserver, si approuvé, uniquement une référence et un snapshot de vérification
  auditable.

**Ne possède pas**

- l’identité maître du client ;
- le dossier KYC maître ;
- les soldes et états maîtres des comptes ;
- le cycle de vie de l’abonnement à un partenaire.

### 6.2 Subscription Domain

**Responsabilités**

- porter l’unique agrégat `Subscription`, associé à un client et un Partner ;
- recevoir, via Integration, la demande d’autorisation TRESOR PAY et préserver sa
  référence externe ;
- orchestrer la vérification Customer préalable à la création ;
- déterminer le niveau de validation applicable à partir de la configuration ;
- gérer validation, activation, suspension et réactivation ;
- générer et invalider la Subscription Key ;
- produire les projections de recherche par client et par partenaire.

### 6.3 Interactions

- Integration authentifie et traduit la demande TRESOR PAY vers un contrat canonique.
- Customer interroge le Core Banking à travers un port et un adaptateur.
- Subscription consomme un contrat public Customer, jamais son repository ou son
  agrégat.
- Subscription consomme le statut public Partner, jamais le code interne Partner.
- Payment vérifie qu’une Subscription Client–Partner est `ACTIVE` avant de poursuivre.
- Aucun module métier ne dépend directement d’un autre module métier.

## 7. Modèle métier provisoire

### 7.1 Customer

| Élément | Type | Responsabilité |
| --- | --- | --- |
| `CustomerProfile` | Modèle canonique non maître | vue minimale renvoyée par Amplitude |
| `CustomerReference` | Value Object | identifiant métier Core Banking |
| `CustomerIdentity` | Value Object | identité minimale vérifiable |
| `KycProfile` | Value Object | NIU, raison sociale, téléphone, e-mail, attributs additionnels versionnés, date et source |
| `CustomerAccount` | Entity/snapshot | compte éligible sélectionnable |
| `AccountStatus` | Value Object/enum | actif, bloqué, en opposition, etc. |
| `CustomerVerification` | Décision métier | résultat horodaté et corrélé transmis à Subscription |

### 7.2 Subscription

| Élément | Type | Responsabilité |
| --- | --- | --- |
| `Subscription` | Aggregate Root | autorisation entre un client bancaire et un Partner unique |
| `SubscriptionId` | Value Object | identité de l’abonnement |
| `CustomerReference` | Value Object référencé | identité métier du client dans Amplitude |
| `PartnerId` | Value Object référencé | identité du partenaire dans SIXPAY |
| `AccountReference` | Value Object | compte vérifié pour l’abonnement, si retenu |
| `ExternalAuthorizationReference` | Value Object | référence immuable de la demande TRESOR PAY |
| `DebitMandateEvidence` | Value Object/Entity | métadonnées et empreinte du mandat signé, sans document brut par défaut |
| `ValidationPolicySnapshot` | Value Object | nombre de validations et règle appliquée à la demande |
| `ApprovalDecision` | Entity/Value Object | validateur, décision, date et motif |
| `SubscriptionKeyFingerprint` | Value Object | représentation protégée de la clé partenaire |
| `SuspensionReason` | Value Object | motif obligatoire |

La création d’un `CustomerEnrollment` global séparé n’est plus recommandée : elle
contredirait BR-SUB-002 et le flux CBF-02 de la spécification fonctionnelle.

### 7.3 Stockage documentaire du mandat

Le mandat signé est géré dans SIXPAY avec séparation entre :

- le contenu binaire chiffré dans un stockage objet privé ;
- les métadonnées, l’empreinte SHA-256, la classification, la taille, le type MIME,
  la version et la référence de stockage en base ;
- l’agrégat Subscription, qui ne conserve que l’identité de la preuve documentaire.

Le dépôt impose contrôle de type/taille, analyse antivirus, chiffrement, contrôle
d’accès objet, audit des lectures et interdiction d’inclure le document dans les logs,
événements ou réponses non dédiées.

## 8. Invariants proposés

| ID | Invariant |
| --- | --- |
| CUS-INV-001 | Une vue Customer ne peut être créée que pour une référence existant dans le Core Banking. |
| CUS-INV-002 | Une vérification d’éligibilité doit référencer sa source, son instant et sa corrélation. |
| CUS-INV-003 | Un compte éligible doit appartenir au client, être actif et ne pas être bloqué ou en opposition. |
| CUS-INV-004 | Les données KYC brutes ne sont pas persistées sans décision de classification et de rétention. |
| SUB-INV-001 | Une demande ne peut être créée qu’après vérification positive du client, du KYC et du compte dans Amplitude. |
| SUB-INV-002 | Une Subscription appartient à un seul couple Client–Partner et un seul abonnement non terminal peut exister pour ce couple. |
| SUB-INV-003 | Le Partner doit être actif lors de la création et de l’activation. |
| SUB-INV-003A | La référence externe TRESOR PAY est obligatoire et unique dans son périmètre. |
| SUB-INV-003B | Le RIB présenté doit correspondre au compte retourné par Amplitude et à l’usager vérifié. |
| SUB-INV-004 | Le nombre de validations exigé est déterminé par la configuration figée au moment de la demande. |
| SUB-INV-005 | L’initiateur ne peut pas valider sa propre demande. |
| SUB-INV-006 | Lorsque deux validations sont requises, elles proviennent de deux personnes distinctes. |
| SUB-INV-007 | La Subscription n’est active qu’après toutes les validations requises. |
| SUB-INV-008 | Une Subscription suspendue interdit toute nouvelle opération pour le Partner concerné. |
| SUB-INV-009 | La clé d’abonnement est unique, n’est jamais journalisée et n’est restituée en clair que selon un protocole approuvé. |
| SUB-INV-010 | Une mutation rejouée avec la même clé d’idempotence ne produit pas un second effet. |
| SUB-INV-011 | Une transition, son audit et ses événements Outbox sont persistés atomiquement. |
| SUB-INV-012 | Le document signé n’est ni journalisé ni propagé dans un événement ; seules ses métadonnées et son empreinte peuvent l’être. |

## 9. États et événements proposés

### 9.1 Subscription

| État source | Action | État cible | Événement |
| --- | --- | --- | --- |
| — | recevoir la demande TRESOR PAY | `PENDING_VERIFICATION` | `SubscriptionRequestReceived` |
| `PENDING_VERIFICATION` | vérifier usager, KYC, compte et mandat signé | `PENDING_VALIDATION` | `SubscriptionCustomerVerified` |
| `PENDING_VERIFICATION` | constater une irrégularité | `REJECTED` | `SubscriptionRejected` |
| `PENDING_VALIDATION` | validation unique lorsque requise = 1 | `ACTIVE` | `SubscriptionActivated` |
| `PENDING_VALIDATION` | première validation lorsque requise = 2 | `PARTIALLY_VALIDATED` | `SubscriptionApprovalRecorded` |
| `PARTIALLY_VALIDATED` | seconde validation | `ACTIVE` | `SubscriptionActivated` |
| `PENDING_VALIDATION` / `PARTIALLY_VALIDATED` | rejeter | `REJECTED` | `SubscriptionRejected` |
| `ACTIVE` | suspendre | `SUSPENDED` | `SubscriptionSuspended` |
| `SUSPENDED` | réactiver après contrôles requis | `ACTIVE` | `SubscriptionReactivated` |
| `ACTIVE` / `SUSPENDED` | révoquer, si retenu au MVP | `REVOKED` | `SubscriptionRevoked` |

Les événements `Registered/Updated/Suspended` du catalogue Customer ne sont pas retenus
tant que Customer reste une donnée administrée par le Core Banking.

## 10. Matrice rôles/actions provisoire

Mapping recommandé : `Agent → OPS`, `Superviseur/Subscription Manager → MANAGER`.

| Action | ADMIN | OPS | MANAGER | AUDITOR | READ_ONLY | SUPPORT | PARTNER |
| --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Rechercher un client | Oui | Oui | Oui | Masqué | Masqué | Masqué | Non |
| Consulter KYC/compte minimal | Oui | Oui | Oui | Selon mandat | Masqué | Masqué | Non |
| Importer une demande TRESOR PAY | Service technique | Non | Non | Non | Non | Non | Partenaire authentifié |
| Instruire/vérifier une demande | Non | Oui | Selon politique | Non | Non | Non | Non |
| Rechercher les demandes en attente | Non | Propre périmètre | Oui | Lecture | Lecture | Non | Non |
| Valider/rejeter | Non | Non | Oui | Non | Non | Non | Non |
| Suspendre/réactiver un abonnement | Non | Selon politique | Oui | Non | Non | Non | Non |
| Consulter le statut via API partenaire | Non | Non | Non | Non | Non | Non | Oui, partenaire courant |
| Rechercher/lister les abonnements | Oui | Oui | Oui | Lecture | Lecture limitée | Masqué | Propres abonnements |
| Lister les clients d’un partenaire | Oui | Oui | Oui | Lecture | Lecture limitée | Masqué | Partenaire courant |
| Lister les partenaires d’un client | Oui | Oui | Oui | Lecture | Lecture limitée | Masqué | Non |
| Consulter l’audit | Non | Non | Non | Oui | Non | Non | Non |

Contraintes obligatoires proposées :

- un `MANAGER` ne valide jamais une demande qu’il a initiée ;
- deux validations doivent provenir de sujets JWT distincts ;
- le partenaire ne peut agir que sur son propre `partnerId` dérivé de son identité ;
- les données KYC et comptes sont masquées selon le besoin d’en connaître ;
- tout refus `403` et tout accès sensible sont audités.

## 11. Contrats API nécessaires

Les routes suivantes sont des candidats à faire approuver, pas des contrats existants.

### 11.1 Customer interne

| Méthode | Route candidate | Operation ID | Objet |
| --- | --- | --- | --- |
| GET | `/api/v1/customers?customerReference=...` | `searchCustomers` | rechercher par référence Core Banking |
| GET | `/api/v1/customers/{customerReference}` | `getCustomer` | consulter la vue client |
| GET | `/api/v1/customers/{customerReference}/accounts` | `getCustomerAccounts` | lister les comptes autorisés |
| POST | `/api/v1/customer-verifications` | `verifyCustomer` | créer une preuve de vérification fraîche |

### 11.2 Subscription Management interne

| Méthode | Route candidate | Operation ID | Objet |
| --- | --- | --- | --- |
| POST | `/api/v1/subscription-requests/{id}/verification` | `verifySubscriptionRequest` | enregistrer les contrôles effectués par l’agent |
| GET | `/api/v1/subscriptions/{subscriptionId}` | `getSubscription` | consulter un abonnement autorisé |
| GET | `/api/v1/subscriptions` | `searchSubscriptions` | rechercher/lister avec filtres |
| POST | `/api/v1/subscriptions/{subscriptionId}/approvals` | `recordSubscriptionApproval` | valider ou rejeter selon le workflow |
| POST | `/api/v1/subscriptions/{subscriptionId}/suspension` | `suspendSubscription` | suspendre l’abonnement |
| POST | `/api/v1/subscriptions/{subscriptionId}/reactivation` | `reactivateSubscription` | réactiver après contrôles |
| GET | `/api/v1/partners/{partnerId}/subscriptions` | `getPartnerSubscriptions` | clients d’un partenaire |
| GET | `/api/v1/customers/{customerId}/subscriptions` | `getCustomerSubscriptions` | partenaires d’un client |

### 11.3 Contrat partenaire

Le contrat entrant TRESOR PAY doit comporter au minimum la référence de demande,
l’identité usager nécessaire, la banque, le RIB/IBAN ou numéro client, la référence
du mandat et une preuve d’intégrité. Le mode d’échange reste à choisir :

- API push TRESOR PAY → SIXPAY, recommandée pour le scénario REST moderne ;
- webhook sécurisé ;
- récupération depuis une sandbox TRESOR PAY ;
- fichier SFTP, uniquement si le scénario sans API est retenu.

Le partenaire reçoit une décision après rejet ou activation et peut consulter le
statut dans son propre périmètre. La représentation canonique ne doit pas exposer
les modèles internes Amplitude.

### 11.4 Éléments obligatoires du futur OpenAPI

- `Authorization`, `X-Correlation-ID` et `Idempotency-Key` pour les mutations ;
- pagination publique stable, tri et filtres autorisés ;
- contrôle d’accès objet ;
- `ProblemDetail` RFC 7807 et codes métier stables ;
- aucun code secret, clé brute ou KYC complet dans les erreurs ;
- opérations internes distinctes des opérations exposées aux partenaires ;
- règles de replay et réponse idempotente ;
- versionnement des schémas et tests de contrat.

Codes d’erreur à arbitrer :

- `CUS-1001` client introuvable ;
- `CUS-1002` KYC non conforme ;
- `CUS-1003` compte inéligible ;
- `CUS-2001` Core Banking indisponible ;
- `SUB-1001` abonnement déjà existant ;
- `SUB-1002` auto-validation interdite ;
- `SUB-1003` partenaire inactif ;
- `SUB-1004` abonnement suspendu ;
- `SUB-1005` validateur déjà enregistré ;
- `SUB-1006` transition invalide ;
- `SUB-1007` vérification Customer refusée ou expirée.

## 12. Décisions structurantes

| ID | Question | Options | Recommandation | Bloquante |
| --- | --- | --- | --- | :---: |
| DEC-CUS-001 | Customer est-il un référentiel local ou une vue du Core Banking ? | A. local maître ; B. référence + snapshot minimal ; C. aucune persistance | **B**, référence Amplitude + preuve minimale | Résolue |
| DEC-CUS-002 | Existe-t-il un enrôlement global distinct ? | A. oui ; B. non, une Subscription par Client–Partner | **B**, imposée par CBF-02 et BR-SUB-002 | Résolue |
| DEC-CUS-003 | Quel module possède le cycle de vie d’abonnement ? | A. Customer ; B. Subscription | **B**, Customer ne possède que la vérification | Résolue |
| DEC-CUS-004 | « matricule » correspond à quel identifiant ? | CustomerNumber, CustomerReference, identifiant fiscal, autre | `CustomerReference` canonique résolue par Amplitude à partir du NIU/RIB | Résolue ; format au contrat |
| DEC-CUS-005 | Quelles données KYC sont stockées ? | complet, snapshot minimal, résultat seulement | NIU, raison sociale, téléphone, e-mail + attributs configurés ; snapshot de décision | Résolue ; rétention à paramétrer |
| DEC-SUB-001 | Quel secret appartient au modèle ? | code secret client, Subscription Key, les deux | Retenir uniquement la `Subscription Key` définie par BR-SUB-004 | Résolue sous réserve du sign-off |
| DEC-SUB-002 | Validation simple/double exacte ? | seuils, catégories, maker-checker | Seuils, catégories et nombre de validations paramétrables ; aucun validateur ne peut être l’initiateur | Résolue |
| DEC-SUB-003 | Cycle de vie canonique ? | catalogues Product, Application ou modèle proposé | Modèle du § 9 | Résolue |
| DEC-SUB-004 | Quand notifier le partenaire ? | création, validation, activation | Après passage effectif à `ACTIVE`, conformément à CBF-02 | Résolue |
| DEC-SUB-005 | Rejet, révocation, expiration et renouvellement sont-ils dans le MVP ? | inclure ou différer | Inclure rejet et suspension/réactivation ; différer expiration/renouvellement/révocation | Résolue |
| DEC-SEC-001 | Mapping des rôles officiels ? | ajouter Agent/Superviseur ou mapper OPS/MANAGER | Mapper `OPS` et `MANAGER`, sans nouveau rôle | Oui |
| DEC-INT-001 | Quel contrat Core Banking est disponible ? | API, SOAP, autre ; payloads et erreurs | REST ; payloads/erreurs au Contract Pack, timeout configurable ultérieurement | Résolue pour l’architecture |
| DEC-API-001 | Approuver les routes, schémas, erreurs et filtres | candidats du § 11 | Contract-first avant contrôleur | Oui |
| DEC-REQ-001 | Créer les User Stories Customer/Subscription manquantes | compléter EPIC 1 ou nouvel EPIC | Nouvel EPIC/lot traçable avec AC Given/When/Then | Oui |
| DEC-NFR-001 | Fixer SLO, volumétrie, rétention et pagination | valeurs à fournir | Mesures explicites dans le brief | Oui |
| DEC-FUN-001 | La banque approuve-t-elle formellement la spécification ? | sign-off ou réserves | Obtenir le sign-off et tracer les réserves | Oui |
| DEC-FUN-002 | Quels seuils déclenchent la double validation ? | montant, catégorie, autre | Paramètres versionnés, audités et figés sur la demande | Résolue ; valeurs configurables |
| DEC-FUN-003 | Quels champs KYC sont obligatoires ? | liste Amplitude | NIU, raison sociale, téléphone, e-mail ; extensions configurables | Résolue |
| DEC-INT-002 | Quel scénario d’interopérabilité est retenu ? | REST, SFTP/Sandbox, mobile | REST pour le MVP ; autres scénarios différés | Résolue |
| DEC-INT-003 | Comment la demande arrive-t-elle dans SIXPAY ? | push, webhook, pull, fichier | API push idempotente ; webhook pour les notifications | Résolue |
| DEC-INT-004 | Où l’agent instruit-il la demande ? | TRESOR PAY ou SIXPAY | Frontend SIXPAY | Résolue |
| DEC-INT-005 | Où conserver le mandat signé ? | TRESOR PAY, GED, SIXPAY | Stockage documentaire SIXPAY ; GED optionnelle future | Résolue |
| DEC-MAP-001 | Que représente « marchand » ? | Partner, banque, service public | Concept TRESOR PAY hors modèle SIXPAY ; les banques sont des institutions financières | Résolue |
| DEC-SEC-002 | Quels identifiants d’autorisation sont retenus ? | références, token, Subscription Key | Token TRESOR PAY + Subscription Key SIXPAY | Résolue ; formats au contrat |
| DEC-PAY-001 | Quel cycle public Payment retenir ? | détaillé ou macro | `PAYMENT_TO_COLLECT` → `RECEIPT_NOTIFIED` | Résolue |
| DEC-DOC-001 | Quel fichier est canonique pour chaque volume ? | conserver doublons ou normaliser | Index versionné des sources d’autorité | Non pour le modèle, oui avant industrialisation |

## 13. Ordre d’implémentation recommandé

Le cahier d’interopérabilité ne justifie pas de remplacer Customer/Subscription par un
autre domaine. Il impose en revanche une étape contractuelle avant le code.

1. **Gate IA-0.5 — Contract Pack**
   - choisir le scénario LA RÉGIONALE BANK ;
   - définir les contrats TRESOR PAY `AuthorizationRequest` et
     `AuthorizationDecision` ;
   - définir le contrat Amplitude de vérification ;
   - approuver le mapping Marchand/Partner et le catalogue des références/tokens ;
   - décider le système d’engagement de l’agent et la GED du mandat.
2. **Customer backend**
   - modèle canonique et port Amplitude ;
   - concordance identité/NIU/référence client/RIB ;
   - KYC et éligibilité du compte ;
   - décision synchrone corrélée, sans référentiel client local.
3. **Subscription backend**
   - réception idempotente de la demande TRESOR PAY via Integration ;
   - instruction par l’agent, validation simple/double, activation/rejet ;
   - Subscription Key, suspension/réactivation, notification et audit.
4. **Golden Subscription Vertical Slice**
   - parcours TRESOR PAY → SIXPAY → Amplitude → validation → TRESOR PAY ;
   - tests contractuels, sécurité, concurrence, idempotence et reprise.
5. **Frontend Customer/Subscription**, si SIXPAY est retenu comme poste de travail
   de l’agent.
6. **Payment**, avec le cycle public `PAYMENT_TO_COLLECT` →
   `RECEIPT_NOTIFIED` ; les étapes techniques intermédiaires restent internes et
   auditables.

## 14. Conditions de passage du Gate IA-0

- [x] Branche, commit et worktree identifiés.
- [x] Sources d’autorité inventoriées.
- [x] Exigences Customer/Subscription consolidées.
- [x] Contradictions documentées.
- [x] Langage métier proposé.
- [x] Agrégats, invariants, états et événements candidats extraits.
- [x] Matrice rôles/actions provisoire reconstruite.
- [x] Contrats API nécessaires inventoriés.
- [x] Brief Customer finalisé en statut `READY_FOR_CONTRACT_PACK`.
- [x] Manifeste Customer finalisé en statut `READY_FOR_CONTRACT_PACK`.
- [x] Frontière Customer/Subscription recalée sur CBF-02 et BR-CUS/BR-SUB.
- [ ] Spécification fonctionnelle signée ou réserves formellement tracées.
- [ ] User Stories et critères d’acceptation Customer approuvés.
- [x] Scénario d’échange TRESOR PAY approuvé.
- [x] Système d’engagement de l’agent et stockage du mandat approuvés.
- [x] Mapping Marchand/Partner et identifiants structurants approuvés.
- [x] Cycle de vie et validation paramétrable approuvés.
- [x] Socle KYC obligatoire approuvé.
- [ ] Contrats REST TRESOR PAY et Amplitude approuvés.
- [ ] Gestion cryptographique détaillée du Token et de la Subscription Key approuvée.
- [ ] Matrice rôles/actions approuvée.
- [ ] OpenAPI et erreurs approuvés.
- [ ] NFR mesurables approuvées.
- [ ] Product, Architecture, Engineering et Security ont donné leur accord.

**Verdict final : `READY_FOR_CONTRACT_PACK`. La conception contractuelle peut
commencer ; la génération backend reste interdite jusqu’au Gate IA-0.5.**
