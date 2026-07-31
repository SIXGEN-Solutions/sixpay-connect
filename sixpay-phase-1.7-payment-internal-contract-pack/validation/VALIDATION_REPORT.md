# Validation Report — Phase 1.7

Date de génération : 2026-07-31  
Branche cible : `feat/payment-contract-pack`  
Gate : `IA-0.5P_PAYMENT_CONTRACT_PACK`  
Étape : `1.7`

## Résultat

```text
Payment internal contract pack validation PASSED
 - YAML files: 3
 - Unique operations: 10
 - OpenAPI version: 3.1.0
 - Contract version: 1.0.0
 - Required metadata: present
 - Read-only boundary: enforced
 - Correlation header: required
 - RFC 7807 fields: present
```

## Contrats

| Fichier | Paths | Version OpenAPI | Version contrat |
|---|---:|---|---|
| `payment-query-api-v1.yaml` | 2 | `3.1.0` | `1.0.0` |
| `observed-customer-query-api-v1.yaml` | 3 | `3.1.0` | `1.0.0` |
| `payment-audit-query-api-v1.yaml` | 5 | `3.1.0` | `1.0.0` |

## Contrôles exécutés

- parsing YAML réussi ;
- métadonnées `x-sixpay-contract` obligatoires présentes ;
- version URI majeure `v1` ;
- `X-Correlation-ID` obligatoire sur chaque opération ;
- réponses de base `400`, `401`, `403` présentes ;
- schéma RFC 7807 enrichi avec `code` et `correlationId` ;
- unicité des `operationId` ;
- absence d’opérations de mutation Payment/ObservedCustomer ;
- seule exception non-GET : création idempotente d’un job d’export d’audit ;
- recherche de marqueurs sensibles évidents ;
- séparation Payment Query / ObservedCustomer Query / Payment Audit Query.

## Limite

Le lint Redocly/Spectral du repository et `git apply --check` doivent être
exécutés après insertion dans la branche cible, car cet environnement ne peut
pas cloner le repository. Le bundle fournit les commandes exactes.
