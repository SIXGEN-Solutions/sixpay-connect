# Intégration continue frontend

Le workflow [frontend-ci.yml](../.github/workflows/frontend-ci.yml) s’exécute sur les
Pull Requests qui modifient le frontend, ainsi que sur les pushes vers `main` et
`develop`.

## Contrôles bloquants

| Job                   | Contrôles                                                              |
| --------------------- | ---------------------------------------------------------------------- |
| Frontend quality gate | `npm ci`, format, contrat API, lint, tests, couverture, audit et build |
| Frontend E2E          | installation Chromium, parcours Playwright et accessibilité            |

Les rapports de couverture, d’audit des dépendances, le bundle de production et les
rapports Playwright sont conservés pendant 14 jours.

## Activer le blocage des Pull Requests

La configuration suivante doit être réalisée une fois par un administrateur GitHub :

1. ouvrir **Settings > Rules > Rulesets** ;
2. créer une règle visant `main` et, si nécessaire, `develop` ;
3. activer **Require a pull request before merging** ;
4. activer **Require status checks to pass** ;
5. sélectionner `Frontend quality gate` et `Frontend E2E` ;
6. exiger une branche à jour avant fusion ;
7. exiger au moins une approbation ;
8. interdire suppressions et force-pushes sur les branches protégées.

La règle ne peut pas être déclarée par le fichier du workflow : le workflow produit les
statuts, tandis que le ruleset GitHub les rend obligatoires.

## Rapports

- couverture : `frontend-coverage` ;
- audit npm : `frontend-dependency-audit` ;
- bundle : `frontend-production-build` ;
- rapport E2E : `frontend-playwright-report` ;
- traces d’échec : `frontend-playwright-traces`.

## Reproduction locale

```bash
cd frontend
npm ci
npm run gate:7
npm run test:e2e
```
