# Tests de référence

## Périmètre

Il couvre les composants et parcours Partner sans modifier le comportement métier
validé par les Gates précédentes :

- tests unitaires et de composants Angular avec Vitest ;
- intégration du client HTTP avec `HttpTestingController` ;
- parcours E2E avec Playwright et backend Partner simulé ;
- accessibilité automatisée avec axe-core ;
- couverture V8 avec seuils bloquants ;
- compilation Angular de production.

## Seuils de couverture

| Mesure     | Seuil minimal |
| ---------- | ------------: |
| Statements |          70 % |
| Branches   |          60 % |
| Functions  |          60 % |
| Lines      |          70 % |

Le rapport HTML est généré dans `coverage/index.html`.

## Installation E2E

Après `npm ci`, installer une seule fois Chromium :

```bash
npm run test:e2e:install
```

Sous Linux CI, installer également les dépendances système si nécessaire :

```bash
npx playwright install --with-deps chromium
```

## Commandes

```bash
npm test
npm run test:coverage
npm run test:e2e
npm run test:e2e:a11y
npm run gate:6
```

## Parcours E2E

- création d’un Partner et redirection vers sa fiche ;
- approbation et consultation du statut ;
- configuration d’un seuil ;
- suspension avec motif ;
- réactivation ;
- consultation de l’audit ;
- refus d’un accès non autorisé ;
- navigation clavier, focus, labels, erreurs de formulaire, contraste et dialogues.

Le backend est simulé au niveau réseau par Playwright. Les scénarios contrôlent également
la présence de `X-Correlation-ID` et de `Idempotency-Key` sur les mutations.
