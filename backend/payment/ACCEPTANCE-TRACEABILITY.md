# Traçabilité d’acceptation — Payment Lot 3.1

| ID | Critère | Preuve automatisée |
| --- | --- | --- |
| `PAY-L3.1-ACC-001` | le module est un JAR non exécutable | `PaymentArchitectureTest.moduleIsNotAnExecutableSpringBootApplication` |
| `PAY-L3.1-ACC-002` | `PaymentModule` et le package pur `domain` existent | `PaymentArchitectureTest.moduleContainsOnlyTheApprovedFoundationSources` |
| `PAY-L3.1-ACC-003` | seules les dépendances de fondation sont déclarées | `PaymentArchitectureTest.moduleDeclaresOnlyFoundationDependencies` |
| `PAY-L3.1-ACC-004` | le domaine ne dépend d’aucun framework | `PaymentArchitectureTest.domainRemainsFrameworkAgnostic` |
| `PAY-L3.1-ACC-005` | Payment n’importe aucun autre domaine métier | `PaymentArchitectureTest.paymentDoesNotDependOnAnotherBusinessDomain` |
| `PAY-L3.1-ACC-006` | l’autorisation active est limitée au domaine et au Lot 3.1 | `PaymentArchitectureTest.controlledAuthorizationRemainsDomainOnly` |
| `PAY-L3.1-ACC-007` | les primitives de plateforme ne sont pas dupliquées | `PaymentArchitectureTest.moduleDoesNotRedefineSharedPlatformPrimitives` |
| `PAY-L3.1-ACC-008` | les couches API/application/infrastructure ne sont pas générées | `PaymentArchitectureTest.moduleContainsOnlyTheApprovedFoundationSources` |
| `PAY-L3.1-ACC-009` | le POM reste gouverné par le BOM | `PaymentArchitectureTest.moduleDeclaresOnlyFoundationDependencies` |

## Contrôles manuels

- `backend/payment/pom.xml` conserve le parent officiel.
- aucune modification n’est apportée aux contrats, migrations ou autres modules;
- le modèle IA-1 reste inchangé;
- la prochaine activation est `Lot 3.2`.

## Limites du sous-lot

Lot 3.1 ne prétend pas valider les 76 invariants ni les 38 transitions. Ces
preuves apparaîtront avec les classes métier correspondantes dans les
sous-lots suivants.
