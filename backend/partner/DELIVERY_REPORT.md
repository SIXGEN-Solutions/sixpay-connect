# Rapport de livraison — Golden module `partner`

## Périmètre réalisé

- cycle de vie du partenaire : création, validation, rejet, suspension et réactivation ;
- contact technique et périmètre transactionnel obligatoires ;
- seuils monétaires multi-niveaux par type de transaction et devise ;
- consultation sécurisée du statut et des informations non sensibles de connexion ;
- audit append-only, corrélé, horodaté et paginé ;
- historique immuable des seuils ;
- idempotence et sérialisation des commandes concurrentes ;
- événements d'intégration versionnés et transactional outbox ;
- métriques à cardinalité bornée ;
- API REST v1, validation Jakarta, OpenAPI et `ProblemDetail` ;
- mapping JPA explicite, verrouillage optimiste et migration PostgreSQL 15 ;
- tests du domaine, de l'application, de l'API sécurisée, de l'architecture et de PostgreSQL.

## Fichiers

- 68 sources Java de production ;
- 5 sources Java de test ;
- 1 POM de module ;
- 1 migration Flyway ;
- 1 déclaration d'auto-configuration ;
- documentation d'architecture et d'exploitation.

## Décisions d'implémentation

- le POM dépend directement de `common`, `shared-kernel` et `security`, sans version locale ;
- `integration` n'est pas déclaré, car son API publique actuelle ne couvre pas l'outbox ;
- `CorrelationId`, `IdentifierGenerator` et `TimeProvider` remplacent les duplications locales ;
- `AggregateRoot`, `DomainEvent`, `DomainException` et `Money` proviennent de `shared-kernel` ;
- `CurrentUserProvider` et `SixpayRole` alignent le module sur le contrat réel de `security` ;
- le domaine reste sans Spring, JPA, HTTP ou Jackson ;
- les interactions externes passent par des ports applicatifs ;
- `partner` ne dépend d'aucun autre domaine métier ;
- `bootstrap` reste l'unique application exécutable ;
- l'assemblage Spring du module réside dans le package top-level `configuration` ;
- aucune couche métier ou applicative ne dépend du package `configuration` ;
- les collections JPA sont chargées explicitement par `EntityGraph`, sans `EAGER` ;
- les commandes mutantes exigent `Idempotency-Key` ;
- PostgreSQL utilise un verrou consultatif transactionnel pour les replays concurrents ;
- les transitions, l'audit, l'historique, l'idempotence et l'outbox partagent la transaction métier ;
- l'audit et l'historique sont également protégés contre les modifications en base ;
- la devise est explicite pour chaque seuil monétaire ;
- la pagination publique utilise un contrat SIXPAY et n'expose pas `Page`.

## Contrats ajoutés

### API

- `POST /api/v1/partners`
- `POST /api/v1/partners/{id}/validation`
- `POST /api/v1/partners/{id}/suspension`
- `POST /api/v1/partners/{id}/reactivation`
- `PUT /api/v1/partners/{id}/validation-thresholds/{transactionType}`
- `GET /api/v1/partners/{id}`
- `GET /api/v1/partners/{id}/status`
- `GET /api/v1/partners/{id}/audit`

### Événements

- `PartnerCreatedIntegrationEvent` v1 ;
- `PartnerStatusChangedIntegrationEvent` v1 ;
- `PartnerThresholdConfiguredIntegrationEvent` v1.

## Migration

`V2026072601__create_partner_module.sql` crée les tables du modèle d'écriture, des seuils, de l'audit, de l'idempotence et de l'outbox, avec leurs contraintes, index et triggers d'immutabilité.

## Contrôles exécutés dans l'environnement de génération

| Contrôle | Résultat |
|---|---|
| Validité XML du POM | Réussi |
| Gouvernance des versions du POM | Réussi — seule la version du parent est déclarée |
| Imports interdits et couplages inter-domaines | Réussi |
| Absence de `@SpringBootApplication`, `EAGER`, `RestTemplate`, `TODO` et `FIXME` en production | Réussi |
| Compilation Java 21 des sources de production et de test | Réussi |
| Confrontation aux API réelles de `common`, `shared-kernel`, `security` et `integration` | Réussi |
| `mvn -pl partner -am clean verify` | Réussi |
| Tests unitaires du reactor ciblé | Réussi — 59 tests, dont 19 pour `partner` |

La validation a été exécutée avec Maven 3.9.9 et Temurin JDK 21.0.12. L'environnement
interdisant l'attachement dynamique d'agents, l'agent Byte Buddy utilisé par Mockito a été
préchargé dans le fork Surefire ; cela ne modifie pas le code ni le POM livré.

## Contrôles restant à exécuter

Docker n'est pas disponible dans l'environnement de génération. Le profil `full-tests` et
`PartnerPersistenceIT` n'ont donc pas été exécutés. Le reactor complet, au-delà de `partner`
et de ses dépendances amont, reste également à contrôler dans le dépôt de destination.

```bash
mvn clean verify
mvn clean verify -Pfull-tests
mvn clean verify -Pfull-tests,coverage
```

La branche `backend-foundation` ne contient pas encore de Maven Wrapper. Ces commandes
pourront utiliser `./mvnw` dès que le wrapper sera ajouté au repository.

## Risques résiduels

1. la migration et le mapping Hibernate doivent être validés par `PartnerPersistenceIT` sur PostgreSQL 15 ;
2. le mapping mTLS/API key doit alimenter `AuthenticatedUser.subject` avec l'UUID partenaire ;
3. un contrat transverse de publication d'outbox reste à concevoir avant toute dépendance vers `integration` ;
4. le module `notification` doit consommer les changements de statut pour l'email US-02 ;
5. le module `payment` doit consommer la projection de statut et de seuil afin de refuser explicitement les nouvelles transactions d'un partenaire non actif ;
6. chaque critère d'acceptation de l'epic Partenaire doit être relié à un test automatisé ou à une preuve de validation explicite.

## Prochaine étape recommandée

Intégrer le dossier sous `backend/partner`, exécuter la validation ciblée puis le reactor
complet, et effectuer la revue de traçabilité des user stories avant de déclarer le golden
module gelé.
