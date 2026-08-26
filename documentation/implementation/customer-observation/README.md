# Customer Observation

## Purpose

This directory contains implementation notes and operational evidence for the
Customer observation capability. The current implementation is owned by the
Customer module.

## Scope

Customer observation provides read-oriented ObservedCustomer projections,
search, detail, payment history and controlled links between an observed
banking identity and a local Customer.

ObservedCustomer is not the canonical banking identity and does not replace
fresh verification against the banking provider.

## Ownership boundaries

- Customer owns observation queries, projections and links.
- Customer verification owns provider interaction and mapping.
- Payment consumes the defined customer verification result but does not own
  the observation projection.
- Security owns authentication and authorization.

## Validation

Run the Customer module tests from backend:

    mvn -pl customer -am test
    mvn -pl customer -am clean verify
