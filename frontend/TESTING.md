# Tests de référence — Phase 7.8

## Objectif

La stratégie de test frontend SIXPAY couvre désormais deux chemins d’exécution
distincts :

```text
standalone + mock
integration + API
```

Le premier valide le Functional Mock Frame autonome. Le second compile
réellement avec `environment.integration.ts`, donc :

```text
authentication.mode = local
backend.mode = api
```

et simule uniquement les réponses backend au niveau réseau Playwright.

## Couverture

Les seuils Vitest restent bloquants :

| Mesure | Seuil |
| --- | ---: |
| Statements | 70 % |
| Branches | 60 % |
| Functions | 60 % |
| Lines | 70 % |

## Commandes

```bash
npm test
npm run test:coverage
npm run test:e2e
npm run test:e2e:integration
npm run test:e2e:a11y
npm run build:all
npm run gate:8
```

## E2E standalone/mock

Configuration :

```text
playwright.config.ts
```

Serveur :

```bash
npm start
```

Ce mode valide notamment :

- Partner Golden Module ;
- rôles ADMIN / MANAGER / AUDITOR / PARTNER ;
- création ;
- approbation ;
- seuil ;
- suspension ;
- réactivation ;
- audit ;
- accessibilité ;
- navigation 404 ;
- absence de dépendance backend.

Les tests `integration-*.spec.ts` sont exclus de ce runner.

## E2E integration/API

Configuration :

```text
playwright.integration.config.ts
```

Serveur :

```bash
npm run start:integration
```

sur le port `4201`.

Playwright simule :

```text
GET /api/v1/auth/me
GET /api/v1/partners
```

mais le frontend reste réellement compilé en mode `api`.

Les scénarios 7.8 vérifient :

- restauration d’une session locale ADMIN ;
- absence du panneau Functional Mock Frame ;
- loading réel avant réponse API ;
- success ;
- empty ;
- erreur 429 ;
- `Retry-After` ;
- `X-Correlation-ID`.

## Navigation 404

Une route inconnue ne redirige plus silencieusement vers `/`.

Elle conserve son URL et affiche :

```text
404
Page introuvable
Page précédente
Retour au tableau de bord
```

## 429

Le modèle global d’erreur conserve :

```text
kind = rate-limit
retryAfterSeconds
correlationId
```

La bannière du shell affiche le délai et la référence support quand ils sont
présents.

## Installation Playwright

```bash
npm run test:e2e:install
```

CI Linux :

```bash
npx playwright install --with-deps chromium
```
