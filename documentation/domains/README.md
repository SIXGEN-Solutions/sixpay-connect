# SIXPAY CONNECT — Domain Documentation

This directory is reserved for **validated current-state domain documentation**.

## Current state

Tracked domain documentation exists only where validated durable domain
knowledge has been explicitly produced. Empty domain placeholders are not
retained.

## Domain documentation rule

A domain document belongs here only when it describes durable business/domain
knowledge that is useful independently from raw requirements, architecture
decisions, physical contracts, implementation source code and AI working notes.

Examples include domain vocabulary, aggregates and invariants, business state
transitions, ownership, domain events and business policies.

## Source precedence

Domain documentation must remain consistent with the authoritative
implementation revision, architecture, requirements and contracts.

## Golden implementation reference

`backend/partner` remains the golden business-module implementation reference.

## No duplication rule

Do not copy raw CDC/user-story content into a domain README merely to populate
the tree.

## Current tracked domain material

The current tree includes Payment policy documentation and Accounting T1 domain
documentation. `payment/PAYMENT_POLICY_BASELINE.md` is the canonical
`payment-mvp/v1` business-policy reference.
