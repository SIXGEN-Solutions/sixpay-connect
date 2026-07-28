# Notification — traitement des décisions Partner

Le module reçoit la même `IntegrationEventEnvelope` depuis l’Internal Bus ou
Kafka. Les deux adaptateurs délèguent à `HandleIntegrationEventUseCase`.

## Flux pris en charge

`PartnerDecisionNotificationService` traite exclusivement :

- `aggregateType = PARTNER` ;
- `eventType = PartnerStatusChangedIntegrationEvent` ;
- `schemaVersion = 2` ;
- `currentStatus = ACTIVE` ou `REJECTED`.

Le payload est décodé dans un modèle local au module `notification`. Aucune
dépendance Maven ou Java vers `partner` n’est introduite.

L’adresse du destinataire est fournie par `recipientEmail` dans le contrat
d’intégration v2. Cela évite un appel synchrone vers `partner` et rend
l’événement autonome sur l’Internal Bus comme sur Kafka.

## Port d’envoi

Le traitement construit un `PartnerDecisionNotification` puis appelle
`PartnerNotificationSender`. L’adaptateur concret doit :

- envoyer le message au canal retenu ;
- utiliser `eventId` comme clé d’idempotence ;
- ne pas journaliser l’adresse complète ni le contenu sensible ;
- propager `correlationId` dans ses logs et métriques ;
- lever une exception en cas d’échec afin que la stratégie de retry du
  consommateur puisse s’appliquer.

L’auto-configuration ne crée le cas d’usage et les listeners que lorsqu’un
bean `PartnerNotificationSender` est fourni. Elle ne remplace jamais un envoi
réel par un faux succès.

## Tests

- `PartnerDecisionNotificationServiceTest` couvre approbation, rejet,
  filtrage et validation de version ;
- `JacksonPartnerStatusChangedEventDecoderTest` couvre le contrat JSON ;
- les tests des listeners prouvent que les modes Internal Bus et Kafka
  conduisent vers le même port ;
- `NotificationApplicationAutoConfigurationTest` garantit l’absence de faux
  traitement lorsqu’aucun adaptateur d’envoi n’est installé.
