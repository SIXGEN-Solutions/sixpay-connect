# P3.3 — Raccordement Payment Outbox

Révision source : `1f600f364d822e4a919759e3b4a015749784c858`.

L'adaptateur `PaymentOutboxMessageSource` relie les services de claim et de
complétion Payment au relais générique `OutboxRelay` par injection Spring.
Le relais invoque `claimPending`, publie via `IntegrationEventTransport`, puis
appelle `markPublished`, `markFailed` ou `markDead`.

## Risque ouvert : concurrence avec les callbacks

Lorsque `sixpay.payment.callback.enabled=true`,
`PaymentCallbackOutboxCoordinator.claim()` utilise lui aussi
`PaymentOutboxRepository.lockClaimable()` sans filtrage par type d'événement.
Les deux relais peuvent donc réclamer les mêmes familles d'événements.
**Ne pas activer simultanément les deux relais avant décision et correction
séparée du modèle de diffusion des callbacks.**

## Validation locale (non exécutée)

```powershell
cd backend
mvn -pl payment -am -Dtest=PaymentOutboxMessageSourceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl payment -am test
mvn verify
```

Ce correctif ne prouve pas à lui seul la livraison E2E, l'idempotence T0
ni la configuration effective des transports. Aucun contrat ou schéma modifié.
