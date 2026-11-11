# SIXPAY CONNECT — AUTH-3
## LDAP Identity Linking & Canonical SIXPAY Account Resolution

### Status

```text
AUTH-3 — IMPLEMENTATION PREPARED; VALIDATION REQUIRED
```

### Baseline

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Baseline SHA: `250d3df3c050126232bbd6127cb74ecf1e4350c9`
- Prerequisite: AUTH-2 implementation complete
- Schema evolution: **NOT REQUIRED**
- Automatic identity provisioning: **NOT INCLUDED**

## Purpose

AUTH-3 reuses the canonical linking mechanism:

```text
AuthenticationIdentityType.LDAP
+ issuer / trust-domain
+ subject
    -> security_user_identities
    -> SixpayUserAccount
    -> SIXPAY roles / permissions
    -> AuthenticatedUser
```

LDAP proves identity. SIXPAY decides whether the identity is linked, whether
the account is active, and which roles/permissions apply.

## Existing model reused

`ExternalIdentity`, `ExternalIdentityResolver`, `LinkedExternalIdentityResolver`,
`FindLinkedIdentityPort`, `JpaLinkedIdentityAdapter`, `UserIdentity`,
`SixpayUserAccount`, `AuthenticatedUser` and `SpringSecuritySessionManager`
remain the canonical path.

No LDAP-specific account, authorization store or session model is introduced.

## Schema decision

`V700__security_baseline.sql` already permits `LDAP` and enforces:

```text
UNIQUE(identity_type, provider, provider_subject)
user_id -> security_user_accounts.id
```

No migration is required. The same subject under two trust domains remains two
distinct identities, while Local/OIDC/LDAP identities can reference the same
canonical `user_id`.

## Mandatory behavior

- linked LDAP identity + ACTIVE SIXPAY account -> canonical `AuthenticatedUser`;
- absent identity -> `ExternalIdentityNotLinkedException`;
- DISABLED SIXPAY account -> `SixpayUserDisabledException`;
- same subject under distinct trust domains -> distinct identity keys;
- Local/OIDC/LDAP links to one `user_id` -> same canonical user and authorities.

The existing `SpringSecuritySessionManager` already accepts
`AuthenticationMethod.LDAP`; AUTH-3 does not invent a new HTTP login endpoint.
The HTTP/session presentation belongs to the later runtime authentication lot.

LDAP groups never become SIXPAY authorities. Unknown LDAP identities are never
automatically linked by username, email, DN or display name.

## Validation

```bash
cd backend
mvn -pl security -Dtest=LdapCanonicalAuthenticationServiceTest,LinkedExternalIdentityResolverTest,HybridIdentityConvergenceTest test
mvn -pl security -am test
mvn -pl bootstrap -am test
mvn verify
```

From repository root:

```bash
py scripts/verify_master_prompt_input_manifest.py
py scripts/verify_repository_hygiene.py
py scripts/verify_documentation_final.py
py scripts/verify_baseline.py
```

No validation result is claimed until these commands complete with exit code 0.
