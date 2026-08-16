# ENGINEERING_CONTEXT.md

> **Purpose**
>
> Mandatory entry point for engineers and AI coding assistants contributing to SIXPAY CONNECT.

---

# Repository

**Project:** SIXPAY CONNECT

**Organization:** SIXGEN-Solutions

**Primary implementation branch:** `'feat/sixpay-pilot-hybrid-consolidated'`

**Current delivery focus:** Dual Authentication — Local + OIDC.

---

# Repository Philosophy

SIXPAY CONNECT follows Documentation-as-Code. Requirements, architecture,
contracts, implementation, infrastructure, tests and AI assets evolve together.

---

# Source of Truth

1. `''feat/sixpay-pilot-hybrid-consolidated'`
2. `documentation/architecture/`
3. `documentation/requirements/`
4. `documentation/contracts/`
5. `documentation/ai/`
6. Engineering assets
7. `ENGINEERING_CONTEXT.md`

When sources conflict, the higher-priority source prevails.

---

# Repository Navigation

| Looking for | Location |
|---|---|
| Business requirements | `documentation/requirements/` |
| Architecture | `documentation/architecture/` |
| Integration landscape | `documentation/architecture/integration/` |
| API and integration contracts | `documentation/contracts/` |
| Backend | `backend/` |
| Shared integration foundation | `backend/integration/` |
| Infrastructure | `infrastructure/` |
| Deployment | `deployment/` |
| Scripts | `scripts/` |

---

# Integration Change Gate

Before changing an integration:

1. identify producer and consumer;
2. identify owning module and contract owner;
3. classify synchronous or asynchronous;
4. reference a published contract or mark it `TO_DEFINE`;
5. define security, errors and testing;
6. update integration architecture documentation;
7. preserve module boundaries and anti-corruption layers;
8. keep co-deployed internal calls in-process unless a deployment decision says otherwise.

The `partner` module remains the golden business-module reference.

The `integration` module contains only provider-neutral HTTP, correlation,
resilience, observability, serialization, Kafka, DLQ and consumer-idempotency
support. Provider payloads and mappings stay in the owning domain.

---

# Engineering Workflow

```text
Requirements -> Architecture -> Contracts -> Implementation
-> Tests -> Documentation -> Validation
```

---

# AI Working Agreement

AI SHALL read existing implementation, reuse patterns, preserve contracts,
synchronize tests and documentation, and reuse `backend/integration` only for
provider-neutral concerns.

AI SHALL NOT invent structures, duplicate business logic, introduce an
omnipotent Core Banking service, move provider mappings into `integration`, or
blindly retry financial commands with unknown outcomes.

---

# Definition of Done

- implementation complete;
- tests pass;
- contracts valid;
- documentation updated;
- conventions respected;
- ownership, security, errors and testing documented;
- provider logic remains in its owning domain;
- CI succeeds.

---

# Final Rule

**Consistency takes precedence over creativity.**
