# SIXPAY CONNECT — Messaging and Outbox

Le module `integration` fournit les adaptateurs de transport des événements
d'intégration. Les contrats indépendants de Spring, Kafka et JPA résident dans
`common`.

## Modes de transport

Un seul transport est actif à la fois.

| Valeur | Usage | État par défaut |
|---|---|---|
| `internal` | Communication entre modules du monolithe modulaire | actif |
| `kafka` | Communication distribuée après extraction en microservices | inactif |

Le choix du transport ne modifie ni les événements métiers ni les Outbox des
modules. Il remplace uniquement l'adaptateur qui implémente
`IntegrationEventTransport`.

## Configuration de référence

```yaml
sixpay:
  messaging:
    transport: internal

    outbox:
      enabled: true
      polling-delay: 1000
      batch-size: 50
      max-attempts: 5
      retry-delay: 30s
      processing-timeout: 5m

    kafka:
      topic-prefix: sixpay
      publish-timeout: 5s
      notification-topic-pattern: 'sixpay\..*\.events\.v[0-9]+'
      notification-group-id: sixpay-notification
```

Les propriétés Kafka standard restent sous `spring.kafka`, par exemple :

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

## Catalogue des propriétés

| Propriété | Défaut | Description |
|---|---:|---|
| `sixpay.messaging.transport` | `internal` | Sélectionne exclusivement `internal` ou `kafka` |
| `sixpay.messaging.outbox.enabled` | `true` | Active le relais Outbox |
| `sixpay.messaging.outbox.polling-delay` | `1000` | Délai en millisecondes entre deux polls |
| `sixpay.messaging.outbox.batch-size` | `50` | Nombre maximal de lignes revendiquées par source |
| `sixpay.messaging.outbox.max-attempts` | `5` | Nombre maximal de tentatives avant `DEAD` |
| `sixpay.messaging.outbox.retry-delay` | `30s` | Délai de base du retry linéaire |
| `sixpay.messaging.outbox.processing-timeout` | `5m` | Délai avant reprise d'un claim interrompu |
| `sixpay.messaging.kafka.topic-prefix` | `sixpay` | Préfixe des topics versionnés |
| `sixpay.messaging.kafka.publish-timeout` | `5s` | Attente maximale de l'accusé Kafka |
| `sixpay.messaging.kafka.notification-topic-pattern` | `sixpay\..*\.events\.v[0-9]+` | Topics consommés par Notification |
| `sixpay.messaging.kafka.notification-group-id` | `sixpay-notification` | Consumer group Notification |

## Garanties

- la création de l'agrégat et de sa ligne Outbox partage la même transaction ;
- chaque source revendique les lignes par lots ;
- PostgreSQL utilise `FOR UPDATE SKIP LOCKED` pour séparer les instances ;
- un message revendiqué passe en `PROCESSING` avant publication ;
- un succès passe en `PUBLISHED` ;
- un échec retryable passe en `FAILED` avec `next_attempt_at` ;
- la dernière tentative passe en `DEAD` ;
- un claim `PROCESSING` abandonné redevient éligible après
  `processing-timeout` ;
- la livraison est **au moins une fois** : les consommateurs doivent être
  idempotents.

Il n'existe aucun double publish fonctionnel entre l'Event Bus interne et
Kafka. Le changement vers Kafka se fait par configuration lors de la migration
vers les microservices.

## Validation

Depuis `backend` :

```bash
mvn clean verify
mvn -Pfull-tests -pl partner,integration,notification,bootstrap -am clean verify
```

La seconde commande nécessite Docker afin d'exécuter PostgreSQL 15 avec
Testcontainers.

Les preuves principales sont :

- `MessagingAutoConfigurationTest` pour la sélection exclusive du transport ;
- `OutboxRelayTest` pour publication, retry et `DEAD` ;
- `PartnerPersistenceIT` pour Flyway et la persistance transactionnelle ;
- `PartnerOutboxConcurrencyIT` pour le claim concurrent PostgreSQL ;
- `NotificationMessagingAutoConfigurationTest` pour les adaptateurs entrants ;
- `MessagingBootstrapConfigurationTest` pour le câblage final.
