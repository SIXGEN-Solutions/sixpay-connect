# SIXPAY CONNECT — AUTH-4
## Multi-provider Capability & Runtime Configuration

### Status

```text
AUTH-4 — IMPLEMENTED; FINAL VALIDATION REQUIRED
```

### Baseline

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Baseline SHA: `4a478bfb9d41db32ac3e5c3c47ba5376c5a2f133`
- Capability owner: Security
- Physical runtime configuration owner: Bootstrap
- New authentication profile combinations: **NONE**

## Capability matrix

Allowed:

```text
LOCAL
OIDC
LDAP
LOCAL + OIDC
LOCAL + LDAP
OIDC + LDAP
LOCAL + OIDC + LDAP
```

Invalid:

```text
LOCAL=false
OIDC=false
LDAP=false
```

Security enforces the non-empty invariant at startup when the runtime authentication provider policy requires a provider. Technical runtimes such as `standalone` may intentionally run with no human authentication provider.

## Runtime configuration

LDAP stays under:

```text
sixpay.security.authentication.ldap.*
```

Bootstrap owns physical values through one reusable fragment:

```text
config/security/ldap-common.yml
```

Sensitive values are runtime injected. In particular,
`SIXPAY_LDAP_SERVICE_ACCOUNT_PASSWORD` has no repository default.

## Profiles

No combinatorial profile matrix is introduced. Existing profiles remain the
composition surface and may toggle LDAP through `SIXPAY_LDAP_ENABLED`.

LDAP beans remain guarded by the existing `@ConditionalOnProperty`, so LDAP
infrastructure is not started when `ldap.enabled=false`.

## Validation

```bash
cd backend
mvn -pl security -Dtest=AuthenticationCapabilitiesPropertiesTest test
mvn -pl bootstrap -Dtest=SecurityAuthenticationConfigurationArchitectureTest,RuntimeProfileConfigurationArchitectureTest test
mvn -pl security,bootstrap -am test
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
