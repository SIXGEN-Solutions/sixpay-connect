# Traçabilité d’acceptation — Golden Module Partner

Cette matrice s’applique à l’état GitHub `fe3ec87` complété par le lot
`step-2-golden-partner`. Elle distingue la responsabilité du module `partner`
des contrôles contractuels imposés aux domaines consommateurs.

| US | Critère | Preuve automatisée |
|---|---|---|
| US-01 | identité, contact technique et périmètre obligatoires | `PartnerControllerTest.rejectsInvalidCreateRequestWithProblemDetail` |
| US-01 | statut initial `PENDING_VALIDATION` | `PartnerTest.createsPartnerPendingValidationAndRaisesEvent` |
| US-01 | création horodatée et auditée | `PartnerApplicationServiceTest.createsThenApprovesPartnerAtomicallyThroughPorts` et `queriesAuditByPartnerPeriodWithPublicPaginationContract` |
| US-02 | approbation vers `ACTIVE` | `PartnerApplicationServiceTest.createsThenApprovesPartnerAtomicallyThroughPorts` |
| US-02 | rejet vers `REJECTED` avec motif obligatoire | `PartnerTest.rejectsPendingPartnerOnlyWithAReason` et `PartnerApplicationServiceTest.rejectsPartnerOnlyWithReasonAndPublishesSelfContainedEvent` |
| US-02 | notification du contact après approbation ou rejet | `PartnerDecisionNotificationServiceTest.sendsApprovalToTechnicalContact` et `sendsRejectionWithReasonToTechnicalContact` |
| US-03 | seuil par partenaire, type et devise avec plusieurs niveaux | `PartnerTest.configuresThresholdOnlyInsideAuthorizedPerimeter` et `PartnerApplicationServiceTest.configuresThresholdAndKeepsHistoryWithActorAndCorrelation` |
| US-03 | historique immuable des seuils | `PartnerPersistenceIT.persistsEveryMutationThroughOutboxAndKeepsImmutableHistory` |
| US-03 | déclenchement multi-validation au dépassement | `PartnerTest.requiresMultipleValidationLevelsOnlyAboveConfiguredThreshold` |
| US-04 | suspension motivée et réactivation autorisée | `PartnerTest.suspendsAndReactivatesOnlyAnActivePartner` et `PartnerApplicationServiceTest.suspendsAndReactivatesWithAuditAndOutboxEvents` |
| US-04 | blocage explicite des nouvelles transactions | `PartnerTest.acceptsNewTransactionsOnlyWhileActive` et `PartnerConnectionInfoResponseTest.explicitlyBlocksNewTransactionsWhenPartnerIsNotActive`; le domaine transactionnel doit consommer `PartnerStatusChangedIntegrationEvent` |
| US-04 | suspension auditée | `PartnerApplicationServiceTest.suspendsAndReactivatesWithAuditAndOutboxEvents` |
| US-05 | statut actuel et méthodes mTLS/API key sans secret | `PartnerConnectionInfoResponseTest.exposesSupportedConnectionMethodsWithoutSecrets` |
| US-05 | accès propriétaire et rôles internes | `PartnerControllerTest.letsPartnerReadOnlyItsOwnStatus`, `forbidsPartnerFromReadingAnotherPartnerStatus` et tests RBAC |
| US-05 | réponse sous deux secondes | garde composant `PartnerControllerTest.statusEndpointRespondsWithinTwoSecondsAtComponentBoundary`; le SLO reste à confirmer sur l’application déployée avec PostgreSQL |
| US-06 | audit avec date, heure et auteur | `PartnerApplicationServiceTest.queriesAuditByPartnerPeriodWithPublicPaginationContract` |
| US-06 | journal non modifiable | `PartnerPersistenceIT.persistsEveryMutationThroughOutboxAndKeepsImmutableHistory` |
| US-06 | consultation par dossier et période | `PartnerApplicationServiceTest.queriesAuditByPartnerPeriodWithPublicPaginationContract` |
| transverse | idempotence sans double effet | `PartnerApplicationServiceTest.replaysCreateWithoutRepeatingSideEffects` |
| transverse | toutes les mutations publient par l’Outbox | `PartnerPersistenceIT.persistsEveryMutationThroughOutboxAndKeepsImmutableHistory` |
| transverse | aucun couplage `partner` vers `notification` | `PartnerArchitectureTest.partnerDoesNotDependOnAnotherBusinessDomain` |
| transverse | OpenAPI sur tous les endpoints | `PartnerOpenApiContractTest.documentsEveryPublicEndpointAndItsSecurityScheme` |
| transverse | métriques à cardinalité bornée | `MicrometerPartnerOperationMetricsTest.recordsBoundedOperationAndOutcomeTags` |

## Limites de responsabilité

- `partner` expose la décision de validation, le contact destinataire et les
  niveaux requis. Il ne doit jamais appeler `notification` ou `payment`.
- `notification` consomme la version 2 de
  `PartnerStatusChangedIntegrationEvent` et appelle
  `PartnerNotificationSender`. L’adaptateur d’envoi concret sera fourni par
  l’infrastructure de notification.
- Le refus effectif d’une opération pour un partenaire suspendu et le
  déclenchement d’un workflow multi-validation sont des critères
  d’intégration à reproduire dans le domaine transactionnel consommateur.
- Le seuil de deux secondes est un SLO d’environnement. Il ne doit pas être
  simulé par un test unitaire non représentatif.
