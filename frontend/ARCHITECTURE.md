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
