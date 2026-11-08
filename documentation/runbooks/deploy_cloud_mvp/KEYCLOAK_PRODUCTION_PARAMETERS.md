# SIXPAY CONNECT — Keycloak production placeholders (NOT a deployment configuration)

Reference revision: `498cbb3ae899880d2ee2489fdf0b4f8db4309091`.
These are planning inputs, not operational values. Do **not** copy the local Keycloak `start-dev` Compose stack to production.
All values marked `<changeMe>` require the approved infrastructure/security team to provision them and store secrets outside Git.

```dotenv
# Keycloak / OIDC M2M
SIXPAY_M2M_ENABLED=<changeMe>
SIXPAY_M2M_ISSUER_URI=<changeMe>
SIXPAY_M2M_JWK_SET_URI=<changeMe>
SIXPAY_M2M_AUDIENCE=<changeMe>

# Keycloak deployment settings — illustrative placeholders, not a validated SIXPAY env schema
KEYCLOAK_PRODUCTION_REALM=<changeMe>
KEYCLOAK_PRODUCTION_CLIENT_ID=<changeMe>
KEYCLOAK_PRODUCTION_CLIENT_SECRET=<changeMe>
KEYCLOAK_PRODUCTION_HTTPS_HOSTNAME=<changeMe>
KEYCLOAK_PRODUCTION_DATABASE_URL=<changeMe>
KEYCLOAK_PRODUCTION_DATABASE_USERNAME=<changeMe>
KEYCLOAK_PRODUCTION_DATABASE_PASSWORD=<changeMe>
KEYCLOAK_PRODUCTION_BACKUP_POLICY=<changeMe>
KEYCLOAK_PRODUCTION_MONITORING_ENDPOINT=<changeMe>
KEYCLOAK_PRODUCTION_MTLS_TRUST_CONFIGURATION=<changeMe>
```

The `SIXPAY_M2M_*` names are drawn from the production Spring profile. The `KEYCLOAK_PRODUCTION_*` names are **documentation placeholders only**, not verified environment-variable names consumed by the current application.

Additional decisions not established by these placeholders: approved API gateway/mTLS termination and certificate trust, target Keycloak runtime/deployment topology, persistent storage, failover, backup/restore verification, rotating service credentials, secret-manager integration, network policy and observable security alerts. No provisioning is performed by this document.
