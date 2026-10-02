# SIXPAY CONNECT - Pre-production Docker

Services: PostgreSQL, Samba AD/LDAPS, SIXPAY backend and frontend. Core Banking is external to this Compose and must be available on `http://localhost:9092`; containers use `http://host.docker.internal:9092`.

Human auth is LDAP-only: LOCAL=false, OIDC=false, LDAP=true. Samba AD is used because SIXPAY consumes AD attributes (`sAMAccountName`, `objectGUID`, `userAccountControl`).

The pre-production LDAP truststore is built from the Temurin 21 default JVM `cacerts` and augmented with the Samba AD CA. This preserves the JVM standard trusted roots while adding trust for the pre-production LDAPS endpoint.

Before start, copy `.env.example` to `.env`, choose passwords, then create `secrets/samba-admin-password.txt` with the same password configured as `SIXPAY_LDAP_SERVICE_ACCOUNT_PASSWORD`. Docker Desktop host networking must be enabled for the Samba AD container.

Start: `docker compose up -d --build`.

Create a test AD user with `docker exec -it sixpay-preprod-ldap samba-tool user create sixpay.testuser 'Change-Me-123!'`. LDAP groups do not grant SIXPAY roles.

For a brand-new empty SIXPAY security realm, the first administrator may be provisioned through the one-shot LDAP administrator bootstrap. Obtain the immutable AD `objectGUID`, choose a stable canonical SIXPAY UUID, then temporarily configure:

```text
SIXPAY_LDAP_ADMIN_BOOTSTRAP_ENABLED=true
SIXPAY_LDAP_ADMIN_BOOTSTRAP_USER_ID=<canonical-sixpay-uuid>
SIXPAY_LDAP_ADMIN_BOOTSTRAP_USERNAME=sixpay.testuser
SIXPAY_LDAP_ADMIN_BOOTSTRAP_EMAIL=<optional-email>
SIXPAY_LDAP_ADMIN_BOOTSTRAP_TRUST_DOMAIN=sixpay-preproduction-ldap
SIXPAY_LDAP_ADMIN_BOOTSTRAP_STABLE_SUBJECT=<ad-object-guid>
```

Security requires the canonical user store to be empty, creates exactly one canonical `ADMIN` without any LOCAL credential, assigns canonical SIXPAY permissions, and links the immutable LDAP identity in one transaction. If identity linking fails, creation is rolled back.

After the first successful start, set `SIXPAY_LDAP_ADMIN_BOOTSTRAP_ENABLED=false` and restart. Leaving bootstrap enabled against an already provisioned user store intentionally fails closed. Normal administrator lifecycle must then use the authenticated administration API; do not reuse bootstrap for subsequent administrators.

Validate with `docker compose config`, backend Maven tests, frontend quality/CI gates and repository Python gates.
