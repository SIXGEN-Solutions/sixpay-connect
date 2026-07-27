# Architecture Decision Record — Partner Golden Module

## Décision

Le module `partner` adopte une architecture hexagonale interne organisée autour d'un agrégat DDD, avec persistance transactionnelle de l'agrégat, de l'audit, de l'historique et des événements d'outbox.

## Invariants

1. Le package `domain` ne dépend d'aucun framework.
2. Les cas d'usage dépendent de ports, jamais d'adapters.
3. Les contrôleurs ne manipulent ni entités JPA ni repositories.
4. Les adapters JPA traduisent explicitement le modèle de domaine.
5. Une opération métier et ses traces sont atomiques.
6. Aucun domaine métier externe n'est importé.
7. La sécurité d'objet complète le RBAC pour les accès partenaire.
8. Toute évolution de contrat d'événement crée une nouvelle version de schéma.
9. Le package top-level `configuration` assemble le module ; aucune autre couche n'en dépend.
10. Un contrat déjà fourni par `common`, `security` ou `integration` n'est pas redéfini localement.

## Choix structurants

### Agrégat riche

Les transitions sont des méthodes métier (`approve`, `reject`, `suspend`, `reactivate`). Un statut ne peut pas être modifié arbitrairement.

### Audit append-only

L'immutabilité ne repose pas seulement sur l'application : PostgreSQL bloque également les `UPDATE` et `DELETE` sur les tables d'historique.

### Outbox locale au domaine

Le module écrit une outbox locale et ne contacte aucun broker dans la transaction. Le composant de publication transverse peut traiter cette table sans introduire une dépendance métier.

### Auto-configuration

Le module reste un JAR non exécutable et s'enregistre par le mécanisme d'auto-configuration Spring Boot 4. La classe `PartnerModuleConfiguration` réside dans le package top-level `configuration`. `bootstrap` demeure l'unique point d'entrée.

### Contrats transverses

`partner` dépend directement de `common`, `shared-kernel` et `security`, car il consomme
leurs contrats publics :

- `common` : corrélation, génération d'identifiants et temps testable ;
- `shared-kernel` : agrégat, événement de domaine, exception métier et monnaie ;
- `security` : utilisateur courant et rôles officiels.

`integration` n'est pas déclaré : son code actuel concerne les appels REST vers des systèmes
externes et n'expose aucun contrat d'outbox. Le stockage d'outbox demeure donc propre au
domaine, sans simuler une capacité transverse inexistante.

## Conséquences

- Le module est testable sans Spring pour le domaine et sans base pour l'application.
- Une panne du broker n'annule pas une décision métier validée.
- Les consommateurs peuvent évoluer indépendamment.
- La publication, le nettoyage et la rétention de l'outbox exigent encore un composant transverse explicitement conçu et validé.
