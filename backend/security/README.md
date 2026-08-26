# Security Module

## Purpose

The Security module provides shared authentication, authorization, identity
linking, password lifecycle and security-audit capabilities.

## Capabilities

- local authentication and session management;
- OIDC session integration;
- JWT resource-server authority conversion;
- SIXPAY-owned roles and permissions;
- local password change and reset support;
- user-account and external-identity linking;
- authentication and security operational audit.

The identity provider proves identity. SIXPAY owns authorization and maps the
authenticated identity to SIXPAY roles and permissions.

## API

Authentication and session endpoints:

    /api/v1/auth/login
    /api/v1/auth/me
    /api/v1/auth/session/oidc
    /api/v1/auth/logout
    /api/v1/auth/password/change

Administration exposes user-management HTTP boundaries while Security owns the
underlying users, identities, credentials and authorization data.

## Boundaries

- Security does not own business-domain aggregates.
- Administration calls Security application capabilities through ports.
- Business modules consume the authenticated principal and authorities.
- Secrets and provider credentials are supplied by runtime configuration.

## Validation

From backend:

    mvn -pl security -am test
    mvn -pl security -am clean verify
    mvn -pl security -am -Pfull-tests clean verify

The full-tests command requires Docker when integration tests are selected.
