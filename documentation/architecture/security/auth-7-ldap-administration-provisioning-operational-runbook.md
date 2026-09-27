# SIXPAY CONNECT — AUTH-7
## LDAP Administration / Provisioning & Operational Runbook

### Status

`AUTH-7 — ADMINISTRATION WORKFLOW BASELINE PREPARED; VALIDATION REQUIRED`

Baseline SHA: `0da070eb22166f45cacc52d11c58ffa41d920491`.

This lot does not add a frontend screen. The human AUTH-7 decision
explicitly includes the LDAP administrative linking operation in the physical
`security-user-administration-api-v1` contract. The contract remains
`PENDING_APPROVAL / REFERENCE_ONLY / codeGenerationAllowed=false`; adding this
operation does not authorize contract-driven code generation.

## Canonical workflow

```text
user exists in trusted LDAP/AD
        ↓
canonical SIXPAY account exists
        ↓
LDAP identity is linked by:
  identityType = LDAP
  provider = configured stable trust-domain
  providerSubject = immutable LDAP subject (objectGUID)
        ↓
SIXPAY-owned roles / permissions are assigned to the canonical account
        ↓
LDAP credential verification succeeds
        ↓
linked canonical SIXPAY account is ACTIVE
        ↓
authentication is authorized
```

LDAP proves identity. SIXPAY owns authorization.

## Administrative creation / linking procedure

1. Confirm the employee/user exists in the trusted directory.
2. Obtain the immutable directory subject configured by AUTH-1/AUTH-2
   (`objectGUID` for the approved Active Directory profile).
3. Create or select the canonical `SixpayUserAccount`.
4. Assign only approved SIXPAY roles/permissions to that canonical account.
5. Link the LDAP external identity to the account using:
   - identity type `LDAP`;
   - configured LDAP trust-domain;
   - stable immutable subject.
6. Verify the account is `ACTIVE`.
7. Validate authentication using the LDAP provider.

Never link automatically by email, username, display name, DN or LDAP group.

## DN / username changes

DN and username are directory attributes, not the durable identity key.

If the user's DN or login name changes while the LDAP stable subject and trust
domain remain unchanged, the existing SIXPAY identity link remains valid.
No relink is required.

A change of trust-domain or stable subject is a distinct external identity and
must follow the reviewed unlink/relink procedure.

## Unlink / relink

Unlinking removes only the selected external identity link. It does not delete
the canonical SIXPAY account and does not alter its roles/permissions.

Relinking requires the same explicit administrative workflow as first linking.
The unique `(identity_type, provider, provider_subject)` constraint prevents one
LDAP identity from being linked to multiple SIXPAY accounts.

LOCAL identities are not processed by the external unlink path.

## SIXPAY account disablement

`SixpayUserAccount.status = DISABLED` is an immediate SIXPAY authorization
barrier. Even if LDAP credentials remain valid, canonical identity resolution
must reject the disabled SIXPAY account.

Re-enabling the account does not recreate or infer LDAP links; existing durable
links remain governed by their persisted identity record.

## User disabled or removed in LDAP

LDAP account state is checked by the LDAP authentication adapter before
canonical SIXPAY identity resolution.

Disabled, locked, expired or password-expired directory accounts fail closed.
A removed user also fails LDAP lookup/authentication and therefore cannot create
a SIXPAY session.

SIXPAY does not automatically delete the canonical account or identity link
when the directory user disappears. Administrative cleanup is explicit and
audited so that transient directory outages are never mistaken for deletion.

## Linking audit

Administrative link/unlink operations produce append-only Security audit
events:

- `IDENTITY_LINKED`;
- `IDENTITY_UNLINKED`.

The audit records the administrative actor, target SIXPAY user, provider/trust
domain where applicable, operation type and UTC timestamp.

Passwords, LDAP bind credentials, DN values, tokens and directory secrets must
not be written to audit detail or application logs.

## LDAP outage runbook

When LDAP is unavailable:

1. confirm the configured LDAP capability is enabled;
2. inspect health/technical metrics without exposing credentials or user data;
3. verify DNS/network/TLS reachability to configured LDAPS endpoints;
4. distinguish certificate failure, timeout and directory unavailability;
5. do not bypass LDAP authentication or auto-link another identity;
6. do not convert LDAP groups into SIXPAY authorities;
7. restore the directory/TLS/network dependency;
8. re-test with a controlled account;
9. record the operational incident according to the existing incident process.

LOCAL or OIDC may continue only when they are independently enabled and
authorized for the affected user. LDAP failure never silently changes a user's
authentication method.

## Health and observability

LDAP observability must expose dependency state, not identity data.

Allowed signals include:

- provider enabled/disabled;
- endpoint availability as an aggregate status;
- connect/read/overall timeout failures;
- TLS/certificate handshake failures;
- authentication success/failure counters with bounded labels;
- failover attempt/success counters;
- latency distributions without usernames, DNs, subjects or account IDs.

Forbidden observability data include:

- password/service-account secret;
- user DN;
- objectGUID/provider subject;
- username/email as metric labels;
- LDAP search result payloads;
- session/token material.

A health endpoint must never expose LDAP credentials, full endpoint secrets or
directory user data.

## Contract boundary

The internal Security User Administration physical contract now documents the
explicit ADMIN-only LDAP linking operation:

```text
POST /internal/api/v1/administration/users/{userId}/identities/ldap
DELETE /internal/api/v1/administration/users/{userId}/identities/{identityId}
```

The POST accepts only the durable LDAP link coordinates:

```json
{
  "trustDomain": "regionale-ldap",
  "stableSubject": "<immutable objectGUID>"
}
```

DN, username, email and LDAP groups are deliberately excluded from the durable
link payload.

The contract remains:

```text
lifecycleStatus: ACTIVE_MVP
approvalStatus: PENDING_APPROVAL
generationPolicy: REFERENCE_ONLY
codeGenerationAllowed: false
```

The human AUTH-7 decision authorizes this contract amendment and matching
implementation only. It does not authorize contract-driven generation.

## Database

No schema evolution is required.

`security_user_identities` already supports `LDAP` and enforces:

```text
UNIQUE(identity_type, provider, provider_subject)
UNIQUE(user_id, identity_type, provider)
```

The durable identity remains `(LDAP, trust-domain, stable subject)`.

## HTTP administration closure

An ADMIN can now invoke the LDAP link workflow through the Administration
boundary. The controller delegates to the existing Security-owned
`linkLdapIdentity(...)` use case. Unlink remains generic for OIDC and LDAP.

No LDAP credential verification is performed by the administration endpoint:
credential verification remains the responsibility of the LDAP authentication
provider at login time. Linking is an explicit administrative association of a
trusted stable directory identity with a canonical SIXPAY account.

## Validation

Targeted:

```bash
cd backend
mvn -pl security -Dtest=SecurityUserAdministrationServiceTest,LdapCanonicalAuthenticationServiceTest,ActiveDirectoryLdapAuthenticationAdapterTest test
mvn -pl security -am test
mvn -pl administration,bootstrap -am test
mvn verify
```

Repository root:

```bash
py scripts/verify_master_prompt_input_manifest.py
py scripts/verify_repository_hygiene.py
py scripts/verify_documentation_final.py
py scripts/verify_baseline.py
```

No validation result is claimed until commands finish with exit code `0`.
