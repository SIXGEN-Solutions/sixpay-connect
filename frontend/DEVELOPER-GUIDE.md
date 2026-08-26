# Guide développeur frontend

## Principes

- organisation feature-first ;
- composants standalone et chargement paresseux des écrans ;
- séparation stricte entre contrat HTTP, modèle applicatif et présentation ;
- composants partagés indépendants des domaines métier ;
- autorisations cohérentes avec le backend sans s’y substituer ;
- accessibilité et tests inclus dans la définition de terminé.

## Créer une feature

Créer `src/app/features/<domain>/` puis ajouter uniquement les dossiers nécessaires :

```text
api/          Client HTTP et mappers
components/   Pages et composants métier
guards/       Protection des routes
models/       Requêtes, réponses et modèles applicatifs
resolvers/    Préchargement des données
security/     Politiques routes/actions/rôles
services/     Orchestration applicative
store/        État local si nécessaire
```

Ajouter ensuite :

1. `<domain>.routes.ts` avec des routes lazy-loaded ;
2. `index.ts` comme API publique du domaine ;
3. les tests à côté du code avec le suffixe `.spec.ts` ;
4. les scénarios E2E prioritaires dans `e2e/` ;
5. le contrôle du contrat OpenAPI si le domaine expose un client API ;
6. la matrice des rôles et actions ;
7. la navigation seulement après validation des guards.

Un domaine n’importe jamais les composants internes d’un autre domaine. Une capacité
réellement transverse est déplacée vers `core` ou `shared` après validation architecturale.

## Conventions des composants

- préfixe de sélecteur : `sp-` ;
- nom TypeScript : `Sp...Component` pour le Design System ;
- composants métier : `<Domain><UseCase>Component` ou `<Domain><Page>Component` ;
- logique métier absente des templates ;
- formulaires réactifs avec validation client et erreurs backend ;
- état `loading`, `success`, `empty`, `forbidden` et `error` explicite ;
- boutons de mutation désactivés pendant l’appel ;
- labels, focus, erreurs et dialogues accessibles ;
- styles locaux basés sur les tokens `--sp-*`.

## Conventions des services et API

```text
Component
    ↓
DomainService
    ↓
DomainApiClient
    ↓
HTTP interceptors
    ↓
Backend
```

- seul `DomainApiClient` appelle `HttpClient` ;
- les fichiers `*.response.ts` représentent le contrat backend ;
- les fichiers `*.request.ts` représentent les commandes HTTP ;
- les mappers convertissent les dates, valeurs optionnelles et collections ;
- les composants manipulent uniquement les modèles applicatifs ;
- les erreurs suivent RFC 7807 et conservent le correlation ID ;
- aucune donnée sensible n’est écrite dans les logs.

## Environnements et authentification

- développement : `standalone`, identité locale sans token ;
- QA et production : OIDC Authorization Code avec PKCE ;
- `401` : expiration locale puis redirection vers `/login` ;
- `403` : redirection vers `/forbidden`.

Les valeurs publiques OIDC sont configurables par environnement. Aucun secret OAuth ne
doit être livré dans un bundle frontend.

## Rôles

La référence complète est [SECURITY-MATRIX.md](SECURITY-MATRIX.md).

| Capacité Partner     | ADMIN | MANAGER | PARTNER | AUDITOR |
| -------------------- | :---: | :-----: | :-----: | :-----: |
| Créer                |  Oui  |   Non   |   Non   |   Non   |
| Approuver/rejeter    |  Non  |   Oui   |   Non   |   Non   |
| Suspendre/réactiver  |  Oui  |   Non   |   Non   |   Non   |
| Consulter son statut |  Non  |   Non   |   Oui   |   Non   |
| Consulter l’audit    |  Non  |   Non   |   Non   |   Oui   |

Le masquage d’un bouton et les guards améliorent l’expérience utilisateur. Le backend
reste la frontière de sécurité et doit réévaluer chaque autorisation.

## Conventions de tests

- Vitest : unités, services, mappers, guards, interceptors et composants ;
- `HttpTestingController` : requêtes, réponses, erreurs et headers ;
- Playwright : parcours utilisateur dans Chromium ;
- axe-core : détection automatisée des violations WCAG ;
- un test décrit un comportement observable, pas une implémentation interne ;
- les sélecteurs E2E privilégient rôle, nom accessible et label ;
- les appels réseau attendus sont synchronisés avec `waitForResponse`.

Consulter [TESTING.md](TESTING.md) pour les seuils et commandes.

## Pull Request

Avant de pousser :

```bash
npm ci
npm run gate:7
npm run test:e2e
```

La protection GitHub doit exiger les jobs `Frontend quality gate` et `Frontend E2E`,
interdire la fusion si la branche n’est pas à jour et exiger au moins une approbation.

## Ajouter un futur domaine

1. copier la structure, pas les noms, du Golden Module Partner ;
2. figer le contrat API et ses enums ;
3. définir modèles HTTP, modèles applicatifs et mappers ;
4. créer le client API puis le service métier ;
5. définir la matrice routes/actions/rôles ;
6. construire les écrans avec le Design System ;
7. couvrir les états et erreurs ;
8. ajouter unités, intégration HTTP, E2E et accessibilité ;
9. ajouter la vérification du contrat au pipeline ;
10. exécuter la checklist Golden Module avant la Pull Request.
