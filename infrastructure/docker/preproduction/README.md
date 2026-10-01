# SIXPAY CONNECT - Pre-production Docker

Services: PostgreSQL, Samba AD/LDAPS, SIXPAY backend and frontend. Core Banking is external to this Compose and must be available on `http://localhost:9092`; containers use `http://host.docker.internal:9092`.

Human auth is LDAP-only: LOCAL=false, OIDC=false, LDAP=true. Samba AD is used because SIXPAY consumes AD attributes (`sAMAccountName`, `objectGUID`, `userAccountControl`).

Before start, copy `.env.example` to `.env`, choose passwords, then create `secrets/samba-admin-password.txt` with the same password configured as `SIXPAY_LDAP_SERVICE_ACCOUNT_PASSWORD`. Docker Desktop host networking must be enabled for the Samba AD container.

Start: `docker compose up -d --build`.

Create a test AD user with `docker exec -it sixpay-preprod-ldap samba-tool user create sixpay.testuser 'Change-Me-123!'`. The corresponding canonical SIXPAY LDAP identity link must also exist; LDAP groups do not grant SIXPAY roles.

Validate with `docker compose config`, backend Maven tests, frontend quality/CI gates and repository Python gates.
