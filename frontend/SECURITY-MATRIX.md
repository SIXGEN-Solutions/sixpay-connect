# SIXPAY CONNECT — Matrice de sécurité frontend

Cette matrice est alignée sur les contrôles `@PreAuthorize` du module backend `partner`.
Les règles frontend améliorent l’expérience utilisateur, mais ne constituent jamais une
frontière de sécurité. Chaque requête reste autorisée par le backend.

## Routes

| Route                  | ADMIN | MANAGER |      PARTNER       | AUDITOR |
| ---------------------- | :---: | :-----: | :----------------: | :-----: |
| `/`                    |  Oui  |   Oui   |        Oui         |   Oui   |
| `/design-system`       |  Oui  |   Oui   |        Oui         |   Oui   |
| `/partners`            |  Oui  |   Oui   |        Non         |   Oui   |
| `/partners/create`     |  Oui  |   Non   |        Non         |   Non   |
| `/partners/:partnerId` |  Oui  |   Oui   |        Non         |   Oui   |
| `/partners/status`     |  Non  |   Non   | Oui, propre statut |   Non   |

Une session absente ou expirée redirige vers `/login`. Un utilisateur authentifié sans le
rôle requis est redirigé vers `/forbidden`.

## Actions Partner

| Action                      | ADMIN | MANAGER | PARTNER | AUDITOR | Contrainte d’état                       |
| --------------------------- | :---: | :-----: | :-----: | :-----: | --------------------------------------- |
| Créer                       |  Oui  |   Non   |   Non   |   Non   | —                                       |
| Consulter la fiche complète |  Oui  |   Oui   |   Non   |   Oui   | —                                       |
| Consulter son statut        |  Non  |   Non   |   Oui   |   Non   | `sub` JWT = UUID Partner                |
| Approuver                   |  Non  |   Oui   |   Non   |   Non   | `PENDING_VALIDATION`                    |
| Rejeter                     |  Non  |   Oui   |   Non   |   Non   | `PENDING_VALIDATION`, motif obligatoire |
| Suspendre                   |  Oui  |   Non   |   Non   |   Non   | `ACTIVE`, motif obligatoire             |
| Réactiver                   |  Oui  |   Non   |   Non   |   Non   | `SUSPENDED`                             |
| Configurer un seuil         |  Oui  |   Non   |   Non   |   Non   | formulaire valide                       |
| Consulter l’audit           |  Non  |   Non   |   Non   |   Oui   | période valide                          |

## Garanties techniques

- OAuth2/OIDC Authorization Code avec PKCE et renouvellement de session en mode sécurisé ;
- profil `standalone` explicite uniquement dans l’environnement de développement ;
- JWT conservé par le gestionnaire OIDC et transmis uniquement aux appels API ;
- réponses `401` : session locale supprimée puis redirection vers la connexion ;
- réponses `403` : redirection vers la page d’accès interdit ;
- boutons mutateurs désactivés pendant les appels et clés d’idempotence envoyées au backend ;
- aucun token, secret, courriel de contact ou contenu sensible écrit dans les logs frontend ;
- décisions d’accès objet, transitions et idempotence toujours réévaluées côté backend.

## Paramètres de déploiement

Les valeurs `authentication.authority` et `authentication.clientId` de l’environnement de
production sont des valeurs de référence à remplacer par celles du fournisseur d’identité
SIXPAY. Les URI de connexion et de déconnexion de l’application doivent aussi être
enregistrées auprès de ce fournisseur.
