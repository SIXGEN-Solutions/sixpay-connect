# FS-2.4.0 — Module Dependency & Boundary Inventory

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.4 — Dependency and module boundary audit`  
**Golden module:** Partner

## Purpose

This inventory is generated from the current Maven descriptors and production Java imports. It makes no implementation change.

## Classification vocabulary

- ✅ application port
- ✅ public domain contract
- ✅ shared-kernel value object
- ✅ event
- ✅ provider-neutral integration
- ✅ common/platform contract
- ⚠ direct repository access
- ⚠ JPA entity import
- ⚠ infrastructure import
- ⚠ unclassified cross-module import
- ❌ circular dependency

## Business-module Java dependency matrix

| Source | Ptn | Cus | Pay | Acc | Rep | Not | Sec | Adm |
|---|---|---|---|---|---|---|---|---|
| Partner | — |  |  |  |  |  | ✅ |  |
| Customer |  | — |  |  |  |  | ✅ |  |
| Payment |  |  | — |  |  |  | ✅ |  |
| Accounting |  |  |  | — |  |  |  |  |
| Reporting |  |  |  |  | — |  |  |  |
| Notification |  |  |  |  |  | — |  |  |
| Security |  |  |  |  |  |  | — |  |
| Administration |  |  |  |  |  |  | ✅ | — |

`✅` means all discovered imports on that edge are currently classified as boundary-safe by the inventory heuristic. `⚠` requires explicit review.

## Maven business-module dependencies

- **Partner** → `security` (compile)
- **Customer** → `security` (compile)
- **Payment** → `security` (compile)
- **Accounting** → _none_
- **Reporting** → `security` (compile)
- **Notification** → _none_
- **Security** → _none_
- **Administration** → `security` (compile)

## Detailed Java cross-module imports

### Accounting → Common

**✅ common/platform contract**

- `com.sixpay.common.context.CorrelationId` — `backend\accounting\src\main\java\com\sixpay\accounting\application\port\output\AccountingIntegrationContext.java`

### Accounting → Integration

**✅ provider-neutral integration**

- `com.sixpay.integration.http.IntegrationHttpHeaders` — `backend\accounting\src\main\java\com\sixpay\accounting\infrastructure\accountingapi\client\RestAccountingBatchClient.java`
- `com.sixpay.integration.http.HttpTimeoutPolicy` — `backend\accounting\src\main\java\com\sixpay\accounting\infrastructure\accountingapi\configuration\AccountingApiConfiguration.java`
- `com.sixpay.integration.http.StandardRestClientFactory` — `backend\accounting\src\main\java\com\sixpay\accounting\infrastructure\accountingapi\configuration\AccountingApiConfiguration.java`

### Administration → Common

**✅ common/platform contract**

- `com.sixpay.common.time.SystemTimeProvider` — `backend\administration\src\main\java\com\sixpay\administration\configuration\AdministrationModuleConfiguration.java`
- `com.sixpay.common.time.TimeProvider` — `backend\administration\src\main\java\com\sixpay\administration\configuration\AdministrationModuleConfiguration.java`
- `com.sixpay.common.time.TimeProvider` — `backend\administration\src\main\java\com\sixpay\administration\infrastructure\monitoring\ActuatorIntegrationHealthQueryAdapter.java`
- `com.sixpay.common.time.TimeProvider` — `backend\administration\src\main\java\com\sixpay\administration\application\service\AdministrationQueryService.java`

### Administration → Security

**✅ application port**

- `com.sixpay.security.application.port.in.CreateSecurityUserCommand` — `backend\administration\src\main\java\com\sixpay\administration\api\SecurityUserAdministrationController.java`
- `com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase` — `backend\administration\src\main\java\com\sixpay\administration\api\SecurityUserAdministrationController.java`
- `com.sixpay.security.application.port.in.UpdateSecurityUserCommand` — `backend\administration\src\main\java\com\sixpay\administration\api\SecurityUserAdministrationController.java`

**✅ public security contract (REVIEW API SURFACE)**

- `com.sixpay.security.application.model.SecurityUserDetail` — `backend\administration\src\main\java\com\sixpay\administration\api\SecurityUserAdministrationController.java`
- `com.sixpay.security.application.model.SecurityUserSummary` — `backend\administration\src\main\java\com\sixpay\administration\api\SecurityUserAdministrationController.java`
- `com.sixpay.security.authentication.CurrentUserProvider` — `backend\administration\src\main\java\com\sixpay\administration\api\SecurityUserAdministrationController.java`

### Customer → Common

**✅ common/platform contract**

- `com.sixpay.common.context.CorrelationId` — `backend\customer\src\main\java\com\sixpay\customer\verification\domain\model\CustomerVerificationContext.java`
- `com.sixpay.common.context.CorrelationId` — `backend\customer\src\main\java\com\sixpay\customer\management\api\ObservedCustomerLinkController.java`
- `com.sixpay.common.context.CorrelationId` — `backend\customer\src\main\java\com\sixpay\customer\management\application\audit\CustomerAuditRecorder.java`
- `com.sixpay.common.context.CorrelationId` — `backend\customer\src\main\java\com\sixpay\customer\management\application\service\CustomerEnrollmentService.java`
- `com.sixpay.common.context.CorrelationId` — `backend\customer\src\main\java\com\sixpay\customer\management\application\service\CustomerManagementService.java`

### Customer → Integration

**✅ provider-neutral integration**

- `com.sixpay.integration.http.IntegrationHttpHeaders` — `backend\customer\src\main\java\com\sixpay\customer\verification\infrastructure\banking\client\AmplitudeCustomerVerificationClient.java`
- `com.sixpay.integration.http.HttpTimeoutPolicy` — `backend\customer\src\main\java\com\sixpay\customer\verification\infrastructure\banking\configuration\AmplitudeCustomerVerificationConfiguration.java`
- `com.sixpay.integration.http.StandardRestClientFactory` — `backend\customer\src\main\java\com\sixpay\customer\verification\infrastructure\banking\configuration\AmplitudeCustomerVerificationConfiguration.java`

### Customer → Security

**✅ public security contract (REVIEW API SURFACE)**

- `com.sixpay.security.authentication.CurrentUserProvider` — `backend\customer\src\main\java\com\sixpay\customer\management\api\ObservedCustomerLinkController.java`
- `com.sixpay.security.authentication.CurrentUserProvider` — `backend\customer\src\main\java\com\sixpay\customer\management\application\audit\CustomerAuditRecorder.java`

### Notification → Common

**✅ common/platform contract**

- `com.sixpay.common.time.SystemTimeProvider` — `backend\notification\src\main\java\com\sixpay\notification\configuration\NotificationApplicationAutoConfiguration.java`
- `com.sixpay.common.time.TimeProvider` — `backend\notification\src\main\java\com\sixpay\notification\configuration\NotificationApplicationAutoConfiguration.java`
- `com.sixpay.common.time.TimeProvider` — `backend\notification\src\main\java\com\sixpay\notification\configuration\NotificationRetryAutoConfiguration.java`
- `com.sixpay.common.messaging.model.IntegrationEventEnvelope` — `backend\notification\src\main\java\com\sixpay\notification\infrastructure\messaging\internal\InternalIntegrationEventListener.java`
- `com.sixpay.common.validation.Preconditions` — `backend\notification\src\main\java\com\sixpay\notification\infrastructure\messaging\internal\InternalIntegrationEventListener.java`
- `com.sixpay.common.messaging.model.IntegrationEventEnvelope` — `backend\notification\src\main\java\com\sixpay\notification\infrastructure\messaging\kafka\KafkaIntegrationEventListener.java`
- `com.sixpay.common.validation.Preconditions` — `backend\notification\src\main\java\com\sixpay\notification\infrastructure\messaging\kafka\KafkaIntegrationEventListener.java`
- `com.sixpay.common.messaging.model.IntegrationEventEnvelope` — `backend\notification\src\main\java\com\sixpay\notification\application\service\PartnerDecisionNotificationService.java`
- `com.sixpay.common.time.TimeProvider` — `backend\notification\src\main\java\com\sixpay\notification\application\service\PartnerDecisionNotificationService.java`
- `com.sixpay.common.time.TimeProvider` — `backend\notification\src\main\java\com\sixpay\notification\application\service\RetryNotificationDeliveriesService.java`
- `com.sixpay.common.messaging.model.IntegrationEventEnvelope` — `backend\notification\src\main\java\com\sixpay\notification\application\port\in\HandleIntegrationEventUseCase.java`

### Partner → Common

**✅ common/platform contract**

- `com.sixpay.common.context.CorrelationId` — `backend\partner\src\main\java\com\sixpay\partner\api\PartnerController.java`
- `com.sixpay.common.identifier.IdentifierGenerator` — `backend\partner\src\main\java\com\sixpay\partner\configuration\PartnerModuleConfiguration.java`
- `com.sixpay.common.identifier.UuidIdentifierGenerator` — `backend\partner\src\main\java\com\sixpay\partner\configuration\PartnerModuleConfiguration.java`
- `com.sixpay.common.time.SystemTimeProvider` — `backend\partner\src\main\java\com\sixpay\partner\configuration\PartnerModuleConfiguration.java`
- `com.sixpay.common.time.TimeProvider` — `backend\partner\src\main\java\com\sixpay\partner\configuration\PartnerModuleConfiguration.java`
- `com.sixpay.common.messaging.model.IntegrationEventEnvelope` — `backend\partner\src\main\java\com\sixpay\partner\infrastructure\outbox\OutboxEventJpaEntity.java`
- `com.sixpay.common.messaging.model.OutboxMessage` — `backend\partner\src\main\java\com\sixpay\partner\infrastructure\outbox\OutboxEventJpaEntity.java`
- `com.sixpay.common.time.TimeProvider` — `backend\partner\src\main\java\com\sixpay\partner\infrastructure\outbox\PartnerOutboxEventPublisher.java`
- `com.sixpay.common.messaging.model.OutboxMessage` — `backend\partner\src\main\java\com\sixpay\partner\infrastructure\outbox\PartnerOutboxMessageSource.java`
- `com.sixpay.common.messaging.outbox.OutboxMessageSource` — `backend\partner\src\main\java\com\sixpay\partner\infrastructure\outbox\PartnerOutboxMessageSource.java`
- `com.sixpay.common.context.CorrelationId` — `backend\partner\src\main\java\com\sixpay\partner\application\command\ConfigureValidationThresholdCommand.java`
- `com.sixpay.common.context.CorrelationId` — `backend\partner\src\main\java\com\sixpay\partner\application\command\CreatePartnerCommand.java`
- `com.sixpay.common.context.CorrelationId` — `backend\partner\src\main\java\com\sixpay\partner\application\command\DecidePartnerCommand.java`
- `com.sixpay.common.context.CorrelationId` — `backend\partner\src\main\java\com\sixpay\partner\application\command\ReactivatePartnerCommand.java`
- `com.sixpay.common.context.CorrelationId` — `backend\partner\src\main\java\com\sixpay\partner\application\command\SuspendPartnerCommand.java`
- `com.sixpay.common.context.CorrelationId` — `backend\partner\src\main\java\com\sixpay\partner\application\service\PartnerApplicationService.java`
- `com.sixpay.common.identifier.IdentifierGenerator` — `backend\partner\src\main\java\com\sixpay\partner\application\service\PartnerApplicationService.java`
- `com.sixpay.common.time.TimeProvider` — `backend\partner\src\main\java\com\sixpay\partner\application\service\PartnerApplicationService.java`

### Partner → Security

**✅ public security contract (REVIEW API SURFACE)**

- `com.sixpay.security.authentication.CurrentUserProvider` — `backend\partner\src\main\java\com\sixpay\partner\api\PartnerController.java`
- `com.sixpay.security.authentication.CurrentUserProvider` — `backend\partner\src\main\java\com\sixpay\partner\api\security\PartnerAccessPolicy.java`
- `com.sixpay.security.authorization.SixpayRole` — `backend\partner\src\main\java\com\sixpay\partner\api\security\PartnerAccessPolicy.java`

### Payment → Common

**✅ common/platform contract**

- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\api\PaymentCommandApiMapper.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\api\PaymentCommandController.java`
- `com.sixpay.common.identifier.IdentifierGenerator` — `backend\payment\src\main\java\com\sixpay\payment\configuration\PaymentModuleConfiguration.java`
- `com.sixpay.common.identifier.UuidIdentifierGenerator` — `backend\payment\src\main\java\com\sixpay\payment\configuration\PaymentModuleConfiguration.java`
- `com.sixpay.common.time.SystemTimeProvider` — `backend\payment\src\main\java\com\sixpay\payment\configuration\PaymentModuleConfiguration.java`
- `com.sixpay.common.time.TimeProvider` — `backend\payment\src\main\java\com\sixpay\payment\configuration\PaymentModuleConfiguration.java`
- `com.sixpay.common.time.TimeProvider` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\idempotency\PaymentInitiationIdempotencyAdapter.java`
- `com.sixpay.common.identifier.IdentifierGenerator` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\initiation\PaymentInitiationPreparationAdapter.java`
- `com.sixpay.common.messaging.model.IntegrationEventEnvelope` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\outbox\PaymentIntegrationMapper.java`
- `com.sixpay.common.time.TimeProvider` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\query\PaymentProjectionReadAdapter.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\customer\mapper\CustomerVerificationPaymentMapper.java`
- `com.sixpay.common.time.TimeProvider` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\callback\relay\PaymentCallbackOutboxCoordinator.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\callback\relay\PaymentCallbackPlanFactory.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\status\mapper\AmplitudePostingStatusMapper.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\domain\event\PaymentDomainEvent.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\domain\event\PaymentEventMetadata.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\domain\model\PaymentRequestIdentity.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\domain\model\evidence\EvidenceMetadata.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\domain\model\evidence\EvidenceValueObjectRules.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\application\command\InitiateDebitCommand.java`
- `com.sixpay.common.time.TimeProvider` — `backend\payment\src\main\java\com\sixpay\payment\application\service\PaymentInitiationOrchestrationService.java`
- `com.sixpay.common.time.TimeProvider` — `backend\payment\src\main\java\com\sixpay\payment\application\service\PaymentMutationCoordinator.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\application\port\output\CustomerVerificationEvidenceMapper.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\application\port\output\banking\BankingRequestContext.java`
- `com.sixpay.common.context.CorrelationId` — `backend\payment\src\main\java\com\sixpay\payment\application\port\output\callback\PaymentStatusCallbackDelivery.java`

### Payment → Integration

**✅ provider-neutral integration**

- `com.sixpay.integration.http.IntegrationHttpHeaders` — `backend\payment\src\main\java\com\sixpay\payment\api\PaymentApiExceptionHandler.java`
- `com.sixpay.integration.http.CorrelationIdResolver` — `backend\payment\src\main\java\com\sixpay\payment\api\PaymentCommandController.java`
- `com.sixpay.integration.http.IntegrationHttpHeaders` — `backend\payment\src\main\java\com\sixpay\payment\api\PaymentCommandController.java`
- `com.sixpay.integration.http.IntegrationHttpHeaders` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\tresorpay\TresorPayRequestGuard.java`
- `com.sixpay.integration.http.IntegrationHttpHeaders` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\tresorpay\TresorPayRequestGuardFilter.java`
- `com.sixpay.integration.event.DistributedEventEnvelope` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\event\distributed\PaymentDistributedEventFactory.java`
- `com.sixpay.integration.event.PayloadClassification` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\event\distributed\PaymentDistributedEventFactory.java`
- `com.sixpay.integration.http.HttpTimeoutPolicy` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\compensation\AmplitudeCompensationConfiguration.java`
- `com.sixpay.integration.http.StandardRestClientFactory` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\compensation\AmplitudeCompensationConfiguration.java`
- `com.sixpay.integration.http.IntegrationHttpHeaders` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\status\client\RestAmplitudePostingStatusClient.java`
- `com.sixpay.integration.http.HttpTimeoutPolicy` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\status\configuration\AmplitudePostingStatusConfiguration.java`
- `com.sixpay.integration.http.StandardRestClientFactory` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\status\configuration\AmplitudePostingStatusConfiguration.java`
- `com.sixpay.integration.http.IntegrationHttpHeaders` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\reversal\client\RestAmplitudeReversalClient.java`
- `com.sixpay.integration.http.IntegrationHttpHeaders` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\reservation\client\RestAmplitudeFundsReservationClient.java`
- `com.sixpay.integration.http.HttpTimeoutPolicy` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\reservation\configuration\AmplitudeFundsReservationConfiguration.java`
- `com.sixpay.integration.http.StandardRestClientFactory` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\reservation\configuration\AmplitudeFundsReservationConfiguration.java`
- `com.sixpay.integration.http.IntegrationHttpHeaders` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\release\client\RestAmplitudeFundsReleaseClient.java`
- `com.sixpay.integration.http.IntegrationHttpHeaders` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\posting\client\RestAmplitudePostingClient.java`
- `com.sixpay.integration.http.HttpTimeoutPolicy` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\posting\configuration\AmplitudePostingConfiguration.java`
- `com.sixpay.integration.http.StandardRestClientFactory` — `backend\payment\src\main\java\com\sixpay\payment\infrastructure\banking\amplitude\posting\configuration\AmplitudePostingConfiguration.java`

### Payment → Security

**✅ public security contract (REVIEW API SURFACE)**

- `com.sixpay.security.authentication.CurrentUserProvider` — `backend\payment\src\main\java\com\sixpay\payment\api\PaymentCommandController.java`
- `com.sixpay.security.authentication.AuthenticatedUser` — `backend\payment\src\main\java\com\sixpay\payment\application\security\PaymentAccessPolicy.java`
- `com.sixpay.security.authentication.CurrentUserProvider` — `backend\payment\src\main\java\com\sixpay\payment\application\security\PaymentAccessPolicy.java`
- `com.sixpay.security.authentication.AuthenticatedUser` — `backend\payment\src\main\java\com\sixpay\payment\application\security\PaymentPartnerIsolationPolicy.java`
- `com.sixpay.security.authorization.SixpayRole` — `backend\payment\src\main\java\com\sixpay\payment\application\security\PaymentPartnerIsolationPolicy.java`
- `com.sixpay.security.authentication.AuthenticatedUser` — `backend\payment\src\main\java\com\sixpay\payment\application\security\PaymentRolePolicy.java`
- `com.sixpay.security.authorization.SixpayRole` — `backend\payment\src\main\java\com\sixpay\payment\application\security\PaymentRolePolicy.java`

### Security → Common

**✅ common/platform contract**

- `com.sixpay.common.validation.Preconditions` — `backend\security\src\main\java\com\sixpay\security\authentication\AuthenticatedUser.java`
- `com.sixpay.common.time.SystemTimeProvider` — `backend\security\src\main\java\com\sixpay\security\configuration\LocalAuthenticationConfiguration.java`
- `com.sixpay.common.time.TimeProvider` — `backend\security\src\main\java\com\sixpay\security\configuration\LocalAuthenticationConfiguration.java`
- `com.sixpay.common.time.TimeProvider` — `backend\security\src\main\java\com\sixpay\security\infrastructure\authentication\persistence\JpaLocalAuthenticationUserAdapter.java`
- `com.sixpay.common.validation.Preconditions` — `backend\security\src\main\java\com\sixpay\security\domain\authentication\ExternalIdentity.java`
- `com.sixpay.common.validation.Preconditions` — `backend\security\src\main\java\com\sixpay\security\domain\authentication\LinkedUserIdentity.java`
- `com.sixpay.common.validation.Preconditions` — `backend\security\src\main\java\com\sixpay\security\domain\authentication\LocalAuthenticationAuditEvent.java`
- `com.sixpay.common.validation.Preconditions` — `backend\security\src\main\java\com\sixpay\security\domain\authentication\LocalAuthenticationUser.java`
- `com.sixpay.common.validation.Preconditions` — `backend\security\src\main\java\com\sixpay\security\domain\authentication\LocalCredential.java`
- `com.sixpay.common.validation.Preconditions` — `backend\security\src\main\java\com\sixpay\security\domain\authentication\SixpayUserAccount.java`
- `com.sixpay.common.validation.Preconditions` — `backend\security\src\main\java\com\sixpay\security\domain\authentication\UserIdentity.java`
- `com.sixpay.common.validation.Preconditions` — `backend\security\src\main\java\com\sixpay\security\application\model\PasswordHistorySnapshot.java`
- `com.sixpay.common.time.TimeProvider` — `backend\security\src\main\java\com\sixpay\security\application\service\LocalAuthenticationService.java`
- `com.sixpay.common.time.TimeProvider` — `backend\security\src\main\java\com\sixpay\security\application\service\LocalLogoutService.java`
- `com.sixpay.common.time.TimeProvider` — `backend\security\src\main\java\com\sixpay\security\application\service\LocalPasswordChangeService.java`
- `com.sixpay.common.validation.Preconditions` — `backend\security\src\main\java\com\sixpay\security\application\port\in\LocalLoginCommand.java`

## Platform dependencies

- **Partner → Common**: ✅ common/platform contract
- **Customer → Common**: ✅ common/platform contract
- **Customer → Integration**: ✅ provider-neutral integration
- **Payment → Common**: ✅ common/platform contract
- **Payment → Integration**: ✅ provider-neutral integration
- **Accounting → Common**: ✅ common/platform contract
- **Accounting → Integration**: ✅ provider-neutral integration
- **Notification → Common**: ✅ common/platform contract
- **Security → Common**: ✅ common/platform contract
- **Administration → Common**: ✅ common/platform contract

## Circular dependency analysis

No circular business-module dependency discovered from production Java imports.

## Initial audit result

- Production cross-module edges discovered: **14**
- Warning-level imports requiring review: **0**
- Circular business-module dependencies: **0**

FS-2.4.0 is an inventory only. A warning does not automatically mean the implementation is wrong; it identifies an edge that must be classified against the owning module's public API.

## Required next step

FS-2.4.1 must review every business-to-business edge and define the allowed public package surface. Infrastructure, JPA entity and repository imports across modules must be eliminated.
