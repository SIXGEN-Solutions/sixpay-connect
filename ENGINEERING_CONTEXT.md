# ENGINEERING_CONTEXT.md

> **Purpose**
>
> Mandatory entry point for every engineer and AI coding assistant contributing
> to SIXPAY CONNECT.

---

# Repository

**Project:** SIXPAY CONNECT

**Organization:** SIXGEN-Solutions

**Primary implementation branch:** `'feat/integration-contracts'`

**Current delivery focus:** Phase 5 — Integrations, Lot 5.2 — TresorPay consolidation.

---

# Source of Truth

1. `'feat/integration-contracts'`
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
| External contracts | `documentation/contracts/external/` |
| TresorPay onboarding | `documentation/onboarding/tresorpay/` |
| TresorPay runbooks and stubs | `documentation/runbooks/tresorpay/`, `documentation/stubs/tresorpay/` |
| Backend | `backend/` |
| Shared integration foundation | `backend/integration/` |

---

# Integration Change Gate

Before changing an integration:

1. identify producer, consumer and contract owner;
2. classify synchronous or asynchronous;
3. reference a versioned contract;
4. define security, errors, replay and test mode;
5. preserve module boundaries and anti-corruption mappings;
6. update architecture and onboarding documentation.

The `partner` module remains the golden business-module reference.

The `integration` module contains only provider-neutral capabilities.
TresorPay payloads, guards and mappings remain in Payment infrastructure.

---

# TresorPay Baseline

Inbound Payment initiation uses:

- HTTPS and production mTLS;
- OAuth2 client-credentials JWT;
- audience `sixpay-payment-api`;
- scope `payment.initiate`;
- authenticated partner identity matching `LoginName`;
- `Idempotency-Key`;
- correlation ID;
- timestamp and nonce replay protection;
- callback-host allowlist;
- per-partner rate limiting.

API key support is disabled by default and exists only as a configurable
compatibility mechanism.

Callbacks use HTTPS/mTLS and detached RS256 JWS with a mandatory `kid`.

Temporary in-memory nonce and rate-limit components must be replaced before
horizontal production deployment.

---

# Engineering Workflow

```text
Requirements -> Architecture -> Contracts -> Implementation
-> Tests -> Documentation -> Validation
```

---

# AI Working Agreement

AI SHALL reuse existing Payment orchestration and idempotency, keep provider
logic in Payment infrastructure, use the shared integration foundation, and
keep tests and contracts synchronized.

AI SHALL NOT move TresorPay DTOs into the domain, put API keys in request bodies,
blindly retry financial operations, expose sensitive error details, or place
private keys in Git.

---

# Definition of Done

- implementation complete;
- contract and simulated-client tests pass;
- mTLS/JWT/API-key policy is configurable;
- replay, idempotency and rate limiting are covered;
- audit contains no sensitive payload;
- onboarding documentation is current;
- CI succeeds.

---

# Final Rule

**Consistency takes precedence over creativity.**
