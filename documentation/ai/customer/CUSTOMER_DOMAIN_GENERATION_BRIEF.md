# SIXPAY CONNECT — Domain Generation Brief Customer

> Ce brief prépare le pilote `customer`. Les décisions structurantes sont approuvées ;
> le brief est prêt pour la production du Contract Pack au Gate IA-0.5.

## 1. Métadonnées

| Champ | Valeur |
| --- | --- |
| Identifiant | `customer-customer-verification-v0.1.0` |
| Domaine | `customer` |
| Capacité | `Customer & Account Verification` |
| Version du brief | `1.0.0-rc1` |
| Statut | `READY_FOR_CONTRACT_PACK` |
| Product Owner | À nommer |
| Architecte | Équipe Architecture SIXPAY CONNECT |
| Tech Lead | Équipe Engineering SIXPAY CONNECT |
| Date d’approbation | Non approuvé |
| Stratégie IA | `AI_GENERATION_STRATEGY.md@1.0.0` |
| Master Prompt | `MASTER_ENGINEERING_PROMPT_V0.md@V0.0` |
| Gate associé | `documentation/ai/customer/GATE_IA_0_CUSTOMER_PREFLIGHT.md` |

## 2. Objectif et valeur métier

- **Problème :** identifier un client bancaire et vérifier son identité, son KYC et
  ses comptes avant toute création d’une demande d’abonnement Client–Partner.
- **Résultat attendu :** fournir une décision de vérification fraîche, minimale,
  auditée et issue du Core Banking.
- **Utilisateurs concernés :** Agent `OPS`, Superviseur `MANAGER`,
  Administrateur, Auditeur.
- **Indicateurs de succès :** à approuver au titre de `DEC-NFR-001`.

## 3. Périmètre

### Inclus

- `CUS-REQ-001` recherche par référence client canonique ;
- `CUS-REQ-002` récupération de l’identité, du KYC et des comptes ;
- `CUS-REQ-003` support de la vérification de conformité par l’agent ;
- `CUS-REQ-004` décision d’éligibilité du client et du compte ;
- normalisation des identifiants entrants NIU, RIB, IBAN/numéro client et
  référence de demande nécessaires à la vérification ;
- audit, corrélation, sécurité, résilience et observabilité associés ;
- contrat public Customer interne ;
- port d’intégration Core Banking et adaptateur ;
- retour synchrone d’une décision canonique à Subscription ;
- persistance minimale de preuve seulement si elle est approuvée ;
- tests unitaires, intégration, contrat, sécurité et architecture.

### Hors périmètre

- création ou modification du client maître dans SIXPAY CONNECT ;
- création et cycle de vie de la Subscription ;
- validation simple/double et seuils associés ;
- génération de la Subscription Key ;
- paiement, comptabilisation et frontend ;
- tout Aggregate Root, module ou Repository `Merchant` ;
- expiration, renouvellement ou révocation d’abonnement ;
- modification de `backend/partner/**`, `backend/subscription/**` ou de la CI.

### Décisions approuvées

- Customer reste administré exclusivement par le Core Banking Amplitude.
- SIXPAY persiste seulement une référence et un snapshot minimal de vérification.
- `OPS` représente l’Agent et `MANAGER` le Superviseur.
- une Subscription active constitue une précondition au paiement.
- la demande d’autorisation est générée par TRESOR PAY et non par Customer.

Le timeout et le SLA Amplitude restent configurables et seront chiffrés avant les
tests de performance et la mise en production.

## 4. Sources d’autorité applicables

| Priorité | Source | Révision | Sections applicables |
| --- | --- | --- | --- |
| 0 | `Cahier des Charges interopérabilité entre TRESOR PAY et le core banking_final_schema_final.docx` | Git `ffd942a...` | §III, création/validation du compte, demande par RIB, scénarios et schémas |
| 0 | `SIXPAY_CONNECT_Specifications_Fonctionnelles.pdf` | Git `ffd942a...` | BP-02, CBF-02, BR-CUS, BR-SUB et §15.1 |
| 1 | `SIXPAY_CONNECT_Product_Blueprint.docx` | Git `ffd942a...` | Customer Domain, Business Information Model, BR-CUS, événements |
| 1 | `SIXPAY_CONNECT_Application_Architecture_Blueprint.docx` | Git `ffd942a...` | Customer services, API, source de vérité, agrégats |
| 1 | `SIXPAY_CONNECT-SDS.docx` | Git `ffd942a...` | queries, DTO, mappers, repositories, errors à arbitrer |
| 1 | `SIXPAY_CONNECT_Technical_Architecture.docx` | Git `ffd942a...` | modules, DDD, Core Banking, IAM, API et NFR |
| 2 | `SIXPAY_CONNECT_CDC.pdf` | Git `ffd942a...` | fonctionnalité #2, sous réserve d’alignement avec la spécification |
| 2 | `SIXPAY_CONNECT_USER_STORIES.docx` | Git `ffd942a...` | sécurité/audit seulement ; stories Customer absentes |
| 3 | Futur OpenAPI Customer | Absent | opérations à approuver |
| 4 | `backend/SIXPAY_BACKEND_TECHNOLOGY_MATRIX.md` | Git `ffd942a...` | intégralité |
| 4 | `backend/SIXPAY_BACKEND_ENGINEERING_GENERATION_CONTRACT.md` | Git `ffd942a...` | intégralité |
| 6 | `backend/partner/**` | Git `ffd942a...` | conventions structurelles uniquement |

## 5. Langage ubiquitaire

| Terme | Définition | Termes interdits/ambigus |
| --- | --- | --- |
| Client bancaire | Client administré par le Core Banking | utilisateur |
| Référence client | Identifiant métier Core Banking à approuver | matricule non défini |
| NIU | Identifiant fiscal fourni par TRESOR PAY, utilisable comme attribut de concordance et non comme clé bancaire sans décision | CustomerReference par défaut |
| Référence de demande | Identifiant de la demande d’autorisation TRESOR PAY | référence de paiement |
| RIB/IBAN | Identifiant bancaire à valider et masquer selon la politique sécurité | identifiant Customer |
| Profil KYC | Informations minimales permettant la décision KYC | dossier local maître |
| Compte client | Compte administré par le Core Banking | wallet |
| Vérification client | Consultation et évaluation identité/KYC/comptes | création client |
| Snapshot de vérification | Résultat horodaté, sourcé et corrélé | copie complète du Core Banking |
| Éligible | Client et compte satisfaisant les contrôles approuvés | actif sans contexte |

## 6. Frontière du domaine

- **Responsabilités :** lookup, normalisation, vérification et preuve d’éligibilité.
- **Données possédées :** aucun référentiel client maître ; éventuellement une preuve
  minimale de vérification, dont le contenu et la rétention restent à approuver.
- **Domaine amont :** Core Banking via un port Customer-owned.
- **Domaines aval :** Subscription et Payment via contrats publics.
- **Interactions interdites :** accès direct aux repositories Partner/Subscription,
  import des agrégats externes, mise à jour du client maître.

## 7. Modèle métier

### Agrégats et Value Objects

| Élément | Type | Identité | Responsabilité |
| --- | --- | --- | --- |
| `CustomerProfile` | Modèle canonique non persistant | `CustomerReference` | vue minimale retournée par Amplitude |
| `CustomerReference` | Value Object | valeur Core Banking | recherche et corrélation métier |
| `CustomerIdentity` | Value Object | sans identité propre | identité minimale |
| `KycProfile` | Value Object extensible | sans identité propre | NIU, raison sociale, téléphone, e-mail, attributs additionnels configurés, statut et source |
| `CustomerAccount` | Modèle canonique | `AccountReference` | compte candidat |
| `CustomerVerification` | Décision métier | `VerificationId` | preuve et résultat horodatés |

Customer ne possède aucun Aggregate Root métier local tant que la persistance d’une
preuve n’est pas approuvée. En conséquence, aucun `CustomerRepository` générique ne
doit être généré par défaut.

### Invariants

| ID | Invariant | Moment de contrôle | Erreur attendue |
| --- | --- | --- | --- |
| CUS-INV-001 | Le client doit exister dans le Core Banking. | lookup | `CUS-1001` |
| CUS-INV-002 | La vérification doit être sourcée, horodatée et corrélée. | verify | erreur interne contrôlée |
| CUS-INV-003 | Le compte doit appartenir au client. | account verification | `CUS-1003` |
| CUS-INV-004 | Le compte doit être actif et sans blocage/opposition. | eligibility | `CUS-1003` |
| CUS-INV-005 | Le KYC doit satisfaire le statut approuvé. | eligibility | `CUS-1002` |
| CUS-INV-006 | Aucun secret ou payload KYC sensible ne doit être journalisé. | toutes frontières | contrôle sécurité |
| CUS-INV-007 | Les identifiants de la demande TRESOR PAY ne sont jamais considérés comme preuve d’identité sans concordance Amplitude. | verify | `CUS-1004` |
| CUS-INV-008 | Le RIB/IBAN de la demande doit correspondre à un compte appartenant au client vérifié. | verify | `CUS-1003` |

### États et transitions

Customer étant administré par le Core Banking, aucun cycle de vie local
`Registered/Updated/Suspended` n’est retenu sans approbation de `DEC-CUS-001`.

Le résultat de vérification candidat est :

| Résultat initial | Commande | Conditions | Résultat final | Fait métier |
| --- | --- | --- | --- | --- |
| — | `VerifyCustomer` | client trouvé | `IDENTIFIED` | `CustomerIdentified` |
| `IDENTIFIED` | contrôles KYC | KYC conforme | `KYC_VERIFIED` | `CustomerKycVerified` |
| `KYC_VERIFIED` | contrôle du compte | compte éligible | `ELIGIBLE` | `CustomerAccountValidated` |
| tout état | contrôle négatif | règle non satisfaite | `INELIGIBLE` | événement d’échec à décider |

## 8. Cas d’usage et critères d’acceptation

| ID | Acteur | Cas d’usage | Préconditions | Résultat |
| --- | --- | --- | --- | --- |
| CUS-UC-001 | OPS/MANAGER | rechercher un client | authentifié et autorisé | vue minimale ou 404 |
| CUS-UC-002 | OPS/MANAGER | consulter ses comptes autorisés | client trouvé | liste filtrée et masquée |
| CUS-UC-003 | OPS/MANAGER | vérifier identité/KYC/compte | contrat Core Banking disponible | décision auditée |
| CUS-UC-004 | AUDITOR | consulter la preuve | mandat et périmètre autorisés | preuve sans secret |

Critères détaillés : **bloqués par `DEC-REQ-001`**. Aucune story Customer approuvée
n’existe encore.

## 9. Sécurité

| Rôle | Action | Portée objet | Autorisé | Preuve attendue |
| --- | --- | --- | --- | --- |
| OPS | recherche/vérification | agence/périmètre à définir | candidat oui | tests 200/403 |
| MANAGER | recherche/vérification | périmètre à définir | candidat oui | tests 200/403 |
| AUDITOR | preuve de vérification | dossier audité | candidat lecture | tests masquage |
| ADMIN | consultation | besoin à confirmer | candidat lecture | test dédié |
| PARTNER | Customer API interne | aucun | non | 403 audité |
| SUPPORT | données KYC | aucun par défaut | non | 403 audité |

- **Classification :** `RESTRICTED`.
- **Masquage/rétention :** à approuver via `DEC-CUS-005`.
- **Audit :** acteur, action, référence masquée, résultat, temps, corrélation et source.
- **Interdiction :** aucun secret, document d’identité complet ou payload bancaire dans
  logs, événements ou erreurs.

## 10. Contrat API

| Méthode | Route candidate | Operation ID | Rôle | Succès | Erreurs |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/v1/customers` | `searchCustomers` | OPS/MANAGER | `200 CustomerSummaryPage` | 400/401/403/502/503 |
| GET | `/api/v1/customers/{customerReference}` | `getCustomer` | OPS/MANAGER | `200 CustomerDetails` | 401/403/404 |
| GET | `/api/v1/customers/{customerReference}/accounts` | `getCustomerAccounts` | OPS/MANAGER | `200 AccountSummaryPage` | 401/403/404/502 |
| POST | `/api/v1/customer-verifications` | `verifyCustomer` | Subscription interne/OPS | `201 CustomerVerification` | 400/401/403/409/422/502/503 |

- **Fichier OpenAPI prévu :**
  `backend/customer/src/main/resources/openapi/customer-api-v1.yaml`.
- **Statut :** absent et non approuvé.
- **Idempotency-Key :** obligatoire pour `verifyCustomer`, à confirmer.
- **Pagination/filtres :** à définir dans `DEC-API-001`.

## 11. Événements et intégrations

| Direction | Contrat candidat | Version | Producteur | Consommateur | Garantie |
| --- | --- | --- | --- | --- | --- |
| IN | Core Banking Customer Lookup | v1 à créer | Core Banking Adapter | Customer | timeout/retry borné |
| OUT synchrone | `CustomerVerificationResult` | v1 à approuver | Customer | Subscription | réponse interne corrélée |
| OUT optionnel | `CustomerEligibilityVerifiedIntegrationEvent` | v1 à approuver | Customer | Audit/Reporting | seulement si un consommateur est confirmé |

- **Outbox/retry/DLQ :** non requis pour la réponse synchrone ; requis uniquement pour
  un événement optionnel approuvé.
- **Corrélation :** obligatoire de l’API au Core Banking puis à l’Outbox.
- **Système simulé :** Core Banking uniquement dans les tests ; aucune donnée réelle.

## 12. Persistance et migrations

- **Tables candidates :** aucune table Customer par défaut ; éventuellement
  `customer_verifications` et `customer_audit_log` si la preuve locale est approuvée.
- **Décision de stockage :** bloquée par `DEC-CUS-001` et `DEC-CUS-005`.
- **Contraintes :** pas de réplication du référentiel Amplitude ; audit append-only.
- **Migration prévue :**
  `backend/customer/src/main/resources/db/migration/V<timestamp>__create_customer_module.sql`.
- **Volumétrie/rétention/rollback :** à définir.

## 13. Frontend

Hors périmètre de cette campagne V0. Le frontend sera traité après validation du backend
et du processus pilote, avec `MASTER_ENGINEERING_PROMPT_V1.md`.

## 14. Exigences non fonctionnelles

| Catégorie | Exigence | Preuve |
| --- | --- | --- |
| Performance | cible Core Banking et API à approuver | test de charge/SLO |
| Résilience | timeout et retry borné ; aucune transaction DB autour de l’appel | IT |
| Observabilité | métriques résultat/latence sans PII ; logs corrélés | tests + dashboard |
| Sécurité | 401/403, accès objet, masquage, audit | tests sécurité |
| Disponibilité | comportement en panne Core Banking à approuver | IT |

## 15. Plan de tests et traçabilité

| Exigence | Niveau | Preuve prévue |
| --- | --- | --- |
| CUS-INV-001/005 | unité domaine/application | `CustomerEligibilityTest` |
| CUS-UC-001/003 | API/application | `CustomerControllerTest` |
| contrat Core Banking | intégration | WireMock ou serveur simulé approuvé |
| persistance/audit/outbox | PostgreSQL Testcontainers | `CustomerPersistenceIT` |
| OpenAPI/RFC 7807 | contrat | `CustomerApiContractTest` |
| frontières module | architecture | `CustomerArchitectureTest` |
| RBAC/objet/PII | sécurité | scénarios positifs et négatifs |

Les noms sont indicatifs jusqu’à approbation des critères.

## 16. Périmètre de fichiers

### Autorisés après approbation

- `backend/customer/**`
- `backend/bootstrap/**` uniquement pour assemblage explicitement approuvé
- `backend/tests/**` uniquement pour E2E approuvé
- `documentation/ai/customer/**`

### Interdits

- `backend/partner/**`
- `backend/subscription/**`
- `frontend/**`
- `.github/**`
- modifications des matrices, prompts, BOM ou contrats gelés

## 17. Gates et commandes

| Gate | Critère | Preuve |
| --- | --- | --- |
| IA-0 | modèle et décisions structurantes approuvés | `READY_FOR_CONTRACT_PACK` |
| IA-0.5 | contrats TRESOR PAY, Subscription et Amplitude approuvés | requis avant génération |
| IA-1 | domaine et invariants | tests unitaires ciblés |
| IA-2 | application/ports | tests application |
| IA-3 | API/OpenAPI/sécurité | tests contrat et sécurité |
| IA-4 | persistance/Outbox | Testcontainers |
| IA-5 | intégration Core Banking | tests timeout/retry/erreurs |
| IA-6 | module complet | `mvn ... verify -pl customer -am` |
| IA-7 | reactor/CI | workflow `Backend CI` |

## 18. Décisions ouvertes

Les décisions de modèle, scénario REST, API push, frontend SIXPAY, stockage du
mandat, KYC extensible, validation paramétrable, Token et Subscription Key sont
résolues au § 12 du Gate.

Restent à produire ou approuver avant génération :

- `DEC-API-001` : OpenAPI et webhooks ;
- `DEC-REQ-001` : critères d’acceptation ;
- `DEC-SEC-001` : matrice des rôles ;
- `DEC-NFR-001` : valeurs mesurables ;
- les formats, erreurs et politiques cryptographiques du Contract Pack.

## 19. Definition of Ready

- [x] Périmètre technique borné au Customer backend.
- [x] Responsabilités Customer proposées.
- [x] Chemins autorisés et interdits proposés.
- [ ] Sources canoniques et versions approuvées.
- [ ] Spécification fonctionnelle signée ou réserves formellement acceptées.
- [ ] User Stories et critères Customer approuvés.
- [ ] Contrat Core Banking approuvé.
- [ ] Contrat canonique de demande TRESOR PAY approuvé.
- [ ] Champs KYC obligatoires approuvés par Métier/Conformité.
- [ ] Modèle de persistance et rétention approuvés.
- [ ] Matrice rôles/actions approuvée.
- [ ] OpenAPI/erreurs/événements approuvés.
- [ ] NFR mesurables approuvées.
- [x] Décisions structurantes du modèle résolues.
- [ ] Contract Pack approuvé.
- [ ] Product, Architecture, Engineering et Security ont approuvé.

## 20. Approbations

| Autorité | Décision | Date | Référence |
| --- | --- | --- | --- |
| Product | PENDING | — | — |
| Architecture | PENDING | — | — |
| Engineering | PENDING | — | — |
| Security | PENDING | — | — |
