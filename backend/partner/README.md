# SIXPAY CONNECT — Golden Partner Module

Le module `partner` est le module métier de référence de SIXPAY CONNECT. Il matérialise les conventions d'architecture, de sécurité, de persistance, d'événements et de tests qui devront être reproduites dans les autres domaines.

## Baseline

- Java 21 ;
- Spring Boot 4.1.0 ;
- Spring Cloud 2025.1.2 ;
- Maven multi-modules ;
- PostgreSQL 15 ;
- Flyway ;
- Spring MVC, Validation, Security et Data JPA ;
- Jackson 3 ;
- JUnit 5 et Testcontainers 2.

Toutes les versions restent gouvernées par `sixpay-bom`. Le POM du module ne fixe aucune version et n'applique pas le plugin de reconditionnement Spring Boot : seul `bootstrap` produit l'application exécutable.

## Réutilisation des modules de plateforme

| Module | Contrats réutilisés | Dépendance Maven |
|---|---|---|
| `common` | `CorrelationId`, `IdentifierGenerator`, `UuidIdentifierGenerator`, `TimeProvider`, `SystemTimeProvider` | directe |
| `shared-kernel` | `AggregateRoot`, `DomainEvent`, `DomainException`, `Money` | directe |
| `security` | `CurrentUserProvider`, `AuthenticatedUser`, `SixpayRole` et auto-configuration JWT | directe |
| `integration` | Aucun contrat applicable au flux d'outbox actuel ; le module expose aujourd'hui des clients REST externes | absente |

Une dépendance n'est déclarée que lorsque son API publique est consommée. En particulier,
`partner` ne dépend pas artificiellement de `integration` : son outbox est un stockage local
au domaine et aucun appel HTTP externe n'est effectué par le module.

## Périmètre fonctionnel

| Story | Capacité matérialisée |
|---|---|
| US-01 | Création avec identité, contact technique et périmètre obligatoires ; statut initial `PENDING_VALIDATION` ; audit et événement |
| US-02 | Approbation ou rejet ; motif obligatoire au rejet ; événement d'outbox pour notification |
| US-03 | Seuil monétaire par partenaire, type de transaction et devise ; nombre de niveaux ; historique immuable |
| US-04 | Suspension motivée et réactivation ; changement d'état audité et publié |
| US-05 | Consultation du statut courant et des informations non sensibles de connexion par un partenaire authentifié ou un rôle interne |
| US-06 | Journal append-only interrogeable par partenaire et période |

## Architecture

```text
com/sixpay/partner/
├── api/              -> exposition REST et validation HTTP
├── application/      -> cas d'usage et ports
├── domain/           -> agrégat, invariants et repository
├── infrastructure/   -> adaptateurs techniques
├── configuration/    -> assemblage et auto-configuration du module
├── events/           -> contrats d'intégration versionnés
└── PartnerModule.java
```

Le domaine expose un agrégat `Partner` sans setters génériques. Les seules transitions valides sont :

```text
PENDING_VALIDATION -> ACTIVE
PENDING_VALIDATION -> REJECTED
ACTIVE             -> SUSPENDED
SUSPENDED          -> ACTIVE
```

Toute transition invalide lève `PartnerDomainException`. Les décisions, suspensions, réactivations et changements de seuil sont enregistrés dans la même transaction que l'agrégat, l'audit et l'outbox.

## API

Base path : `/api/v1/partners`

| Méthode | URI | Rôle |
|---|---|---|
| `POST` | `/` | `ADMIN` |
| `POST` | `/{id}/validation` | `MANAGER` |
| `POST` | `/{id}/suspension` | `ADMIN` |
| `POST` | `/{id}/reactivation` | `ADMIN` |
| `PUT` | `/{id}/validation-thresholds/{type}` | `ADMIN` |
| `GET` | `/{id}` | `ADMIN`, `MANAGER` ou `AUDITOR` |
| `GET` | `/{id}/status` | propriétaire `PARTNER` ou rôle interne |
| `GET` | `/{id}/audit?from=...&to=...&page=0&size=50` | `AUDITOR` |

Les erreurs HTTP suivent `ProblemDetail` (RFC 9457). Les requêtes mutantes acceptent `X-Correlation-ID`; une valeur est générée lorsqu'elle est absente. Elles exigent également `Idempotency-Key`. Un verrou transactionnel PostgreSQL et la table `partner_idempotency` empêchent les doubles effets, y compris lors de requêtes concurrentes ou rejouées.

## Contrat avec le module Security

`partner` ne valide pas lui-même les certificats, JWT ou clés API. Il consomme directement
`CurrentUserProvider`, `AuthenticatedUser` et `SixpayRole` fournis par le module `security`.
La plateforme doit :

1. authentifier les collaborateurs par OAuth2/JWT et fournir les rôles attendus ;
2. authentifier les partenaires par mTLS ou clé API ;
3. attribuer `ROLE_PARTNER` ;
4. utiliser l'UUID du partenaire comme sujet authentifié (`AuthenticatedUser.subject`) ;
5. activer globalement la sécurité de méthode Spring (`@EnableMethodSecurity`).

`PartnerAccessPolicy` empêche ainsi un partenaire de consulter le statut d'un autre dossier. Aucun secret n'est stocké par ce module.

La correspondance avec les personas fonctionnels de l'epic est :

| Persona fonctionnel | Rôle plateforme |
|---|---|
| Administrateur fonctionnel | `ADMIN` |
| Gestionnaire | `MANAGER` |
| RSSI / auditeur | `AUDITOR` |
| Partenaire externe | `PARTNER` |

## Contrats d'intégration

Les événements suivants sont versionnés avec `schemaVersion = 1` :

- `PartnerCreatedIntegrationEvent` ;
- `PartnerStatusChangedIntegrationEvent` ;
- `PartnerThresholdConfiguredIntegrationEvent`.

Ils sont écrits dans `partner_outbox_events` au sein de la transaction métier. Le module
`integration` présent dans la baseline expose actuellement des briques de clients REST
(`RestClientFactory`, `IntegrationContext`, erreurs et en-têtes techniques), mais aucun
contrat de publication d'outbox. Il n'est donc pas importé par `partner`. Un worker
transverse devra être ajouté explicitement avant la publication Kafka des lignes `PENDING`.
Le module `notification` pourra ensuite consommer les changements vers `ACTIVE` ou
`REJECTED` pour envoyer l'email prévu par US-02. Le module `payment` pourra maintenir une
projection du statut courant et refuser toute nouvelle transaction lorsque le partenaire
n'est pas `ACTIVE`.

Ce flux évite toute dépendance directe de `partner` vers `notification` ou `payment`.

Lorsqu'un contrat transverse d'outbox sera officiellement exposé par `integration`, une
évolution d'architecture décidera si l'adapter local doit l'implémenter. Aucune dépendance
future n'est anticipée dans le POM.

## Persistance

La migration `V2026072601__create_partner_module.sql` crée :

- `partners` ;
- `partner_authorized_perimeters` ;
- `partner_validation_thresholds` ;
- `partner_validation_threshold_history` ;
- `partner_audit` ;
- `partner_idempotency` ;
- `partner_outbox_events`.

Des contraintes PostgreSQL protègent les statuts, les motifs, les montants et les niveaux de validation. Des triggers interdisent toute mise à jour ou suppression de l'audit et de l'historique des seuils.

## Intégration

Copier ce répertoire sous `backend/partner`. Le parent contient déjà `<module>partner</module>`. Le fichier d'imports d'auto-configuration permet à `bootstrap` de charger le module sans ajouter de `@SpringBootApplication`.

L'auto-configuration est déclarée dans le package top-level
`com.sixpay.partner.configuration`. Les autres couches ne dépendent jamais de ce package ;
il constitue la racine d'assemblage technique du module.

La configuration d'exécution de `bootstrap` doit conserver `spring.jpa.open-in-view=false`.

Commandes de contrôle depuis `backend` :

```bash
mvn -pl partner -am clean verify
mvn -pl partner -am -Pfull-tests clean verify
mvn -pl partner -am -Pcoverage clean verify
```

La seconde commande exécute `PartnerPersistenceIT` avec PostgreSQL 15 via Testcontainers et nécessite Docker.
La branche `backend-foundation` ne contient actuellement pas de Maven Wrapper ; les mêmes
commandes pourront utiliser `./mvnw` dès qu'il sera versionné.

## Validation des user stories

Le golden module ne doit pas être accepté sur la seule base d'un build vert. Les critères
d'acceptation de l'epic Partenaire doivent être reliés à des tests automatisés :

| Story | Preuve principale |
|---|---|
| US-01 | `PartnerTest`, `PartnerApplicationServiceTest`, `PartnerControllerTest`, `PartnerPersistenceIT` |
| US-02 | transitions et rejets dans `PartnerTest`, événement et audit dans `PartnerApplicationServiceTest` |
| US-03 | règles de seuil dans `PartnerTest`, persistance et historique dans `PartnerPersistenceIT` |
| US-04 | transitions de suspension/réactivation dans `PartnerTest` et tests de service |
| US-05 | contrôle RBAC et accès propriétaire dans `PartnerControllerTest` |
| US-06 | requête paginée et bornes temporelles dans les tests de service et de persistance |

La revue fonctionnelle doit vérifier chaque critère d'acceptation, y compris les scénarios
négatifs, de doublon, de replay et de concurrence. La matrice ci-dessus indique les points
d'ancrage ; elle ne remplace pas la traçabilité détaillée de l'epic.

## Règles de duplication

Pour créer un nouveau module métier à partir de ce golden module :

- conserver la direction des dépendances ;
- remplacer le modèle et les règles métier, pas les responsabilités des couches ;
- conserver les interfaces de sortie pour toute interaction externe ;
- publier via outbox et jamais directement vers un autre domaine ;
- conserver les contrôles RBAC et les politiques d'accès objet ;
- créer une migration Flyway propre au domaine ;
- fournir au minimum des tests du domaine, de l'application, de l'API sécurisée et de la persistance PostgreSQL.
