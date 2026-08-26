# Checklist de réplication du Golden Module

Cette checklist doit être copiée dans la Pull Request de tout nouveau domaine métier.

## Structure

- [ ] Le domaine est créé sous `src/app/features/<domain>`.
- [ ] Les dossiers suivent la structure feature-first.
- [ ] Le domaine expose uniquement son API publique dans `index.ts`.
- [ ] Aucun import ne cible les composants internes d’un autre domaine.
- [ ] Les routes sont chargées à la demande.

## Contrat et intégration API

- [ ] Le contrat OpenAPI est figé et versionné.
- [ ] Les request/response DTO correspondent au contrat.
- [ ] Le client API est l’unique utilisateur de `HttpClient`.
- [ ] Les mappers séparent DTO et modèles applicatifs.
- [ ] `ProblemDetail`, erreurs de champs et correlation ID sont gérés.
- [ ] Les mutations transmettent une clé d’idempotence.
- [ ] Les appels transmettent le correlation ID et, en OIDC, le bearer token.

## Interface

- [ ] Les composants du Design System sont réutilisés.
- [ ] Les formulaires couvrent validations client et backend.
- [ ] Les états chargement, succès, vide, interdit, introuvable et erreur sont traités.
- [ ] Les doubles soumissions sont bloquées.
- [ ] Le rendu est responsive.

## Sécurité

- [ ] La matrice routes/actions/rôles est documentée.
- [ ] Les routes possèdent des guards.
- [ ] Les actions sont affichées selon rôle et état.
- [ ] Les réponses 401 et 403 sont traitées.
- [ ] Aucune donnée sensible n’est journalisée.
- [ ] Les contrôles frontend ne remplacent jamais ceux du backend.

## Tests

- [ ] Services, mappers, validateurs, guards et interceptors sont testés.
- [ ] Formulaires valides/invalides et erreurs API sont testés.
- [ ] Le client API est testé avec backend simulé.
- [ ] Les requêtes, réponses et headers techniques sont vérifiés.
- [ ] Les parcours prioritaires possèdent des tests Playwright.
- [ ] axe-core ne remonte aucune erreur critique.
- [ ] Les seuils de couverture sont respectés.

## Documentation et CI

- [ ] Le README référence le nouveau domaine.
- [ ] Les commandes et paramètres d’environnement sont documentés.
- [ ] La vérification du contrat est ajoutée au pipeline.
- [ ] `npm ci`, lint, couverture, audit, E2E et build sont verts.
- [ ] Les rapports CI sont publiés comme artefacts.
- [ ] Les contrôles GitHub obligatoires bloquent la fusion en cas d’échec.
