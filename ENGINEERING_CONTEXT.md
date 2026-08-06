# ENGINEERING_CONTEXT.md

> **Purpose**
>
> This document is the mandatory entry point for every engineer and AI coding assistant contributing to SIXPAY CONNECT.
>
> It explains **how to work with the repository**, not how the system is implemented. Architecture, requirements, contracts, and implementation details are maintained in their respective locations.

---

# Repository

**Project:** SIXPAY CONNECT

**Organization:** SIXGEN-Solutions

**Primary implementation branch:** `'feat/integration-contracts'`

**Current delivery focus:** Phase 5 — Integrations, Lot 5.0 — Integration inventory and contracts.

---

# Repository Philosophy

SIXPAY CONNECT follows a **Documentation-as-Code** approach.

Requirements, architecture, contracts, implementation, infrastructure, tests, and AI assets evolve together.

The repository is the single source of engineering knowledge.

---

# Source of Truth

Unless explicitly instructed otherwise, information shall be interpreted using the following precedence:

1. **`'feat/integration-contracts'`** (latest implementation)
2. **`documentation/architecture/`**
3. **`documentation/requirements/`**
4. **`documentation/contracts/`**
5. **`documentation/ai/`**
6. Engineering assets (AI manifests, generation contracts, prompts, technology matrices)
7. **ENGINEERING_CONTEXT.md**

When sources conflict, the higher-priority source prevails.

---

# Repository Navigation

| Looking for | Location |
|-------------|----------|
| Business requirements | `documentation/requirements/` |
| Architecture | `documentation/architecture/` |
| Integration landscape and flow ownership | `documentation/architecture/integration/` |
| API & Integration contracts | `documentation/contracts/` |
| AI engineering guidance | `documentation/ai/` |
| Backend implementation | `backend/` |
| Frontend implementation | `frontend/` |
| Infrastructure | `infrastructure/` |
| Deployment | `deployment/` |
| Automation & scripts | `scripts/` |

---

# Integration Change Gate

Before implementing or changing an integration, contributors SHALL:

1. identify the producer and consumer;
2. identify the owning module and contract owner;
3. classify the interaction as synchronous or asynchronous;
4. reference the published contract or explicitly mark it `TO_DEFINE`;
5. define security, error handling and test mode;
6. update `documentation/architecture/integration/`;
7. preserve business-module ports and anti-corruption boundaries;
8. avoid converting internal modular-monolith calls to HTTP or Kafka without an approved deployment-boundary decision.

The `partner` module remains the golden reference for business-module implementation quality and package structure.

---

# Engineering Workflow

Every feature should follow this lifecycle:

```
Requirements
    ↓
Architecture
    ↓
Contracts
    ↓
Implementation
    ↓
Tests
    ↓
Documentation
    ↓
Validation
```

No implementation is complete until documentation, contracts, and tests are consistent.

---

# AI Working Agreement

## AI SHALL

- Read the existing implementation before generating code.
- Reuse existing architecture and implementation patterns.
- Respect module boundaries and published contracts.
- Keep tests and documentation synchronized with code.
- Prefer extending existing components over creating new ones.
- Produce production-ready code.
- Consult `documentation/architecture/integration/` before changing integration code.
- Treat `TO_DEFINE` contracts as blockers, not as permission to invent provider payloads.

## AI SHALL NOT

- Invent repository structures.
- Introduce alternative architectures.
- Rename packages or modules without approval.
- Duplicate business logic.
- Break published contracts.
- Generate placeholder or incomplete production code.
- Ignore existing engineering standards.
- Introduce a generic omnipotent `CoreBankingService`.
- Move provider-specific business mappings into the transverse `integration` module without an approved reuse case.

---

# Definition of Done

A change is complete only when:

- Implementation is complete.
- Tests pass.
- Contracts remain valid.
- Documentation is updated.
- Repository conventions are respected.
- Integration ownership, security, error policy and test mode are documented.
- CI succeeds.

---

# Final Rule

**Consistency takes precedence over creativity.**

Every contribution—whether produced by a human or an AI—must make the repository more maintainable, more coherent, and better documented than it was before.
