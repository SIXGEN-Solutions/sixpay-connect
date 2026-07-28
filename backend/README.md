# Notification — adaptateur email

Ce lot ajoute uniquement l'adaptateur sortant email du vertical slice
Notification. Il ne modifie pas le module `partner`.

## Modes

- `logging` : mode par défaut, rend le message et journalise uniquement ses
  métadonnées sans contacter un serveur SMTP ;
- `smtp` : utilise le `JavaMailSender` auto-configuré par Spring Boot.

## Fichiers existants à mettre à jour

### `backend/notification/pom.xml`

Ajouter :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

### Auto-configuration imports

Dans :

```text
backend/notification/src/main/resources/META-INF/spring/
org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

placer :

```text
com.sixpay.notification.configuration.NotificationEmailAutoConfiguration
com.sixpay.notification.configuration.NotificationApplicationAutoConfiguration
com.sixpay.notification.configuration.NotificationMessagingAutoConfiguration
```

L'ordre garantit :

```text
PartnerNotificationSender
    -> HandleIntegrationEventUseCase
    -> listener internal ou Kafka
```

### `application-standalone.yml`

Sous `sixpay` :

```yaml
  notification:
    email:
      mode: logging
      from: no-reply@sixpay.local
      subject-prefix: "[SIXPAY]"
```

### Configuration SMTP

Pour activer un serveur SMTP :

```yaml
spring:
  mail:
    host: ${SIXPAY_SMTP_HOST}
    port: ${SIXPAY_SMTP_PORT:587}
    username: ${SIXPAY_SMTP_USERNAME}
    password: ${SIXPAY_SMTP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

sixpay:
  notification:
    email:
      mode: smtp
      from: ${SIXPAY_NOTIFICATION_EMAIL_FROM}
      subject-prefix: "[SIXPAY]"
```

## Non inclus dans ce lot

- persistance et idempotence par `eventId` ;
- retries et état `DEAD` ;
- templates HTML externalisés ;
- modification de `PartnerDecisionNotification` et
  `PartnerDecisionNotificationService`.

La valeur `SUSPENDED` doit déjà exister localement, conformément à la
modification réalisée avant ce lot.
