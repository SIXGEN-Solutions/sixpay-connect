# SIXPAY CONNECT — AUTH-6
## LDAP Operational Resilience & Security Validation

### Status
`AUTH-6 — VALIDATION BASELINE PREPARED`

Baseline SHA: `725c1985b60982e809dcbc18f1ef382619753191`.

AUTH-6 changes no production authentication behavior, endpoint, schema or LDAP login UX.

## Automated repository coverage
- LDAPS-only configuration and bounded connect/read/overall timeouts;
- stable objectGUID subject and malformed/missing subject rejection;
- linked active LDAP identity success;
- unlinked identity and disabled SIXPAY account rejection;
- trust-domain mismatch rejection;
- SIXPAY-owned authorities only, never LDAP-group-derived authorities;
- provider capability matrix and zero-provider secured-runtime rejection;
- Local/OIDC regressions and provider coexistence;
- absence of LDAP provider internals in business modules;
- absence of application logging in the LDAP adapter.

## Session invariant
A SIXPAY session may be created only after LDAP credential verification and
canonical identity linking both succeed. Logout/session invalidation remains
provider-neutral.

## Environment-backed evidence still mandatory
The following cannot honestly be proven by static/mock-only tests:
1. real LDAPS authentication success;
2. bad credential rejection by the directory;
3. unavailable LDAP endpoint fail-closed behavior;
4. real connect/read/overall timeout;
5. invalid/untrusted/expired certificate rejection;
6. hostname/SAN mismatch rejection;
7. ordered endpoint failover.

They require a controlled LDAP/LDAPS environment with test-only runtime
credentials and certificates. No production credential/private key/directory
data may enter Git.

## Commands
```bash
cd backend
mvn -pl security -Dtest=AuthenticationCapabilitiesPropertiesTest,AuthenticationProviderPolicyValidatorTest,ActiveDirectoryLdapAuthenticationAdapterTest,LdapCanonicalAuthenticationServiceTest,LdapSensitiveLoggingArchitectureTest test
mvn -pl bootstrap -am -Dtest=SecurityAuthenticationConfigurationArchitectureTest,LdapBusinessModuleIsolationArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl security -Dtest=LocalAuthenticationSessionIT,OidcAuthenticationProviderIT,HybridAuthenticationIT test
mvn -pl security -am test
mvn verify
```

Repository root:
```bash
py scripts/verify_master_prompt_input_manifest.py
py scripts/verify_repository_hygiene.py
py scripts/verify_documentation_final.py
py scripts/verify_baseline.py
```

No validation result is claimed before execution.
