# Correctif Payment Confirmation — activation déterministe des beans

Révision de référence : `cf51030b0674d9630bb4899b939a796ff3d0cc5f`.

## Symptôme

Après vérification bancaire `VERIFIED`, l'initiation échoue avec
`Payment Confirmation bridge unavailable`. Le rapport des conditions Spring
montre `PaymentConfirmationService` exclu parce que `PaymentConfirmationGateway`
n'était pas encore enregistré au moment du scan.

## Cause et correction

`@ConditionalOnBean` sur quatre stéréotypes découverts par component scanning
pouvait être évalué avant le `@Bean` fourni par la configuration Amplitude.
Le correctif utilise **le flag existant**
`sixpay.payment.banking.amplitude.confirmation.enabled` pour activer ensemble :

- `PaymentConfirmationService`
- `PaymentConfirmationRevocationService`
- `PaymentPreConfirmationTerminationService`
- `PaymentConfirmationController`

L'injection constructeur des dépendances reste stricte : lorsque la capacité est
activée sans gateway, le démarrage doit échouer plutôt que laisser le serveur
accepter des demandes qui échoueront en cours de paiement.

## Limites et compatibilité

- Ce correctif est limité au connecteur Amplitude actuellement activé. Si
  plusieurs implémentations de `PaymentConfirmationGateway` deviennent actives,
  faire évoluer le mécanisme de composition dans `bootstrap` sur décision
  d'architecture, sans réutiliser ce flag fournisseur comme règle générique.
- Aucun changement d'API, DTO, payload, HMAC, endpoint, règle bancaire, table,
  dépendance ou migration.
- La génération, la livraison et la vérification de l'OTP restent bancaires.
- Les clés HMAC d'idempotence sont distinctes de la création du challenge.

## Vérification locale (non exécutée par le générateur)

```powershell
cd backend
mvn -pl bootstrap -am "-Dtest=PaymentConfirmationBeanActivationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl payment -am test
mvn verify
cd ..
git diff --check
git status --short
```

Après démarrage avec le profil `payment-banking-local`, vérifier l'existence des
beans, puis `initiateDebit -> VERIFIED -> challenge ACTIVE -> AWAITING_OTP`.
Ne pas réutiliser aveuglément un `Idempotency-Key` déjà consommé par un essai
antérieur ; utiliser une nouvelle opération logique de test si nécessaire.
