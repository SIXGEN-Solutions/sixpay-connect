# SIXPAY CONNECT — Payment Backend Source Baseline

## Baseline identity

| Field | Value |
| --- | --- |
| Repository | `SIXGEN-Solutions/sixpay-connect` |
| Authoritative branch | `feat/payment-domain-generation-brief` |
| Mandatory entry point | `ENGINEERING_CONTEXT.md` |
| Baseline mode | Branch reference plus inspected blob identities |
| Delivery scope | Phase 3, Lot 3.1 — Backend Foundation |

The delivery environment could read repository files through the GitHub
connector but could not resolve a local clone. The branch name remains the
implementation authority. Blob identities below make the inspected inputs
traceable without pretending that an unobserved commit SHA was captured.

## Inspected authoritative inputs

| Repository path | Inspected blob SHA | Role |
| --- | --- | --- |
| `ENGINEERING_CONTEXT.md` | `0d82bc2fc7e56d02219f2020d5127fb22dde8972` | Repository workflow and precedence |
| `backend/payment/pom.xml` | `d7e2a0a0babacc93761e3e7bbab106403f55427f` | Existing Payment dependency baseline |
| `backend/partner/pom.xml` | `b309dd17244cc6b7f53280c226a7ed73f0aac6b2` | Golden Module dependency convention |
| `backend/payment/src/test/java/com/sixpay/payment/architecture/PaymentArchitectureTest.java` | `d9a77ec76cd5697f2b0d366c71792eadce3cc829` | Existing frozen-domain architecture gate |

## Existing Payment baseline

The Payment module currently provides a frozen domain-only implementation with:

```text
PaymentModule marker
Payment aggregate and immutable PaymentState
17 named operations
17 states
38 legal transitions
76 invariant and transition checks
33 Domain Events
14 Policies
12 Policy Profiles
4 Domain Services
```

The existing `PaymentArchitectureTest` already protects the aggregate,
canonical domain packages, event catalogue, policy/service counts, named
operations and absence of framework or infrastructure dependencies in the
domain.

## Lot 3.1 delta

This lot adds only:

1. backend dependencies already established by the Golden Partner module;
2. documented package boundaries for future vertical layers;
3. an additional architecture test focused on Phase 3 foundation rules;
4. planning, authorization and baseline documentation.

No existing Payment domain source is included in the delivery ZIP and no
business behavior is changed.

## Copy and validation procedure

1. Confirm the local branch is `feat/payment-domain-generation-brief`.
2. Confirm the worktree has no unrelated changes.
3. Copy the contents of `files-to-copy/` to the repository root, preserving
   paths.
4. Review the diff and verify that no file under
   `backend/payment/src/main/java/com/sixpay/payment/domain/` changed.
5. Run:

```bash
cd backend
mvn --batch-mode --no-transfer-progress -pl payment -am test
```

6. Record the observed result in the project delivery report. Do not mark the
   lot successful if the command was not executed or failed.
