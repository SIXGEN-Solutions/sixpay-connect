# Fondation Angular de référence

Ce document décrit le socle Angular partagé de SIXPAY CONNECT. Il conserve
l'organisation feature-first déjà présente dans le repository.

## Structure

```text
src/app/
├── core/
│   ├── auth/       Authentification, guard et propagation JWT
│   ├── errors/     Gestion centralisée des erreurs
│   └── http/       URL backend et en-têtes techniques
├── layout/
│   ├── header/
│   ├── sidebar/
│   ├── footer/
│   └── shell/
├── shared/         Composants et utilitaires indépendants du métier
└── features/       Fonctionnalités chargées à la demande
```

`core` contient uniquement les services singleton et mécanismes transverses.
`shared` ne dépend d'aucun domaine métier. Chaque domaine reste autonome dans
`features`.

## Environnements et API

- `environment.ts` utilise une URL relative pour un déploiement derrière le
  même point d'entrée que le backend.
- `environment.development.ts` cible `http://localhost:8080`.
- Les requêtes relatives commençant par `/api/` sont préfixées par
  `apiBaseUrl`.

## Chaîne HTTP

Les interceptors sont appliqués dans cet ordre :

1. résolution de l'URL backend ;
2. ajout de `X-Correlation-ID` lorsqu'il est absent ;
3. ajout de `Idempotency-Key` aux mutations lorsqu'il est absent ;
4. propagation du token avec `Authorization: Bearer ...` ;
5. normalisation et publication des erreurs API.

Le token d'accès est conservé dans `sessionStorage` et exposé par
`AuthenticationService`. Les routes protégées utilisent
`authenticationGuard`.

## Routage

Le shell et les pages sont chargés avec `loadComponent`. Le shell assemble le
header, la navigation latérale, la zone principale et le footer. Les futures
routes Partner seront ajoutées dans `features/partners` sans déplacer le socle.

## Styles

`src/styles/styles.scss` est l'unique point d'entrée global. Il assemble les
tokens, le thème Angular Material, la typographie, l'espacement et les
utilities. Les composants utilisent le préfixe `sp`.

## Validation

Depuis `frontend/` :

```bash
npm ci
npm run lint
npm run test
npm run build
```

Ces quatre commandes constituent le Gate 1.

## Design System minimal viable

La Phase 2 complète les emplacements existants sans modifier l’architecture :

```text
src/app/shared/components/
├── button/          SpButtonComponent
├── card/            SpCardComponent
├── data-table/      SpDataTableComponent
├── dialog/          SpDialogComponent
├── loading/         SpLoadingComponent
├── notification/    SpNotificationComponent
├── search-field/    SpSearchFieldComponent
└── toolbar/         SpToolbarComponent
```

`SpFormErrorComponent` fournit l’affichage accessible des erreurs de formulaire.
Tous ces composants restent indépendants des domaines métier. Le catalogue
minimal est chargé à la demande sur `/design-system`, dans le domaine
`dashboard` déjà présent.

Les styles globaux conservent `src/styles/styles.scss` comme point d’entrée :

- `tokens` définit les couleurs, états, espacements, bordures, ombres et la
  typographie ;
- `themes` configure Angular Material ;
- `typography`, `spacing` et `utilities` appliquent les règles communes ;
- chaque composant conserve ses styles locaux et consomme uniquement les
  tokens partagés.

Le Gate 2 comprend le Gate technique (`npm ci`, lint, tests et build), puis une
validation visuelle du catalogue en desktop et mobile, du focus clavier, des
labels et des retours accessibles.

## Modèle frontend Partner et client API

La Phase 3 complète exclusivement les emplacements Partner existants :

```text
src/app/features/partners/
├── api/
│   ├── partners-api.client.ts
│   └── partners-api.mapper.ts
├── models/
│   ├── create-partners.request.ts
│   ├── partners.response.ts
│   └── partners.ts
└── services/
    └── partners.service.ts
```

- `partners.response.ts` et `create-partners.request.ts` représentent le contrat
  HTTP figé par `partner-api-v1.yaml`.
- `PartnerApiClient` est le seul composant qui appelle directement les huit
  endpoints Partner.
- Les interceptors du socle continuent de gérer JWT, `X-Correlation-ID` et
  `Idempotency-Key`.
- `partners-api.mapper.ts` transforme les réponses HTTP en modèles
  applicatifs, notamment les chaînes `date-time` en `Date`.
- `PartnersService` expose ces modèles applicatifs afin que les composants ne
  dépendent pas directement des DTO HTTP.
- Le mapping RFC 7807 produit des `ApplicationError.fieldErrors` directement
  exploitables par les formulaires.

Le Gate 3 est exécutable avec :

```bash
npm run gate:3
```

Il vérifie automatiquement les propriétés, types, champs obligatoires, enums
et opérations TypeScript par rapport au contrat OpenAPI backend, puis exécute
lint, tests, build et contrôle du formatage.

La configuration `vitest-base.config.mts` utilise un worker `threads` unique.
Elle garantit un comportement stable et déterministe sur les postes Windows et
en CI, sans modifier l’isolation logique entre les fichiers de tests.
