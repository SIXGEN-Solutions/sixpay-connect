# SIXPAY CONNECT — DA-5 Identity Linking

## 1. Scope

DA-5 separates the canonical SIXPAY user from the authentication identities
used to prove that user's identity.

Authoritative implementation branch:

```text
feat/hybrid-authentification-system
```

The `partner` module remains the golden implementation/structure reference.
Identity ownership remains entirely inside `backend/security`.

## 2. Existing implementation reviewed

Before DA-5:

- Local authentication persisted `security_local_users` and returned its Local
  subject as the authenticated subject.
- OIDC authentication validated bearer JWTs through the Resource Server and
  `OidcAuthenticationAdapter`.
- DA-4's temporary `SubjectExternalIdentityResolver` accepted the external OIDC
  `sub` directly as the SIXPAY subject.
- `AuthenticatedUser` / `SixpayPrincipal` already provided one mechanism-neutral
  principal shape.

That was sufficient for DA-1 to DA-4 but did not establish that a Local identity
and an OIDC identity belong to the same canonical SIXPAY person/account.

## 3. DA-5 data model

DA-5 introduces a canonical user table:

```text
security_user_accounts
────────────────────────────
id
username
normalized_username
email
status
created_at
updated_at
version
```

and an authentication-identity table:

```text
security_user_identities
────────────────────────────
id
user_id
identity_type
provider
provider_subject
created_at
updated_at
```

The relationship is:

```text
SIXPAY user A
    │
    ├── LOCAL identity
    │      provider = SIXPAY
    │      provider_subject = local-provider-subject
    │
    └── OIDC identity
           provider = exact trusted issuer URI
           provider_subject = OIDC sub
```

The canonical user is not assigned a single `authType`.

## 4. Canonical subject

To preserve the existing `SixpayPrincipal` API and avoid broad business-module
changes, DA-5 uses:

```text
security_user_accounts.id.toString()
```

as the canonical authenticated `subject`.

Therefore:

```text
LOCAL identity ──┐
                 ├──> user account A ──> subject = A.id
OIDC identity ───┘
```

No business module needs a Local/OIDC branch and no provider subject escapes the
security boundary.

## 5. Conservative linking policy

DA-5 explicitly chooses the banking-conservative policy:

```text
OIDC identity must already be linked
OR
it must be provisioned through a controlled administrative process.
```

There is no public auto-provisioning endpoint in DA-5.

There is no automatic linking by email.

There is no automatic linking by username.

A JWT that is cryptographically valid but whose `(issuer, sub)` pair is absent
from `security_user_identities` is rejected.

## 6. Provider key

For OIDC, the `provider` value is the exact trusted OIDC issuer URI (`iss`).

Examples:

```text
https://login.microsoftonline.com/<tenant>/v2.0
https://keycloak.example/realms/sixpay
https://example.okta.com/oauth2/default
```

The core model does not contain `ENTRA`, `KEYCLOAK`, `OKTA`, or `AUTH0` service
classes. The protocol boundary remains provider-neutral.

For Local authentication:

```text
identity_type = LOCAL
provider      = SIXPAY
```

## 7. Email changes at the IdP

OIDC identity lookup is performed only by:

```text
identity_type + issuer + provider_subject(sub)
```

Email is never used as an identity-linking key.

Therefore an IdP email change does not silently relink the user and does not
create a second SIXPAY account.

The canonical SIXPAY account email is administration-owned and may be updated
through a controlled user-management workflow independently.

## 8. Disabled users

Both authentication paths now enforce the canonical user-account status.

```text
security_user_accounts.status = DISABLED
```

causes authentication to fail whether the caller uses Local or OIDC.

For OIDC, an unlinked or disabled user is converted to an OAuth2 authentication
failure and the protected API remains inaccessible.

## 9. Local migration

Existing Local users are migrated without duplicating people:

1. each existing Local record creates one canonical `security_user_accounts`
   row using the existing Local UUID as the initial account UUID;
2. one `LOCAL / SIXPAY` identity is created for that account;
3. `security_local_users.user_id` is backfilled and made mandatory;
4. future Local authentication returns the canonical user-account UUID as its
   `subject`.

The Local credential table remains credential-specific and continues to own:

```text
password_hash
failed_attempts
locked_until
last_authenticated_at
local credential status
```

## 10. OIDC resolution

DA-5 replaces subject-only resolution with:

```text
OIDC JWT
  │
  ▼
ExternalIdentity(issuer, sub, username)
  │
  ▼
LinkedExternalIdentityResolver
  │
  ▼
FindLinkedIdentityPort
  │
  ▼
security_user_identities
  │
  ▼
security_user_accounts
  │
  ▼
AuthenticatedUser / SixpayPrincipal
```

`SubjectExternalIdentityResolver` is retained only as a deprecated class name
for source compatibility, but it now delegates to the linked-identity behavior;
it no longer permits subject-only authentication.

## 11. Authorization boundary

DA-5 changes identity ownership, not business authorization ownership.

Existing Local/JWT authority sources remain untouched in this lot to avoid
mixing DA-5 with DA-6.

DA-6 will make SIXPAY roles/permissions definitively user-account-owned and
independent from the authentication provider.

## 12. Database constraints

DA-5 enforces:

```text
unique canonical normalized username
unique canonical email when non-null
unique (identity_type, provider, provider_subject)
unique (user_id, identity_type, provider)
one Local credential record per canonical user
```

The second identity uniqueness rule means a user may have Local + OIDC, and may
also have identities from distinct OIDC providers, but cannot accidentally have
two identities for the same provider/type.

## 13. No-change boundary

No authentication-mode/provider dependency is introduced into:

```text
partner
payment
customer
accounting
reporting
incident
```

Those modules continue consuming `AuthenticatedUser` / `SixpayPrincipal`.

## 14. Tests

DA-5 proves:

- a pre-linked OIDC identity resolves to the canonical SIXPAY account;
- an unknown OIDC identity is rejected;
- a disabled canonical account is rejected;
- Local authentication produces the canonical account UUID as subject;
- a Local identity and an OIDC identity linked to the same account produce the
  exact same canonical subject;
- existing Resource Server behavior remains compatible.

## 15. Validation

From `backend/`:

```bash
mvn -pl security \
  -Dtest=LinkedExternalIdentityResolverTest,HybridIdentityConvergenceTest,LocalAuthenticationUserTest,LocalAuthenticationServiceTest,OidcAuthenticationAdapterTest,OidcAuthenticationProviderIT,SixpaySecurityAutoConfigurationTest \
  test

mvn -pl security -am test
mvn -pl bootstrap -am test
```

Frontend regression:

```bash
cd ../frontend
npm test
npm run build
```

## 16. Exit criteria

```text
[ ] canonical SIXPAY user is separate from authentication identities
[ ] one canonical user can have LOCAL and OIDC identities
[ ] Local and OIDC resolve to the same canonical subject
[ ] OIDC identity lookup uses issuer + sub, never email
[ ] unknown OIDC identities fail closed
[ ] disabled canonical users fail through both authentication paths
[ ] no automatic email linking exists
[ ] no automatic OIDC user provisioning exists
[ ] existing Local credential state remains credential-owned
[ ] no provider-specific business dependency exists
[ ] focused tests pass
[ ] backend regression passes
[ ] frontend regression passes
```

Next lot:

```text
DA-6 — SIXPAY-owned Authorization
```
