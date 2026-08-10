# SIXPAY CONNECT — Notification Golden Test Coverage

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.6 — Notification
```

## Correction après exécution

L'exécution de `NotificationDeliveryPersistenceIT` a révélé que le module
Notification possède deux ensembles fonctionnels distincts :

```text
1. Partner decision notification
2. Operational notification
```

Le second ensemble contient notamment :

```text
com.sixpay.notification.domain.*
OperationalNotificationPersistenceAutoConfiguration
OperationalNotificationApplicationAutoConfiguration
OperationalNotificationOperationsAutoConfiguration
OperationalNotificationRetryAutoConfiguration
OperationalNotificationEmailAutoConfiguration
```

La classification initiale `Domain = N/A` était donc incorrecte.

## Incident de test observé

`NotificationDeliveryPersistenceIT` cible uniquement la persistance du flux
Partner notification :

```text
NotificationDeliveryStore
JpaNotificationDeliveryStore
NotificationDeliverySpringDataRepository
```

Cependant Spring Boot chargeait également les auto-configurations Operational
déclarées dans `AutoConfiguration.imports`.

`OperationalNotificationPersistenceAutoConfiguration` crée
`NotificationTemplateVariablesCodec`, qui nécessite un
`tools.jackson.databind.ObjectMapper`.

Le contexte spécifique du test legacy ne fournit pas ce bean.

Le test ne doit pas élargir artificiellement son scope en ajoutant un
`ObjectMapper`; il doit rester focalisé sur la persistance qu'il valide.

## Correction

Le `TestApplication` exclut explicitement les auto-configurations hors scope et
importe uniquement :

```text
NotificationPersistenceAutoConfiguration
```

Cela respecte la règle golden :

```text
un test = une responsabilité claire
```

## Statut 8.2.6 après cette découverte

```text
Partner notification
  Application     COVERED
  Infrastructure  COVERED

Operational notification
  Domain          TO_VERIFY
  Application     TO_VERIFY
  Infrastructure  TO_VERIFY

API               TO_VERIFY / N/A selon implémentation réelle
```

Le lot 8.2.6 ne doit pas encore être déclaré CLOSED avant l'audit complet du
sous-système Operational Notification.

## Validation immédiate

```bash
mvn -pl notification     -Dtest=NotificationDeliveryPersistenceIT     test
```

Puis :

```bash
mvn -pl notification -am test
```
