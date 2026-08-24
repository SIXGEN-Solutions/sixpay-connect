# FS-2.4.1 — Business-to-Business Edge Classification

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.4 — Dependency and module boundary audit`  
**Golden module:** Partner

## Reviewed public seams

The following Security surfaces are explicitly treated as published module contracts:

- `security.authentication.CurrentUserProvider`
- `security.authentication.AuthenticatedUser`
- `security.authentication.SixpayPrincipal`
- `security.authorization.*`
- `security.application.port.in.*` / `security.application.port.input.*`
- `security.application.model.*` when returned by published use cases

`application.port.in` and `application.port.input` are treated as the same architectural input-port convention during baseline consolidation; no package rename is required by FS-2.4.1.

`SecurityContextCurrentUserProvider`, Security infrastructure, JWT implementation and configuration remain internal.

**Regression policy:** FS-2.4.1 is classification-first. Existing functional code is not moved or renamed merely to satisfy package naming consistency. Only proven boundary violations may trigger a targeted refactor, followed by module/consumer tests and full reactor verification.

## Edge matrix

| Source | Target | Maven | Imports | Decision | Status |
|---|---|---:|---:|---|---|
| Administration | Security | yes | 6 | `KEEP_COMPOSED_PUBLIC_CONTRACTS` | ✅ ACCEPT |
| Customer | Security | yes | 2 | `KEEP_PUBLIC_SECURITY_CONTRACT` | ✅ ACCEPT |
| Partner | Security | yes | 3 | `KEEP_PUBLIC_SECURITY_CONTRACT` | ✅ ACCEPT |
| Payment | Security | yes | 7 | `KEEP_PUBLIC_SECURITY_CONTRACT` | ✅ ACCEPT |

## Detailed classification

### Administration → Security

- ✅ Security application response contract — `com.sixpay.security.application.model.SecurityUserDetail` — `backend\administration\src\main\java\com\sixpay\administration\api\SecurityUserAdministrationController.java` — `KEEP_APPLICATION_CONTRACT`
- ✅ Security application response contract — `com.sixpay.security.application.model.SecurityUserSummary` — `backend\administration\src\main\java\com\sixpay\administration\api\SecurityUserAdministrationController.java` — `KEEP_APPLICATION_CONTRACT`
- ✅ application input port — `com.sixpay.security.application.port.in.CreateSecurityUserCommand` — `backend\administration\src\main\java\com\sixpay\administration\api\SecurityUserAdministrationController.java` — `KEEP_APPLICATION_PORT`
- ✅ application input port — `com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase` — `backend\administration\src\main\java\com\sixpay\administration\api\SecurityUserAdministrationController.java` — `KEEP_APPLICATION_PORT`
- ✅ application input port — `com.sixpay.security.application.port.in.UpdateSecurityUserCommand` — `backend\administration\src\main\java\com\sixpay\administration\api\SecurityUserAdministrationController.java` — `KEEP_APPLICATION_PORT`
- ✅ public security authentication-context contract — `com.sixpay.security.authentication.CurrentUserProvider` — `backend\administration\src\main\java\com\sixpay\administration\api\SecurityUserAdministrationController.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`

### Customer → Security

- ✅ public security authentication-context contract — `com.sixpay.security.authentication.CurrentUserProvider` — `backend\customer\src\main\java\com\sixpay\customer\management\api\ObservedCustomerLinkController.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`
- ✅ public security authentication-context contract — `com.sixpay.security.authentication.CurrentUserProvider` — `backend\customer\src\main\java\com\sixpay\customer\management\application\audit\CustomerAuditRecorder.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`

### Partner → Security

- ✅ public security authentication-context contract — `com.sixpay.security.authentication.CurrentUserProvider` — `backend\partner\src\main\java\com\sixpay\partner\api\PartnerController.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`
- ✅ public security authentication-context contract — `com.sixpay.security.authentication.CurrentUserProvider` — `backend\partner\src\main\java\com\sixpay\partner\api\security\PartnerAccessPolicy.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`
- ✅ public security authorization contract — `com.sixpay.security.authorization.SixpayRole` — `backend\partner\src\main\java\com\sixpay\partner\api\security\PartnerAccessPolicy.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`

### Payment → Security

- ✅ public security authentication-context contract — `com.sixpay.security.authentication.CurrentUserProvider` — `backend\payment\src\main\java\com\sixpay\payment\api\PaymentCommandController.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`
- ✅ public security authentication-context contract — `com.sixpay.security.authentication.AuthenticatedUser` — `backend\payment\src\main\java\com\sixpay\payment\application\security\PaymentAccessPolicy.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`
- ✅ public security authentication-context contract — `com.sixpay.security.authentication.CurrentUserProvider` — `backend\payment\src\main\java\com\sixpay\payment\application\security\PaymentAccessPolicy.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`
- ✅ public security authentication-context contract — `com.sixpay.security.authentication.AuthenticatedUser` — `backend\payment\src\main\java\com\sixpay\payment\application\security\PaymentPartnerIsolationPolicy.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`
- ✅ public security authorization contract — `com.sixpay.security.authorization.SixpayRole` — `backend\payment\src\main\java\com\sixpay\payment\application\security\PaymentPartnerIsolationPolicy.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`
- ✅ public security authentication-context contract — `com.sixpay.security.authentication.AuthenticatedUser` — `backend\payment\src\main\java\com\sixpay\payment\application\security\PaymentRolePolicy.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`
- ✅ public security authorization contract — `com.sixpay.security.authorization.SixpayRole` — `backend\payment\src\main\java\com\sixpay\payment\application\security\PaymentRolePolicy.java` — `KEEP_PUBLIC_SECURITY_CONTRACT`

## Maven-only dependency review

No unused business Maven dependency detected.

## Result

- Business edges: **4**
- Blocking imports: **0**
- Maven-only dependencies: **0**
- Circular dependencies: **0**

**Code-level business edges PASS.** Any Maven-only dependency remains cleanup debt.
