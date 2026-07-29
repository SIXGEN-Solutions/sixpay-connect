# Pipeline CI backend

Ce correctif cible la branche `feat/industrialisation-documentation` à partir du commit :

```text
ed591ea
```

## Intégration

Depuis la racine du repository :

```bash
git apply --check backend-ci.patch
git apply backend-ci.patch
git diff --check
git status
```

Le dossier `overlay/` contient également le fichier complet :

```text
.github/workflows/backend-ci.yml
```

## Checks produits

- `Backend quality gate` ;
- `Backend integration tests` ;
- `Backend dependency review`.

## Après le push

1. déclencher manuellement `Backend CI` ou ouvrir une Pull Request modifiant `backend/**` ;
2. vérifier les trois jobs et leurs artefacts ;
3. rendre `Backend quality gate` et `Backend integration tests` obligatoires dans le
   ruleset GitHub ;
4. rendre `Backend dependency review` obligatoire si le dépôt prend en charge Dependency
   Review ;
5. ajouter ultérieurement le Maven Wrapper 3.9.11 pour rendre la version Maven totalement
   déterministe.

Le workflow utilise actuellement le Maven préinstallé du runner GitHub. Maven Enforcer
refuse toute version hors de la plage `[3.9.6,4.0.0)`.
