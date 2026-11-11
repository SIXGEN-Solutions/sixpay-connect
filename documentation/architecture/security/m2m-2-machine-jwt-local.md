# M2M-2 — Local JWT machine authentication (Security)

Reference revision: `7b937e78d29e70a8608bed9afca03a3bd072a9f2`.

This is a **local validation** overlay. It does not alter human LOCAL, LDAP or
OIDC login. This does **not** implement mTLS; the approved Payment contract
requires both OAuth2 client credentials and mTLS. Do not expose the payment API
without the bank's verified mTLS gateway. M2M-3 remains required for deployment.

## Existing identity and Partner boundaries preserved

The Keycloak signed client JWT uses `azp` (`sixpay-partner-test`) as the stable
machine subject. Security produces `AuthenticatedMachineIdentity` and an
`SCOPE_payment.initiate` authority only if the signed token validates, including
issuer, signature, expiry, audience `sixpay-api`, and `payment.initiate` scope.

The existing Security `security_partner_machine_identities` association then
resolves the subject to a Partner through the Bootstrap adapter. The existing
Payment `PartnerIdentityAlignmentService` rejects non-active or mismatching
Partners and checks the declared request `AppID`. No schema or partner business
policy is changed; test data mapping must be provisioned through authorized
administrative arrangements.

## IDE local settings

Start the existing Keycloak M2M-1 stack and ensure the realm was **reimported**
after the audience-mapper change. Start SIXPAY from IntelliJ with its existing
non-`dev` security configuration and append Spring profile `m2m-local`.
Set the following **local-only** environment variables (never commit secrets):

```text
SIXPAY_M2M_ENABLED=true
SIXPAY_M2M_ISSUER_URI=http://localhost:8180/realms/sixpay-m2m-local
SIXPAY_M2M_JWK_SET_URI=http://localhost:8180/realms/sixpay-m2m-local/protocol/openid-connect/certs
SIXPAY_M2M_AUDIENCE=sixpay-api
```

The issuer must match the token's `iss` claim EXACTLY, even if JWKS is fetched
from a different URL. Docker-to-host loopback networking is not configured in
this lot; the URL above is for running the backend from the local IDE.
The standalone `dev` security-filter-chain override is **not** wired for this
lot: use an existing profile which uses `SixpaySecurityAutoConfiguration`.
M2M protection is disabled by default. When enabled, an absent issuer, JWKS
URL, or audience stops startup instead of silently weakening the checks.

## Verification order (commands NOT executed by patch)

From repository root:

```powershell
cd infrastructure/docker/m2m-keycloak
py -m unittest discover -s tests -p "test_*.py" -v
docker compose config
docker compose down
docker compose up -d --force-recreate
py verify_token.py --negative
```

From `backend`:

```powershell
mvn -pl security -am "-Dtest=M2mJwtAuthenticationFilterTest,SecurityContextCurrentMachineIdentityProviderTest" -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl security -am test
mvn verify
```

Negative tests must reject unsigned/expired/wrong issuer/wrong audience tokens,
missing scope, unknown client/disabled partner, and JWT-`AppID` mismatch.
The `M2mJwtAuthenticationFilterTest` mocks the JWT decoder to isolate filter
logic; **it does not prove cryptographic validation or Payment end-to-end flow**.
Integration tests must use a real signed Keycloak token and verify decoder
validation on the running application.

M2M-3 must implement verified mTLS gateway and certificate-to-client linkage
before production activation. The local Keycloak self-audience mapper for
introspection is a test accommodation; `sixpay-api` is the separate local API
audience. The production audience requires explicit environment approval.
