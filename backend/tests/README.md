# Backend Test Foundation

## Purpose

The tests module hosts tests that require multiple bounded contexts or the
assembled application. Module-local tests remain beside the implementation
they verify.

## Responsibilities

- assembled Spring application-context verification;
- contract-backed cross-module integration tests;
- full-stack persistence and security integration tests;
- repository-level coverage and architecture gates.

## Ownership rule

Domain, application, API and persistence behavior is tested in the owning
module. The tests module verifies only cross-module or assembled behavior.

## Execution

From backend:

    mvn -pl tests test
    mvn -pl tests -Pfull-tests verify

Repository-wide verification:

    python scripts/verify_baseline.py

## Persistence ownership

The tests module owns no production tables or Flyway baseline. Test-only
migration fixtures may exist, but production schema ownership remains with the
owning backend module.
