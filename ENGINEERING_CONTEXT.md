# ENGINEERING_CONTEXT.md

> Mandatory entry point for engineers and AI coding assistants contributing to
> SIXPAY CONNECT.

# Repository

**Project:** SIXPAY CONNECT  
**Organization:** SIXGEN-Solutions  
**Primary implementation branch:** `'feat/integration-contracts'`  
**Current delivery focus:** Phase 5, Lot 5.4.1 — Payment account, opposition and funds.

# Source of Truth

1. `'feat/integration-contracts'`
2. `documentation/architecture/`
3. `documentation/requirements/`
4. `documentation/contracts/`
5. `documentation/ai/`
6. Engineering assets
7. `ENGINEERING_CONTEXT.md`

# Rules

- `partner` remains the golden business-module reference.
- Payment owns its banking application ports.
- Provider DTOs and mappings remain in Payment infrastructure.
- `integration` contains provider-neutral transport and resilience only.
- Account verification and funds checks are read-only operations.
- Posting and reversal must not be implemented or simulated in Lot 5.4.1.
- Unknown provider codes are technical invalid responses, not business rejects.
- Production banking traffic requires OAuth2 client credentials and mTLS.
- No direct repository writes are performed by the AI delivery workflow.

# Definition of Done

- account existence, active status and opposition are mapped to Payment evidence;
- balance and funds eligibility are mapped to Payment evidence;
- OAuth2, mTLS, headers, timeout and retry are configurable;
- provider responses are validated;
- business rejection is not retried;
- tests, contract and runbook are aligned;
- CI succeeds.
