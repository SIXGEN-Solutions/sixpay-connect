# Correctif pipeline frontend

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
