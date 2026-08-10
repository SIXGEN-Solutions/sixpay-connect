# SIXPAY CONNECT — DA-0 Review Checklist

Use this checklist during architecture review before starting DA-1.

## Repository

- [ ] `ENGINEERING_CONTEXT.md` reviewed first.
- [ ] Authoritative branch is `feat/hybrid-authentification-system`.
- [ ] `partner` remains the golden business-module reference.
- [ ] Security ownership remains under `backend/security`.

## Baseline

- [ ] Existing `standalone`, `local`, `oidc` modes documented.
- [ ] Existing exclusive-mode behavior documented.
- [ ] Existing Local auth endpoints documented.
- [ ] Existing OIDC integration documented.
- [ ] Existing mechanism-neutral route guard documented.

## Target

- [ ] `standalone` classified as dev/demo-only.
- [ ] `local.enabled` capability defined.
- [ ] `oidc.enabled` capability defined.
- [ ] Local-only configuration accepted.
- [ ] OIDC-only configuration accepted.
- [ ] Hybrid configuration accepted.
- [ ] Neither-enabled production configuration rejected.

## Separation of responsibilities

- [ ] Authentication is owned by security.
- [ ] Authorization remains SIXPAY-owned.
- [ ] OIDC provider proves identity but does not own SIXPAY business authorization.
- [ ] Local and OIDC converge to one canonical SIXPAY identity.
- [ ] Business modules do not branch on authentication method.
- [ ] Business modules do not import provider-specific contracts.

## No-change boundary

- [ ] No auth-mode change in `partner`.
- [ ] No auth-mode change in `payment`.
- [ ] No auth-mode change in `customer`.
- [ ] No auth-mode change in `accounting`.
- [ ] No auth-mode change in `reporting`.
- [ ] No auth-mode change in `incident`.

## DA-0 closure

- [ ] Architecture approved.
- [ ] Ownership approved.
- [ ] Conformance gaps assigned to later DA lots.
- [ ] No production code modified.
- [ ] Ready to start DA-1 — Unified SIXPAY Principal.
