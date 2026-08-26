# Intégration continue frontend — Phase 7.8

## Checks obligatoires

Le workflow `.github/workflows/frontend-ci.yml` expose deux jobs bloquants :

```text
Frontend quality gate
Frontend E2E
```

## Frontend quality gate

Exécute :

```text
npm ci
format:check
contract:partner
lint
test:coverage
dependencies:audit
build:all
```

`build:all` compile les trois profils supportés :

```text
integration
netlify
production
```

Production est compilé en dernier afin que `dist/frontend` corresponde au
bundle de production archivé par GitHub Actions.

## Frontend E2E

Exécute :

```text
npm run test:e2e
npm run test:e2e:integration
```

Le premier runner valide le mode standalone/mock.

Le second valide le profil integration/API avec backend simulé au niveau réseau.

## Gate local final de Phase 7

```bash
npm run gate:8
```

Équivalent logique :

```text
contract
→ lint
→ coverage
→ dependency audit
→ integration build
→ netlify build
→ production build
→ standalone/mock E2E
→ integration/API E2E
→ format
```

## Protection de branche

Configurer un Ruleset GitHub sur `main` et `develop` avec :

- PR obligatoire ;
- branche à jour ;
- au moins une approbation ;
- `Frontend quality gate` obligatoire ;
- `Frontend E2E` obligatoire ;
- interdiction du force-push ;
- interdiction de suppression de branche protégée.

## Artifacts

Le workflow conserve pendant 14 jours :

- couverture ;
- audit npm ;
- bundle production ;
- rapport Playwright mock ;
- rapport Playwright integration ;
- traces/vidéos/screenshots d’échec Playwright.
