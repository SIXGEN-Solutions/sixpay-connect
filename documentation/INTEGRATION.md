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


# Pipeline CI frontend

Ce correctif cible la branche `feat/industrialisation-documentation` à partir du commit :

```text
5bbe2c2
```

## Intégration

Depuis la racine du repository :

```bash
git apply --check pipeline-fix.patch
git apply pipeline-fix.patch
git diff --check
git status
```

Le dossier `overlay/` contient également les deux fichiers complets.

## Fichiers

- `.github/workflows/frontend-ci.yml` ;
- `.github/CODEOWNERS`.

## Après le push

1. ouvrir une Pull Request modifiant le frontend ;
2. vérifier l’apparition des checks `Frontend quality gate` et `Frontend E2E` ;
3. ouvrir **Settings > Rules > Rulesets** ;
4. rendre ces deux checks obligatoires sur `main` et `develop` ;
5. exiger au moins une approbation CODEOWNERS.

`CODEOWNERS` utilise actuellement `@toukam-rodrigue`. Les règles pourront être remplacées
progressivement par des équipes telles que `@SIXGEN-Solutions/backend` et
`@SIXGEN-Solutions/frontend` après leur création dans l’organisation GitHub.
