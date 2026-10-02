# SIXPAY CONNECT — Local Docker runtime

## Purpose

This stack packages the existing SIXPAY CONNECT modular monolith for local
Docker execution close to a deployed runtime. It starts PostgreSQL 15, the
Spring Boot `bootstrap` executable and the Angular integration build served by
Nginx. Flyway remains the schema-management mechanism.

## Authentication

The Docker baseline uses the existing backend `local-auth` profile and Angular
`integration` environment. It does not invent an OIDC or LDAP provider.

`SIXPAY_SESSION_COOKIE_SECURE=false` is restricted to local HTTP. Do not use
this setting for a TLS deployment.

## Prerequisites

Docker Engine / Docker Desktop with Docker Compose v2.

No local Java, Maven, Node.js, npm or PostgreSQL installation is required for
the image build.

## Configure

From `infrastructure/docker`:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Change `POSTGRES_PASSWORD`. `.env` is ignored by Git and must stay local.

## Build and start

```bash
docker compose build
docker compose up -d
docker compose ps
```

Endpoints:

```text
Frontend:       http://localhost:8088
Backend:        http://localhost:8080
Backend health: http://localhost:8080/actuator/health
PostgreSQL:     localhost:15432
```

Payment confirmation / OTP is delegated by SIXPAY to the Core Banking
`PaymentConfirmationGateway`. This SIXPAY stack does not run an SMTP server for
OTP delivery. The Notification module's independent operational or partner
email capabilities require an explicitly configured SMTP provider when they are
intentionally enabled.

Follow logs:

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres
```

## Stop / reset

Preserve database data:

```bash
docker compose down
```

Delete the local database volume as well:

```bash
docker compose down -v
```

The second command destroys local Docker data.

## Rebuild after source changes

```bash
docker compose up -d --build
```

## Validation commands

Image assembly does not replace repository gates. Run them separately.

Backend:

```bash
cd backend
mvn verify
mvn -Pfull-tests clean verify
```

Frontend:

```bash
cd frontend
npm ci
npm run verify:quality
npm run verify:ci
```

Repository root:

```bash
py scripts/verify_master_prompt_input_manifest.py
py scripts/verify_repository_hygiene.py
py scripts/verify_documentation_final.py
py scripts/verify_baseline.py
git diff --check
git status --short
```

The patch script executes none of these gates.
