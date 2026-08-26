# SIXPAY CONNECT — Frontend

Application web Angular de SIXPAY CONNECT. Le domaine `Partner` constitue le Golden Module
de référence pour l’implémentation des futurs domaines métier.

## Prérequis

| Outil   | Version           |
| ------- | ----------------- |
| Node.js | `22.23.1`         |
| npm     | `>= 10` et `< 12` |

Les versions attendues sont aussi déclarées dans `.node-version`, `.nvmrc` et
`package.json`.

Avec `nvm` :

```bash
nvm install
nvm use
node --version
npm --version
```

## Installation

Depuis `frontend/` :

```bash
npm ci
npm run test:e2e:install
```

`npm ci` est obligatoire en CI et recommandé localement après un changement de branche.
Il installe exactement les versions enregistrées dans `package-lock.json`.

## Démarrage local

```bash
npm start
```

L’application est accessible sur `http://localhost:4200`. Le serveur Angular utilise
`proxy.conf.json` pour transmettre les appels `/api` au backend local et éviter les
erreurs CORS.

Le mode développement utilise une identité `standalone`. Ce mode est strictement réservé
au poste de développement.

## Environnements

| Environnement | Fichier/configuration                  | Authentification | API                           |
| ------------- | -------------------------------------- | ---------------- | ----------------------------- |
| Développement | `environment.development.ts`           | `standalone`     | URL relative via proxy        |
| QA            | build de production avec paramètres QA | OIDC Code + PKCE | point d’entrée QA             |
| Production    | `environment.ts`                       | OIDC Code + PKCE | URL relative au reverse proxy |

Les paramètres `authority`, `clientId`, `scope` et `apiBaseUrl` ne doivent contenir aucun
secret. Les secrets, certificats et clés privées restent gérés par la plateforme de
déploiement et le fournisseur d’identité.

## Commandes principales

```bash
npm start
npm run lint
npm test
npm run test:coverage
npm run test:e2e
npm run test:e2e:a11y
npm run dependencies:audit
npm run build
npm run format:check
npm run gate:7
```

## Architecture

```text
src/app/
├── core/           Authentification, HTTP, erreurs et services singleton
├── layout/         Shell, header, sidebar et footer
├── shared/         Composants, directives et utilitaires sans dépendance métier
└── features/
    └── partners/   Golden Module métier
```

Une feature respecte la structure suivante :

```text
features/<domain>/
├── api/
├── components/
├── guards/
├── models/
├── resolvers/
├── security/
├── services/
├── store/
├── <domain>.routes.ts
└── index.ts
```

Les règles détaillées de développement et de réplication sont décrites dans :

- [ARCHITECTURE.md](ARCHITECTURE.md) ;
- [DEVELOPER-GUIDE.md](DEVELOPER-GUIDE.md) ;
- [CI.md](CI.md) ;
- [TESTING.md](TESTING.md) ;
- [SECURITY-MATRIX.md](SECURITY-MATRIX.md) ;
- [GOLDEN-MODULE-CHECKLIST.md](GOLDEN-MODULE-CHECKLIST.md).

## Intégration API

- les composants appellent les services applicatifs ;
- les services utilisent le client API du domaine ;
- le client API manipule les DTO HTTP ;
- les mappers convertissent DTO et modèles applicatifs ;
- les interceptors ajoutent l’URL, le JWT, `X-Correlation-ID` et
  `Idempotency-Key` ;
- les erreurs RFC 7807 `ProblemDetail` sont converties en erreurs applicatives.

Le contrat Partner est contrôlé avec :

```bash
npm run contract:partner
```

## Pull Requests

Le workflow `Frontend CI` exécute l’installation déterministe, le contrat OpenAPI, le
lint, les tests avec couverture, l’analyse des dépendances, le build de production, les
tests E2E et l’accessibilité.

La règle de protection de la branche cible doit rendre obligatoires les contrôles :

- `Frontend quality gate` ;
- `Frontend E2E`.

Une Pull Request ne doit jamais être fusionnée si l’un de ces contrôles échoue.
