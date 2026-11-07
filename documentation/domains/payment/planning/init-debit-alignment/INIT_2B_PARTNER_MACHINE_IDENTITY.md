# INIT-2B — Partner machine identity

## Reference revision

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Branch: `feat/repository-baseline-consolidation-cucumber`
- SHA: `366556155a82022e5d76b47b4aca2ad50daf41f8`

## Goal

Establish a strict separation between:

- human SIXPAY users (`AuthenticatedUser`, Local/OIDC/LDAP);
- Partner machine callers used for M2M payment initiation.

Target flow:

```text
transport authentication
    -> trusted machine subject
    -> Security machine-identity link
    -> canonical Partner partnerIdentifier
    -> Partner public resolution
    -> Payment comparison with declared AppID
```

## Ownership

Security owns the technical machine identity and its durable link to a Partner
business identifier.

Partner owns `partnerIdentifier`, Partner status and authorization to accept
new transactions.

Payment owns the consistency decision between authenticated Partner and the
Partner declared by the request.

Bootstrap owns only cross-module composition.

## Schema

Security adds `security_partner_machine_identities`.

The table deliberately stores `partner_identifier` rather than a foreign key to
Partner persistence. Security must not access Partner tables or repositories.
Runtime composition resolves the identifier through Partner's public application
surface.

## Human/M2M separation

`CurrentUserProvider` remains the surface for human SIXPAY users.

`CurrentMachineIdentityProvider` is the distinct surface for M2M callers.
`AuthenticatedUser` is explicitly rejected as a machine principal.

## Contract decision still required

INIT-2B does not modify the active physical Payment contract.

At this revision, the active contract still maps `AppID` to
`X-TresorPay-App-Id` and forbids it in the body. The INIT-1/INIT-2A target
decision states instead that the physical `AppID` request field is the canonical
`partnerIdentifier`.

That public-contract/security mismatch requires a separate explicit human
approval before the physical contract and request DTO are changed.

## Transport authentication

INIT-2B does not invent a new authentication protocol. Existing JWT,
subscription-key/API-key and mTLS processing remain governed by their current
contract/configuration status. The new surface consumes only the trusted
technical subject produced by the selected transport authentication.

## Status

```text
INIT-2B — INTERNAL MACHINE IDENTITY LINK IMPLEMENTED
PUBLIC APPID CONTRACT ALIGNMENT STILL REQUIRES HUMAN APPROVAL
```


## Corrective composition rule

Payment no longer depends directly on `CurrentMachineIdentityProvider`.

The module boundary is:

```text
PaymentCommandController
    -> AuthenticatedPartnerCallerPort        [owned by Payment]
    -> Bootstrap adapter
    -> CurrentMachineIdentityProvider        [owned by Security]
```

This keeps Payment module tests independent from servlet Security
auto-configuration while preserving the M2M identity boundary. The empty
Payment fallback port is composition-safe for isolated module contexts and
never authenticates a caller; protected API invocation still fails when no
authenticated subject is supplied.


## Final INIT-2B alignment

The effective identity invariant is now:

```text
authenticated machine subject
    -> Security machine identity link
    -> Partner.partnerIdentifier
    == request.AppID
```

`X-TresorPay-App-Id` remains independent transport metadata. It may equal
`AppID`, but equality is not required.

`LoginName` is not an authentication identity and no longer has to equal the
authenticated machine subject.

The Payment HTTP boundary invokes `PartnerIdentityAlignmentService` before
delegating to the Payment initiation use case.

## Closure status

```text
INIT-2B — IMPLEMENTATION COMPLETE, VALIDATION REQUIRED
```
