# Customer Module

## Purpose

The Customer module owns customer enrollment and management, local customer
subscription management, current customer verification and ObservedCustomer
read projections.

## Responsibilities

- create, list, retrieve, update, suspend, reactivate and delete customers;
- manage customer bank accounts and the default account;
- create, activate, suspend, retrieve, list and close CustomerSubscription;
- verify customer and account information through the owning banking adapter;
- link and unlink ObservedCustomer records to local customers;
- expose customer observation and customer audit queries.

The external TRESOR PAY subscription remains outside the Payment MVP. It must
not be confused with the local CustomerSubscription capability owned by this
module.

## APIs

Customer management:

    /internal/api/v1/customers

Customer subscriptions:

    /internal/api/v1/subscriptions

Customer observation:

    /internal/api/v1/observed-customers

Customer audit:

    /internal/api/v1/customer-audit-records

Active contracts include:

- documentation/contracts/internal/customer-management-query-api-v1.yaml;
- documentation/contracts/internal/customer-subscription-management-api-v1.yaml;
- documentation/contracts/internal/observed-customer-query-api-v1.yaml.

## Boundaries

- Amplitude-specific verification clients and mappings remain in Customer.
- ObservedCustomer is a read projection, not the canonical banking identity.
- CustomerSubscription is local to Customer and is not a Payment aggregate.
- Security owns authentication and authorization.
- Cross-module collaboration uses application ports and published contracts.

## Structure

Customer follows the Partner reference layering. Its main capability areas
are management, verification and observation, each with explicit api,
application, domain, infrastructure and configuration boundaries.

## Validation

From backend:

    mvn -pl customer -am test
    mvn -pl customer -am clean verify
    mvn -pl customer -am -Pfull-tests clean verify

The full-tests command requires Docker when PostgreSQL integration tests are
selected.

## Persistence ownership

Customer owns these production table families:

| Table/family | Purpose |
|---|---|
| customer_management_customer | Local customer lifecycle |
| customer_management_bank_account | Customer bank-account references |
| customer_management_subscription | Local CustomerSubscription lifecycle |
| customer_management_audit | Customer-management audit |
| customer_observed_customer | ObservedCustomer projection |
| customer_observed_institution | Observed institution projection |
| customer_observed_account | Observed account projection |
| customer_observed_payment | Observed payment projection |
| customer_observation_processed_event | Observation idempotency |
| customer_observation_audit | Observation audit |
| customer_observed_master_link | Observed/local customer link |

The external TRESOR PAY subscription is not stored as a local
CustomerSubscription record.

Schema:
backend/customer/src/main/resources/db/migration/V200__customer_baseline.sql

## C1 — Internal subscription resolution for Payment

`referenceClient` is the existing `CustomerSubscriptionId` (UUID); no additional
identifier or database column is created. Customer exposes an **in-process**
`ResolveCustomerSubscriptionUseCase` taking the reference and the **authenticated**
partner UUID. Its result is an application DTO, not a JPA entity.

Resolution enforces partner ownership, `ACTIVE` subscription, `ACTIVE` customer
and membership of the bank account selected by the subscription. Failures are
reported without exposing another partner's subscription details. The trusted
caller must supply the authenticated partner UUID; a partner/application header
alone is not authentication.

The result includes optional NIU, optional banking customer number, canonical
banking customer reference and `bankingAccountReference`. By approved SIXPAY
business convention, `bankingAccountReference` represents the RIB: there is no
second `rib` field or database column. Any provider-specific naming or format
translation belongs to the owning banking adapter, subject to its contract.

C1 deliberately does not change the customer-verification identity/value objects,
Amplitude API contract or Payment orchestration. Those changes require separate
contract and security approval before the subsequent integration lot.

### Partner and application scope (C1 decision)

A CustomerSubscription is valid for **all authenticated applications belonging
to its partner**. Customer resolves the subscription using `referenceClient`
(the existing `CustomerSubscriptionId`) and the **trusted authenticated partner
UUID**. Customer does not bind subscriptions to individual application IDs and
does not authorize applications: Security must authenticate and authorize the
calling application and establish its partner before this port is invoked.
Application-provided IDs or headers must never be treated as proof of partner
ownership. The resolution rejects missing, inactive or cross-partner subscriptions,
inactive customers and missing subscription-bound bank accounts.

Payment integration, removal of mandatory NIU from the banking-verification flow,
and changes to verification contracts/DTOs belong to P3, not this C1 patch.
