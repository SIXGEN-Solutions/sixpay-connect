# Notification

Le module Notification contient actuellement deux périmètres :

```text
Partner decision notification
Operational notification
```

## Partner decision notification

Le flux Partner reste découplé du module `partner` et reçoit les événements via
les adapters messaging du module.

## Operational notification

Le repository contient également un sous-système Operational Notification avec
une couche domaine, des repositories, des politiques, une persistance, des
opérations, un retry et un adapter email.

Ce périmètre doit être inclus dans la matrice Phase 8.2.6.

## Phase 8.2.6

`NotificationDeliveryPersistenceIT` est volontairement isolé sur :

```text
NotificationPersistenceAutoConfiguration
```

afin de ne pas charger les auto-configurations Operational hors de son scope.

Le statut détaillé est documenté dans :

```text
NOTIFICATION-TEST-COVERAGE.md
```
