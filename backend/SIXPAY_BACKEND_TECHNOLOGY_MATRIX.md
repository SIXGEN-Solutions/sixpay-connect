# SIXPAY CONNECT — Matrice technologique officielle du backend

| Métadonnée | Valeur |
|---|---|
| Statut | **FROZEN — Baseline officielle** |
| Version du document | **1.0** |
| Date de gel | **26 juillet 2026** |
| Périmètre | Backend SIXPAY CONNECT |
| Version du projet | `1.0.0-SNAPSHOT` |
| Autorité de dépendances | `sixpay-bom` |
| Autorité de build | `backend/pom.xml` |

## 1. Objet

Cette matrice constitue le contrat technologique officiel du backend SIXPAY CONNECT.

Elle s'applique :

- aux modules de plateforme `common`, `shared-kernel`, `security` et `integration` ;
- aux modules métier `customer`, `partner`, `payment`, `accounting`, `reporting`, `notification` et `administration` ;
- la capacité de souscription Customer-owned, implémentée dans `customer` ;
- au module exécutable `bootstrap` ;
- aux futurs tests transverses du backend.

Les modules métier doivent être générés et développés à partir de cette baseline, sans redéfinir localement les versions gouvernées par les BOM.

## 2. Baseline principale

| Domaine | Technologie | Version officielle | Gouvernance |
|---|---|---:|---|
| Langage | Java | **21 LTS** | Fixée par le parent Maven |
| Build | Apache Maven | **3.9.11** | Version de référence à utiliser dans Maven Wrapper |
| Build | Plage Maven autorisée | **`[3.9.6,4.0.0)`** | Maven Enforcer |
| Framework applicatif | Spring Boot | **4.1.0** | Import `spring-boot-dependencies` |
| Écosystème distribué | Spring Cloud | **2025.1.2** | Import `spring-cloud-dependencies` |
| Mapping Java | MapStruct | **1.6.3** | Version explicitement gérée par `sixpay-bom` |
| Documentation d'API | Springdoc OpenAPI | **3.0.3** | Version explicitement gérée par `sixpay-bom` |
| Base de données | PostgreSQL Server | **15.x** | Baseline d'infrastructure |
| Migration de schéma | Flyway | **Gérée par Spring Boot 4.1.0** | Aucun override dans les modules |
| Sécurité Spring | Spring Security | **Gérée par Spring Boot 4.1.0** | Aucun override dans les modules |
| Gestion des secrets | Spring Cloud Vault | **Gérée par Spring Cloud 2025.1.2** | Utilisation du starter Spring Cloud |

### Décision Maven 4

Maven 4 est exclu de cette baseline. Le parent importe actuellement `sixpay-bom`, qui appartient au même reactor Maven. Cette topologie doit être revue et validée avant toute migration vers Maven 4.

## 3. Dépendances gouvernées par les BOM

La version des familles suivantes est gelée indirectement par l'import de Spring Boot `4.1.0` ou de Spring Cloud `2025.1.2`.

| Famille | Autorité | Règle |
|---|---|---|
| Spring Framework | Spring Boot BOM | Aucune version dans les POM de modules |
| Spring Security | Spring Boot BOM | Aucune version dans les POM de modules |
| Spring Data et Spring Data JPA | Spring Boot BOM | Aucune version dans les POM de modules |
| Hibernate ORM et Jakarta Persistence | Spring Boot BOM | Aucune version dans les POM de modules |
| Jackson | Spring Boot BOM | Aucune version dans les POM de modules |
| PostgreSQL JDBC | Spring Boot BOM | Aucune version dans les POM de modules |
| Flyway | Spring Boot BOM | Aucune version dans les POM de modules |
| Spring for Apache Kafka et Kafka Client | Spring Boot BOM | Aucune version dans les POM de modules |
| Redis et Spring Data Redis | Spring Boot BOM | Aucune version dans les POM de modules |
| Micrometer | Spring Boot BOM | Aucune version dans les POM de modules |
| OpenTelemetry | Spring Boot BOM | Aucune version dans les POM de modules |
| JUnit Jupiter | Spring Boot BOM | Aucune version dans les POM de modules |
| Mockito | Spring Boot BOM | Aucune version dans les POM de modules |
| Testcontainers | Spring Boot BOM | Aucune version dans les POM de modules |
| Lombok | Spring Boot BOM | Usage optionnel et limité |
| Spring Cloud Vault | Spring Cloud BOM | Aucun override de `spring-vault-core` |

L'absence d'une version explicite dans cette table ne signifie pas que la version est libre : elle est verrouillée par le BOM désigné.

## 4. Matrice fonctionnelle des technologies

### 4.1 API et intégration

| Besoin | Standard officiel | Règle d'utilisation |
|---|---|---|
| API synchrone | REST sur HTTPS | Contrats versionnés et documentés |
| Documentation | OpenAPI 3 avec Springdoc `3.0.3` | Spécification générée et validée |
| Validation | Jakarta Validation | Validation aux frontières de l'application |
| Intégration historique | SOAP | Réservée aux systèmes externes qui l'imposent |
| Événements asynchrones | Apache Kafka | Contrats d'événements versionnés |
| Cache distribué | Redis | Uniquement pour les cas d'usage justifiés |
| Résilience | Timeout, retry borné, idempotence et circuit breaker | Aucun retry infini |
| Fiabilité événementielle | Transactional Outbox | Requis pour la publication liée à une transaction métier |
| Échec asynchrone | Dead-letter topic | Rejeu contrôlé et auditable |

### 4.2 Persistance

| Élément | Décision officielle |
|---|---|
| SGBD | PostgreSQL `15.x` |
| Accès transactionnel | Spring Data JPA / Jakarta Persistence |
| Implémentation ORM | Hibernate, version gérée par Spring Boot |
| Évolution du schéma | Flyway uniquement |
| Validation du schéma | Activée dans les environnements de validation et de production |
| Requêtes de lecture complexes | Modèle de lecture dédié, sans gonfler les repositories d'agrégats |
| Identifiants et contraintes | Contraintes métier également garanties en base |

Les migrations Flyway sont immuables après livraison. Toute correction est introduite par une nouvelle migration.

### 4.3 Sécurité

| Domaine | Baseline officielle |
|---|---|
| Authentification | OAuth 2.0 / OpenID Connect |
| Protection des API | Spring Security Resource Server |
| Jetons | JWT validés cryptographiquement |
| Autorisation | RBAC complété par des règles métier explicites |
| Transport | TLS `1.3` |
| Chiffrement des données sensibles | AES-256 ou mécanisme équivalent validé |
| Secrets | HashiCorp Vault via Spring Cloud Vault, ou service équivalent validé |
| Journalisation | Aucun secret, jeton ou donnée sensible dans les logs |
| Audit | Actions sensibles tracées de manière infalsifiable et corrélable |

Les domaines métier ne dépendent pas directement de Spring Security. Ils reçoivent une représentation métier minimale de l'acteur et de ses droits par l'intermédiaire de ports applicatifs.

### 4.4 Observabilité

| Capacité | Technologie officielle |
|---|---|
| Endpoints opérationnels | Spring Boot Actuator |
| Métriques | Micrometer |
| Export de métriques | Prometheus |
| Tableaux de bord | Grafana |
| Traces distribuées | OpenTelemetry |
| Logs centralisés | ELK ou plateforme compatible |
| Corrélation | Trace ID, Span ID et identifiant de corrélation |

Les données de paiement, secrets, jetons et informations personnelles sensibles doivent être masqués avant émission.

### 4.5 Tests et qualité

| Niveau | Technologie ou règle |
|---|---|
| Tests unitaires | JUnit Jupiter et Mockito, versions gérées par Spring Boot |
| Tests d'intégration | JUnit Jupiter et Testcontainers |
| Base de test | PostgreSQL Testcontainer, pas de substitution H2 pour les tests de persistance |
| Tests Kafka/Redis | Testcontainers lorsque l'intégration réelle est nécessaire |
| Tests d'architecture | Règles automatisées de dépendances entre couches et modules |
| Tests de sécurité | Authentification, autorisation, données sensibles et scénarios négatifs |
| Couverture | JaCoCo `0.8.13` |
| Tests unitaires Maven | Surefire `3.5.6` |
| Tests d'intégration Maven | Failsafe `3.5.6` |
| Scénarios minimaux | Succès, rejet, timeout, doublon, replay et concurrence |

Conventions de nommage :

- tests unitaires : `*Test.java` ou `*Tests.java` ;
- tests d'intégration : `*IT.java` ou `*ITCase.java`.

## 5. Plugins Maven officiels

| Plugin | Version |
|---|---:|
| Maven Clean Plugin | `3.5.0` |
| Maven Resources Plugin | `3.5.0` |
| Maven Compiler Plugin | `3.15.0` |
| Maven Surefire Plugin | `3.5.6` |
| Maven Failsafe Plugin | `3.5.6` |
| Maven JAR Plugin | `3.5.1` |
| Maven Install Plugin | `3.1.4` |
| Maven Deploy Plugin | `3.1.4` |
| Maven Enforcer Plugin | `3.6.3` |
| Spring Boot Maven Plugin | `4.1.0` |
| JaCoCo Maven Plugin | `0.8.13` |

Les versions des plugins sont déclarées uniquement dans le parent Maven.

## 6. Architecture d'exécution et packaging

| Élément | Décision |
|---|---|
| Structure | Maven multi-modules |
| Modules de bibliothèque | JAR standards non exécutables |
| Application exécutable | Module `bootstrap` uniquement |
| Packaging exécutable | Spring Boot layered JAR |
| Configuration | Externalisée par environnement |
| Profils Spring | `dev`, `test`, `prod` |
| Profils Maven | Réservés au comportement du build et des tests |
| Conteneurisation | Docker |
| Orchestration | Kubernetes lorsque le contexte de déploiement le requiert |
| Déploiement Kubernetes | Helm |

Un même artefact doit être promu entre les environnements. La configuration d'environnement ne doit pas provoquer une recompilation du code.

## 7. Règles de découplage obligatoires

1. Le domaine ne dépend ni de Spring, ni de JPA, ni de Kafka, ni de composants HTTP.
2. Les interfaces de repository appartiennent au domaine ; leurs adaptateurs appartiennent à l'infrastructure.
3. Les modules métier ne dépendent pas directement les uns des autres.
4. Les échanges inter-domaines utilisent des contrats explicites, des événements ou des ports.
5. `shared-kernel` ne contient que des concepts métier réellement invariants et partagés.
6. `common` ne contient que des composants techniques transverses sans logique métier.
7. `security` et `integration` exposent des contrats minimaux ; leurs détails techniques ne fuient pas vers le domaine.
8. Les DTO d'API, entités JPA, messages Kafka et objets du domaine sont distincts.
9. Les mappings sont explicites ; MapStruct peut être utilisé aux frontières, jamais pour masquer un couplage de modèles.
10. Lombok n'est pas requis. Les `record`, constructeurs explicites et objets immuables de Java 21 sont privilégiés lorsqu'ils conviennent au modèle.

## 8. Règles de gouvernance des versions

### 8.1 Interdictions

Sont interdits :

- les versions de dépendances directement déclarées dans un module lorsqu'elles sont déjà gérées ;
- les versions `LATEST`, `RELEASE`, dynamiques ou les plages ouvertes ;
- l'import d'un BOM supplémentaire sans validation architecturale ;
- l'override isolé d'une bibliothèque Spring ;
- l'utilisation directe d'un composant interne à la place de son starter officiel ;
- une mise à niveau de dépendance sans tests de compatibilité du reactor complet.

### 8.2 Ordre d'autorité

En cas de divergence, l'ordre d'autorité est :

1. la présente matrice ;
2. `backend/sixpay-bom/pom.xml` ;
3. Spring Boot BOM `4.1.0` ;
4. Spring Cloud BOM `2025.1.2` ;
5. le parent `backend/pom.xml` pour les plugins et le build ;
6. les POM des modules pour la sélection des dépendances, jamais pour leur version.

### 8.3 Procédure d'évolution

Toute évolution de cette baseline exige :

1. une justification et une analyse d'impact ;
2. une décision d'architecture enregistrée ;
3. la mise à jour du BOM ou du parent ;
4. `mvn clean verify` sur l'ensemble du reactor ;
5. l'exécution des tests d'intégration et de sécurité concernés ;
6. l'analyse des dépendances et vulnérabilités ;
7. la mise à jour de cette matrice avec incrément de version.

## 9. Commandes de validation de référence

```bash
./mvnw clean verify
./mvnw clean verify -Pfull-tests
./mvnw clean verify -Pfull-tests,coverage
```

Sous Windows :

```powershell
mvnw.cmd clean verify
mvnw.cmd clean verify -Pfull-tests
mvnw.cmd clean verify -Pfull-tests,coverage
```

Le build lancé depuis un sous-module n'est considéré autonome que si le parent et le BOM SIXPAY sont préalablement installés dans le repository Maven local. La validation officielle reste celle du reactor racine.

## 10. Éléments volontairement non figés dans cette version

La présente version ne fige pas :

- la version Angular et la toolchain frontend ;
- les versions des images Docker ;
- les versions de Kubernetes, Helm, Kafka Broker, Redis Server, Prometheus, Grafana et ELK ;
- les services managés propres à un fournisseur cloud ;
- les outils CI/CD et d'analyse de sécurité.

Ces éléments seront intégrés dans les matrices frontend et infrastructure après leur validation dédiée. Ils ne doivent pas être déduits arbitrairement de la baseline Maven.

## 11. Références officielles

- [Spring Boot 4.1 — System Requirements](https://docs.spring.io/spring-boot/4.1/system-requirements.html)
- [Spring Boot 4.1 — Managed Dependency Coordinates](https://docs.spring.io/spring-boot/4.1/appendix/dependency-versions/coordinates.html)
- [Spring Cloud 2025.1.2 — Release Announcement](https://spring.io/blog/2026/06/11/spring-cloud-2025-1-2-aka-oakwood-has-been-released)
- [Springdoc OpenAPI — Releases](https://github.com/springdoc/springdoc-openapi/releases)
- [MapStruct — Releases](https://github.com/mapstruct/mapstruct/releases)
- [Apache Maven 4 — What's New](https://maven.apache.org/whatsnewinmaven4.html)

## 12. Décision

La baseline **Java 21 / Maven 3.9.11 / Spring Boot 4.1.0 / Spring Cloud 2025.1.2** est désormais la matrice technologique officielle du backend SIXPAY CONNECT.

Elle est obligatoire pour la génération des modules métier. Toute divergence doit être explicitement approuvée et documentée.
