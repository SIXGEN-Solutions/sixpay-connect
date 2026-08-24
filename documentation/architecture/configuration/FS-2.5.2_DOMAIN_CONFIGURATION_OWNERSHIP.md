# FS-2.5.2 — Domain Configuration Ownership

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.5 — Configuration consolidation`  
**Golden module:** Partner

## Canonical ownership rule

```text
sixpay.partner.*        -> Partner
sixpay.customer.*       -> Customer
sixpay.payment.*        -> Payment
sixpay.accounting.*     -> Accounting
sixpay.reporting.*      -> Reporting
sixpay.notification.*   -> Notification
sixpay.security.*       -> Security
sixpay.administration.* -> Administration
```

Physical YAML location and semantic ownership are deliberately separated during
consolidation. Bootstrap may still provide runtime values, but it does not become
the semantic owner of domain keys.

## Rules

1. A business module owns the meaning, defaults and validation of its
   `sixpay.<domain>.*` configuration.
2. Bootstrap may assemble values but must not define business semantics for
   those keys.
3. A business module must not read another domain's configuration directly.
4. Cross-domain behavior must go through application/domain contracts, not
   through shared property reads.
5. Property keys and environment-variable names remain unchanged in FS-2.5.2.
6. Existing profile behavior remains unchanged.
7. Package/configuration-class renames are out of scope unless needed to fix a
   proven ownership violation.

## Physical relocation policy

FS-2.5.2 does not require moving all domain YAML into module resources. The
current modular-monolith runtime may keep profile values in Bootstrap while
semantic ownership and Java binding stay with the domain. Physical profile
consolidation is handled in FS-2.5.3.


## Domain consumer matrix

| Consumer module | Property/prefix | Semantic owner | Kind | Source |
|---|---|---|---|---|
| Customer | `sixpay.customer.observation.audit.persistence.enabled` | Customer | ConditionalOnProperty | `backend/customer/src/main/java/com/sixpay/customer/observation/configuration/ObservedCustomerAuditPersistenceConfiguration.java` |
| Customer | `sixpay.customer.observation.persistence` | Customer | ConfigurationProperties | `backend/customer/src/main/java/com/sixpay/customer/observation/configuration/ObservedCustomerPersistenceProperties.java` |
| Customer | `sixpay.customer.observation.persistence.enabled` | Customer | ConditionalOnProperty | `backend/customer/src/main/java/com/sixpay/customer/observation/configuration/ObservedCustomerPersistenceConfiguration.java` |
| Customer | `sixpay.customer.observation.query` | Customer | ConfigurationProperties | `backend/customer/src/main/java/com/sixpay/customer/observation/configuration/ObservedCustomerQueryProperties.java` |
| Customer | `sixpay.customer.observation.query.enabled` | Customer | ConditionalOnProperty | `backend/customer/src/main/java/com/sixpay/customer/observation/api/configuration/ObservedCustomerQueryApiConfiguration.java` |
| Customer | `sixpay.customer.observation.query.enabled` | Customer | ConditionalOnProperty | `backend/customer/src/main/java/com/sixpay/customer/observation/configuration/ObservedCustomerQueryConfiguration.java` |
| Customer | `sixpay.customer.observation.resilience` | Customer | ConfigurationProperties | `backend/customer/src/main/java/com/sixpay/customer/observation/configuration/ObservedCustomerProjectionResilienceProperties.java` |
| Notification | `sixpay.notification.email` | Notification | ConfigurationProperties | `backend/notification/src/main/java/com/sixpay/notification/infrastructure/email/NotificationEmailProperties.java` |
| Notification | `sixpay.notification.email.mode` | Notification | ConditionalOnProperty | `backend/notification/src/main/java/com/sixpay/notification/configuration/NotificationEmailAutoConfiguration.java` |
| Notification | `sixpay.notification.email.mode` | Notification | ConditionalOnProperty | `backend/notification/src/main/java/com/sixpay/notification/configuration/NotificationEmailAutoConfiguration.java` |
| Notification | `sixpay.notification.email.mode` | Notification | ConditionalOnProperty | `backend/notification/src/main/java/com/sixpay/notification/configuration/NotificationEmailAutoConfiguration.java` |
| Notification | `sixpay.notification.retry.enabled` | Notification | ConditionalOnProperty | `backend/notification/src/main/java/com/sixpay/notification/configuration/NotificationRetryAutoConfiguration.java` |
| Payment | `sixpay.payment.callback` | Payment | ConfigurationProperties | `backend/payment/src/main/java/com/sixpay/payment/infrastructure/callback/PaymentCallbackProperties.java` |
| Payment | `sixpay.payment.callback.enabled` | Payment | ConditionalOnProperty | `backend/payment/src/main/java/com/sixpay/payment/infrastructure/callback/PaymentCallbackDetachedJwsSigner.java` |
| Payment | `sixpay.payment.callback.enabled` | Payment | ConditionalOnProperty | `backend/payment/src/main/java/com/sixpay/payment/infrastructure/callback/PaymentCallbackSchedulingConfiguration.java` |
| Payment | `sixpay.payment.callback.enabled` | Payment | ConditionalOnProperty | `backend/payment/src/main/java/com/sixpay/payment/infrastructure/callback/PaymentCallbackSigningConfiguration.java` |
| Payment | `sixpay.payment.callback.enabled` | Payment | ConditionalOnProperty | `backend/payment/src/main/java/com/sixpay/payment/infrastructure/callback/PaymentStatusCallbackHttpAdapter.java` |
| Payment | `sixpay.payment.callback.enabled` | Payment | ConditionalOnProperty | `backend/payment/src/main/java/com/sixpay/payment/infrastructure/callback/relay/PaymentCallbackOutboxCoordinator.java` |
| Payment | `sixpay.payment.callback.enabled` | Payment | ConditionalOnProperty | `backend/payment/src/main/java/com/sixpay/payment/infrastructure/callback/relay/PaymentCallbackOutboxRelay.java` |
| Payment | `sixpay.payment.callback.enabled` | Payment | ConditionalOnProperty | `backend/payment/src/main/java/com/sixpay/payment/infrastructure/callback/relay/PaymentCallbackPlanFactory.java` |
| Payment | `sixpay.payment.tresorpay` | Payment | ConfigurationProperties | `backend/payment/src/main/java/com/sixpay/payment/infrastructure/tresorpay/TresorPayIntegrationProperties.java` |
| Payment | `sixpay.payment.tresorpay.enabled` | Payment | ConditionalOnProperty | `backend/payment/src/main/java/com/sixpay/payment/infrastructure/tresorpay/TresorPayIntegrationConfiguration.java` |
| Reporting | `sixpay.reporting.audit-export` | Reporting | ConfigurationProperties | `backend/reporting/src/main/java/com/sixpay/reporting/configuration/ReportingAuditExportProperties.java` |
| Reporting | `sixpay.reporting.audit-query` | Reporting | ConfigurationProperties | `backend/reporting/src/main/java/com/sixpay/reporting/configuration/ReportingAuditQueryProperties.java` |
| Security | `sixpay.security` | Security | ConfigurationProperties | `backend/security/src/main/java/com/sixpay/security/config/SixpaySecurityProperties.java` |
| Security | `sixpay.security.authentication-mode` | Security | ConditionalOnProperty | `backend/security/src/main/java/com/sixpay/security/config/OidcSecurityConfiguration.java` |
| Security | `sixpay.security.authentication.local.enabled` | Security | ConditionalOnProperty | `backend/security/src/main/java/com/sixpay/security/api/controller/LocalAuthenticationController.java` |
| Security | `sixpay.security.authentication.local.enabled` | Security | ConditionalOnProperty | `backend/security/src/main/java/com/sixpay/security/api/controller/LocalPasswordController.java` |
| Security | `sixpay.security.authentication.local.enabled` | Security | ConditionalOnProperty | `backend/security/src/main/java/com/sixpay/security/configuration/LocalAuthenticationConfiguration.java` |
| Security | `sixpay.security.authentication.oidc.enabled` | Security | ConditionalOnProperty | `backend/security/src/main/java/com/sixpay/security/configuration/SixpaySecurityAutoConfiguration.java` |

## Ownership decisions

- **Partner** owns `sixpay.partner.*` — detected Java consumers: **0**.
- **Customer** owns `sixpay.customer.*` — detected Java consumers: **7**.
- **Payment** owns `sixpay.payment.*` — detected Java consumers: **10**.
- **Accounting** owns `sixpay.accounting.*` — detected Java consumers: **0**.
- **Reporting** owns `sixpay.reporting.*` — detected Java consumers: **2**.
- **Notification** owns `sixpay.notification.*` — detected Java consumers: **5**.
- **Security** owns `sixpay.security.*` — detected Java consumers: **6**.
- **Administration** owns `sixpay.administration.*` — detected Java consumers: **0**.

## Cross-domain configuration consumption

No direct `sixpay.<other-domain>.*` configuration consumption was detected in production Java.

## Result

- Domain property consumers detected: **30**
- Cross-domain ownership violations: **0**

**FS-2.5.2 ownership model is consistent with the current code.**
