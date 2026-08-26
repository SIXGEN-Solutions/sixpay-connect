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
