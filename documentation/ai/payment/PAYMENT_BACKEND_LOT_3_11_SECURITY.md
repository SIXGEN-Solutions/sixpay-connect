# SIXPAY CONNECT — Phase 3 / Lot 3.11

## Security

### Existing platform security reused

The Payment module reuses:

```text
AuthenticatedUser
CurrentUserProvider
SixpayRole
```

No Payment-specific role enum is introduced.

### Policies

```text
PaymentRolePolicy
PaymentPartnerIsolationPolicy
PaymentAccessPolicy
```

Authorization requires both:

```text
role permission
AND
specific Payment authority
```

### Roles

Current platform roles are interpreted as follows:

- `ADMIN`: all approved Payment actions;
- `OPS`: read, operate and reconcile;
- `MANAGER`: read, operate, audit and reverse;
- `SUPPORT`: masked read only;
- `AUDITOR`: read and audit;
- `READ_ONLY`: masked read only;
- `PARTNER`: masked read only and only inside its ownership scope.

### Object access

Object access uses `PaymentObjectAccessPort`, which returns minimal ownership
metadata without loading the Payment Aggregate Root.

A Partner may access a Payment only when:

```text
descriptor.partnerSubject == authenticatedUser.subject
```

Missing ownership data is denied.

### Partner isolation

Searches receive:

```text
PaymentVisibilityScope.Partner(authenticatedSubject)
```

The projection datastore adapter must apply this scope in the database query.
Post-filtering an unrestricted result set is forbidden.

### Important repository limitation

The current Payment model contains only:

```text
PaymentSource.TRESOR_PAY
```

and no persisted Partner owner ID. Therefore, ownership must not be inferred
from source, subscription reference or external reference.

Until a projection/access adapter can supply explicit partner ownership,
Partner queries remain inactive because
`SecuredPaymentProjectionQueryService` requires both:

```text
PaymentProjectionReadPort
PaymentObjectAccessPort
```

### REST integration

The Payment query controller now delegates authorization to
`PaymentAccessPolicy` rather than checking only a raw OAuth2 scope.

Security failures are mapped to:

```text
401 AUTHENTICATION_REQUIRED
403 PAYMENT_ACCESS_DENIED
```

The 403 response intentionally conceals object existence.
