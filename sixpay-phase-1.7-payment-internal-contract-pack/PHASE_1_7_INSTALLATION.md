# Phase 1 — Gate IA-0.5P : Payment Contract Pack
## Étape 1.7 — Contrats internes de consultation

Ce bundle ne modifie pas GitHub directement. Il est préparé pour la branche
`feat/payment-contract-pack`.

## Contenu

### Nouveaux contrats

- `documentation/contracts/internal/payment-query-api-v1.yaml`
- `documentation/contracts/internal/observed-customer-query-api-v1.yaml`
- `documentation/contracts/internal/payment-audit-query-api-v1.yaml`

### Synchronisation des autorités existantes

Le fichier `patches/phase-1.7-internal-contracts.patch` met à jour :

- `documentation/contracts/README.md`
- `documentation/contracts/CONTRACT_REGISTRY.yaml`
- `documentation/ai/payment/PAYMENT_CONTRACT_REQUIREMENTS.yaml`
- `documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml`

Le patch retire le préfixe historique `sixpay-` des deux contrats internes,
ajoute le contrat d’audit séparé et conserve `amplitude-payment-verification-api-v1`
comme seul contrat MVP encore manquant dans le registre.

## Installation

Depuis la racine du repository, sur `feat/payment-contract-pack` :

```bash
git switch feat/payment-contract-pack
unzip sixpay-phase-1.7-payment-internal-contract-pack.zip -d /tmp/sixpay-1.7

mkdir -p documentation/contracts/internal
cp /tmp/sixpay-1.7/sixpay-phase-1.7-payment-internal-contract-pack/documentation/contracts/internal/*.yaml \
   documentation/contracts/internal/

git apply --check \
  /tmp/sixpay-1.7/sixpay-phase-1.7-payment-internal-contract-pack/patches/phase-1.7-internal-contracts.patch

git apply \
  /tmp/sixpay-1.7/sixpay-phase-1.7-payment-internal-contract-pack/patches/phase-1.7-internal-contracts.patch
```

Si la branche a évolué après la génération de ce bundle, appliquer les mêmes
hunks manuellement plutôt que de forcer le patch.

## Validation locale

Le script fourni valide les nouveaux fichiers indépendamment du repository :

```bash
python3 scripts/validate-payment-internal-contract-pack.py
```

Après copie du script dans le repository, exécuter également le lint OpenAPI
déjà retenu par le projet. Exemple avec Redocly :

```bash
npx @redocly/cli lint \
  documentation/contracts/internal/payment-query-api-v1.yaml \
  documentation/contracts/internal/observed-customer-query-api-v1.yaml \
  documentation/contracts/internal/payment-audit-query-api-v1.yaml
```

Validation YAML complémentaire :

```bash
python3 - <<'PY'
from pathlib import Path
import yaml
for path in sorted(Path("documentation/contracts/internal").glob("*.yaml")):
    yaml.safe_load(path.read_text(encoding="utf-8"))
    print("YAML OK", path)
PY
```

## Vérifications Git avant commit

```bash
git diff --check
git status --short
grep -R "registryId:" documentation/contracts/internal
grep -R "phaseStep: \"1.7\"" documentation/contracts/internal
grep -R "payment-audit-query-api-v1" \
  documentation/contracts/CONTRACT_REGISTRY.yaml \
  documentation/ai/payment/PAYMENT_CONTRACT_REQUIREMENTS.yaml \
  documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml
```

## Décisions structurantes

- Les trois contrats sont OpenAPI `3.1.0`, version `1.0.0`.
- Ils restent `REFERENCE_ONLY` et `codeGenerationAllowed: false` tant que les
  approbations internes du Gate IA-0.5P ne sont pas obtenues.
- Payment Query et ObservedCustomer Query utilisent des scopes ordinaires
  distincts.
- La timeline, l’audit immuable et l’export contrôlé sont isolés dans
  Payment Audit Query avec des scopes privilégiés.
- Toutes les consultations privilégiées, les refus et les exports sont audités.
- Aucun contrat n’expose d’opération de mutation Payment ou ObservedCustomer.
- La création d’un job d’export est une opération administrative idempotente ;
  elle ne modifie ni Payment ni la piste d’audit.
- Les comptes sont masqués et aucun secret ou payload externe brut n’est exposé.
