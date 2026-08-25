# FS-2.7.0 — Documentation Inventory

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.7 — Documentation consolidation`

## Purpose

Inventory and classify documentation before any merge, rename or deletion.

No documentation file is deleted by FS-2.7.0.

## Source-of-truth order

```text
1. authoritative implementation branch
2. documentation/architecture/
3. documentation/requirements/
4. documentation/contracts/
5. documentation/ai/
6. engineering assets
7. ENGINEERING_CONTEXT.md
```

## Inventory summary

- documentation files: **248**
- normalized-name duplicate groups: **11**
- binary documents: **14**
- unreferenced binary documents: **2**
- historical/transitional candidates: **25**

### By extension

- `.docx`: **10**
- `.json`: **10**
- `.md`: **177**
- `.pdf`: **4**
- `.yaml`: **46**
- `<none>`: **1**

### By classification

- `CANONICAL_AI_CANDIDATE`: **89**
- `CANONICAL_ARCHITECTURE_CANDIDATE`: **49**
- `CANONICAL_CONTRACT`: **39**
- `CANONICAL_RUNBOOK_CANDIDATE`: **21**
- `DOCUMENTATION_OTHER`: **11**
- `REVIEW_BINARY_SOURCE`: **14**
- `REVIEW_HISTORY`: **25**

## Duplicate / near-duplicate names

Similar names are review candidates, not automatic deletion candidates.

- Group:
  - `documentation/ai/customer/AI_CONTEXT_MANIFEST.yaml` — references: **0**
  - `documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml` — references: **6**
- Group:
  - `documentation/ai/customer/IA_0R_BLOCKING_DECISIONS.md` — references: **2**
  - `documentation/ai/customer/IA_0R_BLOCKING_DECISIONS.yaml` — references: **11**
- Group:
  - `documentation/ai/payment/PAYMENT_COMMAND_CATALOGUE.md` — references: **3**
  - `documentation/ai/payment/PAYMENT_COMMAND_CATALOGUE.yaml` — references: **4**
- Group:
  - `documentation/ai/payment/PAYMENT_INVARIANT_CATALOGUE.md` — references: **4**
  - `documentation/ai/payment/PAYMENT_INVARIANT_CATALOGUE.yaml` — references: **5**
- Group:
  - `documentation/ai/payment/PAYMENT_POLICY_DOMAIN_SERVICE_CATALOGUE.md` — references: **5**
  - `documentation/ai/payment/PAYMENT_POLICY_DOMAIN_SERVICE_CATALOGUE.yaml` — references: **6**
- Group:
  - `documentation/architecture/integration/accounting-batch-reconciliation.md` — references: **0**
  - `documentation/runbooks/accounting/ACCOUNTING_BATCH_RECONCILIATION.md` — references: **0**
- Group:
  - `documentation/architecture/SIXPAY_CONNECT-SDS.docx` — references: **1**
  - `documentation/architecture/SIXPAY_CONNECT_SDS.docx` — references: **1**
- Group:
  - `documentation/architecture/tresorpay/Cahier des Charges interopérabilité entre TRESOR PAY et le core banking_final_schema_final.docx` — references: **0**
  - `documentation/requirements/cdc/Cahier des Charges interopérabilité entre TRESOR PAY et le core banking_final_schema_final.docx` — references: **1**
- Group:
  - `documentation/contracts/amplitude/amplitude-customer-verification-api-v1.yaml` — references: **0**
  - `documentation/contracts/integration/amplitude-customer-verification-api-v1.yaml` — references: **12**
- Group:
  - `documentation/contracts/README.md` — references: **0**
  - `documentation/implementation/customer-observation/README.md` — references: **4**
- Group:
  - `documentation/implementation/customer-observation/PHASE-CLOSURE-CHECKLIST.md` — references: **0**
  - `documentation/implementation/phase6/PHASE-CLOSURE-CHECKLIST.md` — references: **1**

## Binary sources requiring semantic review

- `documentation/architecture/Product_SIXPAY_CONNECT.docx` — references: **1**
- `documentation/architecture/SIXPAY_CONNECT-SDS.docx` — references: **1**
- `documentation/architecture/SIXPAY_CONNECT_Application_Architecture_Blueprint.docx` — references: **1**
- `documentation/architecture/SIXPAY_CONNECT_Guide_Implementation.docx` — references: **1**
- `documentation/architecture/SIXPAY_CONNECT_Product_Blueprint.docx` — references: **1**
- `documentation/architecture/SIXPAY_CONNECT_SDS.docx` — references: **1**
- `documentation/architecture/SIXPAY_CONNECT_Technical_Architecture.docx` — references: **1**
- `documentation/architecture/tresorpay/API_ESSENTIEL_TRESORPAY.pdf` — references: **1**
- `documentation/architecture/tresorpay/Cahier des Charges interopérabilité entre TRESOR PAY et le core banking_final_schema_final.docx` — references: **0**
- `documentation/architecture/tresorpay/Documentation_API_TresorPay_v1.pdf` — references: **0**
- `documentation/requirements/cdc/Cahier des Charges interopérabilité entre TRESOR PAY et le core banking_final_schema_final.docx` — references: **1**
- `documentation/requirements/cdc/SIXPAY_CONNECT_CDC.pdf` — references: **1**
- `documentation/requirements/cdc/SIXPAY_CONNECT_Specifications_Fonctionnelles.pdf` — references: **1**
- `documentation/requirements/user-stories/SIXPAY_CONNECT_USER_STORIES.docx` — references: **1**

## Historical/transitional candidates

- `documentation/architecture/configuration/FS-2.5.0_CONFIGURATION_INVENTORY.md` — references: **0**
- `documentation/architecture/configuration/FS-2.5.1_BOOTSTRAP_GLOBAL_CONFIGURATION_NORMALIZATION.md` — references: **0**
- `documentation/architecture/configuration/FS-2.5.2_DOMAIN_CONFIGURATION_OWNERSHIP.md` — references: **0**
- `documentation/architecture/configuration/FS-2.5.3_PROFILES_CONSOLIDATION.md` — references: **0**
- `documentation/architecture/configuration/FS-2.5.4_SECURITY_AUTHENTICATION_CONFIGURATION.md` — references: **0**
- `documentation/architecture/configuration/FS-2.5.5_OPENAPI_SPRINGDOC_CONFIGURATION.md` — references: **0**
- `documentation/architecture/configuration/FS-2.5.6_ANGULAR_ENVIRONMENTS.md` — references: **0**
- `documentation/architecture/configuration/FS-2.5.7_FEATURE_FLAG_REGISTRY.md` — references: **0**
- `documentation/architecture/configuration/FS-2.5.8_CONFIGURATION_NON_REGRESSION_GATE.md` — references: **0**
- `documentation/architecture/configuration/FS-2.5.9_CONFIGURATION_FINAL_VALIDATION.md` — references: **0**
- `documentation/architecture/FS-2.6_TESTS_AND_GATES_CONSOLIDATION.md` — references: **0**
- `documentation/architecture/integration/FS-1.1-contract-backed-full-stack-consolidation.md` — references: **0**
- `documentation/architecture/integration/FS-1.2-full-stack-conformance-tests.md` — references: **0**
- `documentation/architecture/integration/FS-1.3-accounting-query-contract.md` — references: **0**
- `documentation/architecture/internal/fs-1.4.0-administration-incidents-ownership.md` — references: **0**
- `documentation/architecture/module-boundaries/FS-2.4.0_MODULE_DEPENDENCY_AUDIT.md` — references: **0**
- `documentation/architecture/module-boundaries/FS-2.4.1_BUSINESS_EDGE_CLASSIFICATION.md` — references: **0**
- `documentation/architecture/module-boundaries/FS-2.4.2_MODULE_BOUNDARY_NON_REGRESSION_GATE.md` — references: **0**
- `documentation/architecture/persistence/FS-2.3.0_DATABASE_MIGRATION_INVENTORY.md` — references: **0**
- `documentation/architecture/persistence/FS-2.3.1_DATABASE_OWNERSHIP_POLICY.md` — references: **0**
- `documentation/architecture/persistence/FS-2.3.2_DATABASE_BASELINE_DESIGN.md` — references: **0**
- `documentation/architecture/persistence/FS-2.3.5_CROSS_DOMAIN_PERSISTENCE_REVIEW.md` — references: **0**
- `documentation/architecture/persistence/FS-2.3.6_FLYWAY_RUNTIME_ASSEMBLY.md` — references: **0**
- `documentation/architecture/persistence/FS-2.3.7_FRESH_POSTGRESQL_VALIDATION.md` — references: **0**
- `documentation/architecture/persistence/FS-2.3.8_DATABASE_BASELINE_GOVERNANCE_GATE.md` — references: **0**

## FS-2.7 decision vocabulary

```text
KEEP_CANONICAL
MERGE_INTO_CANONICAL
KEEP_REFERENCE_SOURCE
ARCHIVE_HISTORY
DELETE_ABSORBED_HISTORY
REVIEW_SEMANTIC_DUPLICATE
```

## Next step

FS-2.7.1 defines the canonical documentation map/index and ownership before destructive cleanup.
