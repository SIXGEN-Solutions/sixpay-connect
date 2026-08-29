# SIXPAY CONNECT — Matrice de sécurité frontend

Cette matrice distingue deux niveaux :

1. **Golden Module Partner** : règles alignées sur les contrôles backend `@PreAuthorize`.
2. **Navigation Phase 7.1** : politique RBAC frontend pour les domaines du Functional Mock
   Frame. Elle contrôle l'expérience utilisateur et le routage mais ne constitue jamais
   une frontière de sécurité backend.

Les futures intégrations API devront être réalignées sur les scopes et autorisations de
leurs contrats et du backend avant livraison de production.

## Rôles actuellement supportés

- `ADMIN`
- `MANAGER`
- `PARTNER`
- `AUDITOR`

Aucun autre rôle n'est introduit en Phase 7.1.

## Routes globales

| Route                            | ADMIN | MANAGER | PARTNER | AUDITOR | Statut         |
| -------------------------------- | :---: | :-----: | :-----: | :-----: | -------------- |
| `/`                              |  Oui  |   Oui   |   Oui   |   Oui   | Authentifié    |
| `/design-system`                 |  Oui  |   Oui   |   Oui   |   Oui   | Authentifié    |
| `/payments`                      |  Oui  |   Oui   |   Non   |   Oui   | Navigation 7.1 |
| `/payments/:paymentId`           |  Oui  |   Oui   |   Non   |   Oui   | Navigation 7.1 |
| `/customers`                     |  Oui  |   Oui   |   Non   |   Oui   | Navigation 7.1 |
| `/customers/:observedCustomerId` |  Oui  |   Oui   |   Non   |   Oui   | Navigation 7.1 |
| `/accounting`                    |  Oui  |   Oui   |   Non   |   Oui   | Navigation 7.1 |
| `/accounting/batches/:batchId`   |  Oui  |   Oui   |   Non   |   Oui   | Navigation 7.1 |
| `/incidents`                     |  Oui  |   Oui   |   Non   |   Oui   | Navigation 7.1 |
| `/incidents/:incidentId`         |  Oui  |   Oui   |   Non   |   Oui   | Navigation 7.1 |
| `/administration`                |  Oui  |   Non   |   Non   |   Non   | Navigation 7.1 |
| `/administration/settings`       |  Oui  |   Non   |   Non   |   Non   | Navigation 7.1 |
| `/administration/integrations`   |  Oui  |   Non   |   Non   |   Non   | Navigation 7.1 |
| `/identity/users`                |  Oui  |   Non   |   Non   |   Non   | Navigation 7.1 |
| `/identity/roles`                |  Oui  |   Non   |   Non   |   Non   | Navigation 7.1 |

## Routes Golden Module Partner

| Route                  | ADMIN | MANAGER |      PARTNER       | AUDITOR |
| ---------------------- | :---: | :-----: | :----------------: | :-----: |
| `/partners`            |  Oui  |   Oui   |        Non         |   Oui   |
| `/partners/create`     |  Oui  |   Non   |        Non         |   Non   |
| `/partners/:partnerId` |  Oui  |   Oui   |        Non         |   Oui   |
| `/partners/status`     |  Non  |   Non   | Oui, propre statut |   Non   |

Une session absente ou expirée redirige vers `/login` avec le `returnUrl`.
Un utilisateur authentifié sans le rôle requis est redirigé vers `/forbidden`.

## Actions Partner

| Action                      | ADMIN | MANAGER | PARTNER | AUDITOR | Contrainte d'état                       |
| --------------------------- | :---: | :-----: | :-----: | :-----: | --------------------------------------- |
| Créer                       |  Oui  |   Non   |   Non   |   Non   | —                                       |
| Consulter la fiche complète |  Oui  |   Oui   |   Non   |   Oui   | —                                       |
| Consulter son statut        |  Non  |   Non   |   Oui   |   Non   | `sub` JWT = UUID Partner                |
| Approuver                   |  Non  |   Oui   |   Non   |   Non   | `PENDING_VALIDATION`                    |
| Rejeter                     |  Non  |   Oui   |   Non   |   Non   | `PENDING_VALIDATION`, motif obligatoire |
| Suspendre                   |  Oui  |   Non   |   Non   |   Non   | `ACTIVE`, motif obligatoire             |
| Réactiver                   |  Oui  |   Non   |   Non   |   Non   | `SUSPENDED`                             |
| Configurer un seuil         |  Oui  |   Non   |   Non   |   Non   | formulaire valide                       |
| Consulter l'audit           |  Non  |   Non   |   Non   |   Oui   | période valide                          |

## Implémentation Phase 7.1

- `authenticationGuard` protège le shell global.
- `roleGuard` attend `AuthenticationService.ready$`, comme le Golden Module Partner.
- Les routes déclarent leurs rôles dans `route.data.roles`.
- La sidebar consomme `SIXPAY_NAVIGATION`, une configuration unique filtrée avec les mêmes rôles.
- Les accès directs par URL restent protégés même lorsque l'entrée est absente du menu.
- Le simulateur de rôles reste limité au profil `standalone`.
- Le Golden Module Partner conserve son `partnerRoleGuard` et ses politiques métier propres.

## Garanties techniques

- OAuth2/OIDC Authorization Code avec PKCE en environnement sécurisé.
- Le profil `standalone` reste réservé au développement.
- Le frontend ne remplace jamais les décisions d'autorisation backend.
- Les réponses `401` conduisent à la ré-authentification.
- Les réponses `403` conduisent à `/forbidden`.
- Aucun secret ou token n'est journalisé.
- Les contrôles objet, scopes, transitions métier et idempotence restent côté backend.
