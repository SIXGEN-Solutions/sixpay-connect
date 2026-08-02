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

**Primary implementation branch:** `'feat/backend-payment-tresorapi`

---

# Repository Philosophy

SIXPAY CONNECT follows a **Documentation-as-Code** approach.

Requirements, architecture, contracts, implementation, infrastructure, tests, and AI assets evolve together.

The repository is the single source of engineering knowledge.

---

# Source of Truth

Unless explicitly instructed otherwise, information shall be interpreted using the following precedence:

1. **`'feat/backend-payment-tresorapi`** (latest implementation)
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
| API & Integration contracts | `documentation/contracts/` |
| AI engineering guidance | `documentation/ai/` |
| Backend implementation | `backend/` |
| Frontend implementation | `frontend/` |
| Infrastructure | `infrastructure/` |
| Deployment | `deployment/` |
| Automation & scripts | `scripts/` |

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

## AI SHALL NOT

- Invent repository structures.
- Introduce alternative architectures.
- Rename packages or modules without approval.
- Duplicate business logic.
- Break published contracts.
- Generate placeholder or incomplete production code.
- Ignore existing engineering standards.

---

# Definition of Done

A change is complete only when:

- Implementation is complete.
- Tests pass.
- Contracts remain valid.
- Documentation is updated.
- Repository conventions are respected.
- CI succeeds.

---

# Final Rule

**Consistency takes precedence over creativity.**

Every contribution—whether produced by a human or an AI—must make the repository more maintainable, more coherent, and better documented than it was before.