# SIXPAY CONNECT — Administration Golden Test Coverage

## Phase

```text
Dual Authentication — Local + OIDC
User Administration CRUD
Sous-lot 4 — Tests / documentation
```

## Source of truth

The authoritative implementation revision is supplied by the task invocation or selected execution environment. `ENGINEERING_CONTEXT.md` remains the mandatory engineering entry point and `partner` remains the golden business-module reference.

Administration is no longer a module shell. The implemented responsibility split is:

```text
backend/administration
    HTTP administration boundary

backend/security
    canonical SIXPAY user model
    authentication identities
    Local credentials
    SIXPAY authorization
    operational security audit
    administration use cases and persistence
```

Overall classification:

```text
ADMINISTRATION = COVERED
```

## Evidence

`SecurityUserAdministrationServiceTest` covers canonical creation, normalization, role/permission propagation, Local password hashing and minimum length, update, audit creation, and delete audit ordering.

`SecurityUserAdministrationControllerTest` covers anonymous rejection, non-ADMIN rejection, ADMIN list access, create 201 + Location, Bean Validation, actor propagation, update, enable/disable and delete 204.

`IntegrationSecurityUserSeederTest` covers empty-database seed, admin/manager/auditor/partner profiles, Local provisioning, idempotency, partial completion and deterministic technical identities.

`SecurityUserAdministrationService.spec.ts` covers the frontend HTTP mapping for CRUD and protects existing Local/OIDC administration endpoints against regression.

## Architectural invariants

```text
SIXPAY User != Authentication Identity
IdP proves identity
SIXPAY owns authorization
Local and OIDC may represent the same canonical user
business modules do not branch on LOCAL vs OIDC
Administration API is ADMIN-only
passwords/tokens/secrets are never written to audit
```

## Validation

```bash
cd backend
mvn -pl security -Dtest=SecurityUserAdministrationServiceTest test
mvn -pl administration -am -Dtest=SecurityUserAdministrationControllerTest test
mvn -pl bootstrap -am -Dtest=IntegrationSecurityUserSeederTest test
mvn -pl tests -Dtest=BackendGoldenCoverageGateTest test
mvn clean package
```

```bash
cd frontend
npm test
npm run build
```

The sub-lot is closed when all commands are green.
