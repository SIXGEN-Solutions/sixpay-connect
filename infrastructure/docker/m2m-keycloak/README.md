# SIXPAY M2M-1 — Keycloak local (Client Credentials)

This folder is a **standalone local development stack**. It does not start or
configure the SIXPAY backend, and it does not alter LOCAL, LDAP or OIDC **USER**
authentication. M2M-1 demonstrates **token issuance**, not end-to-end payment
initiation. mTLS and the SIXPAY-side M2M JWT security chain belong to later
lots and are not bypassed here.

## Prerequisites

- Docker Desktop / Engine with Docker Compose v2
- Python 3 (stdlib only, for the verification script)
- Windows PowerShell or a POSIX shell

## Configure (do not commit secrets)

From `infrastructure/docker/m2m-keycloak`:

```powershell
Copy-Item .env.example .env
```

Edit `.env`: provide strong, non-production values for
`KC_BOOTSTRAP_ADMIN_PASSWORD` and `SIXPAY_M2M_TEST_CLIENT_SECRET`.
The project `.gitignore` already excludes `.env`. The realm import uses an
environment placeholder for the client secret, not a committed credential.
Never share tokens or client secrets in logs or screenshots.

## Start and verify

```powershell
docker compose config
docker compose up -d
docker compose ps
docker compose logs keycloak
py verify_token.py --negative
```

Keycloak Admin Console: http://localhost:8180/admin

Realm: `sixpay-m2m-local`.
Client: `sixpay-partner-test` (confidential service account, no user login).
Optional client scope: `payment.initiate`.

The verifier fetches a token using `client_credentials` and explicitly requests
`payment.initiate`. It then asks Keycloak's token introspection endpoint whether
the issued token is active and contains the required scope. It also checks that
wrong client credentials are rejected. **No token or secret is printed.**

To use another port, set `SIXPAY_M2M_KEYCLOAK_PORT` in `.env` and call:

```powershell
py verify_token.py --base-url http://127.0.0.1:8180 --negative
```

Update `--base-url` to the selected local port. The verifier only accepts
loopback HTTP(S) URLs; never use it with live credentials.

## Postman / CLI / Swagger

Postman OAuth 2.0: grant `Client Credentials`;
token URL `http://127.0.0.1:8180/realms/sixpay-m2m-local/protocol/openid-connect/token`;
client `sixpay-partner-test`; authentication `client_secret_basic`;
client secret from `.env`; scope `payment.initiate`.

The issued token can be inspected **locally** in Postman; it cannot yet be
used to prove SIXPAY's `/v1/payments/initiate` security works. That requires a
separate M2M JWT converter and mTLS transport trust, which M2M-1 does not add.
For Swagger, the token may be acquired through Postman; the API-side mTLS gate
remains mandatory under the approved contract.

## Local token introspection audience

The local demonstration client `sixpay-partner-test` introspects its own access token in `verify_token.py`. Keycloak 26 rejects introspection when the caller is
not a token audience. The realm therefore adds the **local-test-self-audience**
mapper on this client, including `sixpay-partner-test` in the **access token**
audience. This is **only** a local test accommodation; it does not define the
production SIXPAY API audience or replace API-side JWT validation.

The realm is imported only when it does not already exist. To apply changes to
this disposable Keycloak stack, run `docker compose down` and
`docker compose up -d --force-recreate` from this directory. Never use `down -v`
against other Docker stacks.

## Diagnostics and limitations

- `invalid_client`: check the secret and restart with a fresh realm after a
  clean initial import if you accidentally imported the wrong value.
- `invalid_scope` or introspected token missing `payment.initiate`: check the
  realm's optional client-scope mapping and whether the request explicitly
  includes the scope. Do not bypass the verifier's assertion.
- Keycloak imports realm JSON **only if the realm does not already exist**.
  An existing realm is not replaced on restart. For a *disposable* local stack,
  `docker compose down` and `docker compose up -d` recreate the container;
  this stack defines no persistent volume, so its realm is reimported.
- `start-dev`, HTTP, and `sslRequired: none` are **local-only**. Do not deploy
  this Compose configuration or its test client in production.
- The Keycloak-to-SIXPAY link, application/certificate association, certificate
  revocation, secure production storage, and production mTLS gateway are
  separate approved M2M follow-up lots, **not** completed by M2M-1.

## Local static config tests

```powershell
py -m unittest discover -s tests -p "test_*.py" -v
```

These tests inspect the newly added configuration; they do not start Keycloak.

## Production decision (not implemented in this lot)

Keycloak is the selected production identity service. A production deployment
requires a persistent database, secret management, backup, HTTPS, monitoring,
and availability design. Termination of mutual TLS is expected to use the bank's
approved gateway if available; no gateway choice or PKI configuration is made
here. JWT, certificate and partner identity correlation must be verified in
future M2M lots before any payment initiation is considered secure.
