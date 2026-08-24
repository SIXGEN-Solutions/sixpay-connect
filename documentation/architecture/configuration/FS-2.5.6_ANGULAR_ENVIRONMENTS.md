# FS-2.5.6 — Angular Environment Consolidation

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.5 — Configuration consolidation`

## Purpose

FS-2.5.6 freezes the current Angular runtime environment policy without
renaming environments or changing application behavior.

## Canonical environment matrix

| Angular configuration | Environment file | backend.mode | standalone | local | oidc | Intent |
|---|---|---|---|---|---|---|
| production | `environment.ts` | `api` | false | true | true | production hybrid auth |
| development | `environment.development.ts` | `mock` | true | false | false | local developer mock mode |
| integration | `environment.integration.ts` | `api` | false | true | false | contract-backed integration |
| netlify | `environment.netlify.ts` | `mock` | true | false | false | hosted demo/mock mode |

## Runtime datasource policy

The backend datasource mode is explicit:

```text
mode = api
or
mode = mock
```

There is no implicit fallback.

In particular:

```text
production  => API only
integration => API only
development => mock explicitly allowed
netlify     => mock explicitly allowed
```

An API failure must never cause a silent switch to mock data.

## Authentication policy

Production:

```text
standalone = false
local      = enabled
oidc       = enabled
```

Integration:

```text
standalone = false
local      = enabled
oidc       = disabled
```

Development and Netlify:

```text
standalone = true
local      = disabled
oidc       = disabled
```

`validateAuthenticationEnvironment()` remains the central runtime validation
for impossible authentication combinations.

## Angular CLI mapping

`angular.json` remains the canonical build mapping:

```text
development -> environment.development.ts
integration -> environment.integration.ts
netlify     -> environment.netlify.ts
production  -> environment.ts (no replacement)
```

## Environment model ownership

`environment.model.ts` owns the structural contract:

```text
BackendMode = 'mock' | 'api'
AppEnvironment
AuthenticationEnvironment
BackendEnvironment
```

Environment files only provide runtime values conforming to that model.

## Non-regression policy

FS-2.5.6 does not:

- rename environment files;
- rename `backend.mode`;
- rename authentication properties;
- change OIDC authority/client/scope;
- change production authentication capabilities;
- change integration authentication mode;
- remove mock mode from development/demo;
- add API-to-mock fallback.

## Exit criteria

FS-2.5.6 is DONE when:

- Angular CLI mappings are canonical;
- production/integration are API-only;
- development/netlify are explicit mock-only environments;
- authentication semantics remain reviewed;
- environment files conform to `AppEnvironment`;
- a permanent frontend gate protects this matrix.
