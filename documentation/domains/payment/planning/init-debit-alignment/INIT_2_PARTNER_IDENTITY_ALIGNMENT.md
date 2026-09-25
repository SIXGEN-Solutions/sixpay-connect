# SIXPAY CONNECT — INIT-2
## Partner Identity Alignment

### Reference revision

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Branch: `feat/repository-baseline-consolidation-cucumber`
- SHA: `3857f80711e5e7d5c36abd627d00f6aa9e7d79e1`
- Mode: bounded implementation + architecture alignment
- Commit/push: none
- Worktree check: not performed by request
- Gates: not executed by request

---

# 1. Goal

Decouple Partner business identity from Security login while preserving strict
module boundaries.

Target:

```text
Security principal
      ↓
reviewed Partner resolution surface
      ↓
canonical partnerIdentifier
      ↓
Payment consistency check against request AppID
```

Payment must never access Partner repository, JPA or infrastructure.

---

# 2. Observed baseline

## Partner

The current Partner aggregate is identified only by:

```text
PartnerId(UUID)
```

The `partners` table has no observed `partner_identifier` business column.

The current public query surface supports lookup by technical `PartnerId`, not
by canonical business identifier.

## Security

`AuthenticatedUser.subject()` is the canonical SIXPAY account subject.

No observed authoritative source at this revision establishes that the
Security subject equals the Partner business identifier.

## Payment

Payment currently compares request `LoginName` with authenticated username and
uses Security subject for Partner visibility/isolation.

This couples Security identity semantics to Partner ownership.

---

# 3. Implemented public surfaces

INIT-2 introduces:

```text
PartnerIdentityQueryUseCase
PartnerIdentityView
```

as Partner-owned public application contracts.

Payment receives its own outbound boundary:

```text
PartnerIdentityResolutionPort
ResolvedPartnerIdentity
PartnerIdentityAlignmentService
```

Bootstrap provides composition:

```text
PaymentPartnerIdentityModuleAdapter
PaymentPartnerIdentityConfiguration
```

Dependency direction:

```text
Payment ──> Payment-owned port
                ↑
             Bootstrap
              /     \
         Security   Partner public application surface
```

There is no:

```text
Payment -> PartnerRepository
Payment -> PartnerJpaEntity
Payment -> Partner infrastructure
```

---

# 4. Deliberately blocked implementation

The repository does not currently contain an approved durable
`partnerIdentifier` distinct from the UUID `PartnerId`.

Therefore INIT-2 does **not** invent:

- a `partner_identifier` database column;
- a new Flyway migration;
- a new Partner creation field;
- a mapping from Security account to Partner;
- a new Security role/permission;
- a heuristic based on username, email or UUID equality.

The Partner query contract is defined, but its concrete implementation cannot
be completed safely until the business identifier storage model is approved.

Likewise, the Bootstrap adapter cannot complete
`authenticated Security principal -> Partner` resolution until an approved
linking source exists.

---

# 5. Required human decisions

Before closing INIT-2 completely:

1. confirm the durable `partnerIdentifier` representation;
2. confirm whether it is:
   - an explicit new Partner field, or
   - an already-existing authoritative identifier not yet exposed;
3. confirm uniqueness and normalization rules;
4. confirm how a SIXPAY Security account is linked to a Partner:
   - explicit partner reference on the user/account,
   - dedicated linking relation,
   - another approved source;
5. approve any required schema/migration/security-model change.

---

# 6. Consistency rule owned by Payment

Once both resolutions are available, Payment must enforce:

```text
resolved authenticated Partner
        ==
Partner resolved from request AppID
```

Mismatch classification:

```text
PARTNER_IDENTITY_MISMATCH
```

Partner unavailable / unauthorized remain separate internal classifications.

These are internal application classifications in INIT-2, not public
ProblemDetail codes.

---

# 7. Contract impact

INIT-2 does not modify the public Payment OpenAPI.

It prepares the runtime boundary required by INIT-1's contract proposal.

No public contract, registry entry, database schema or migration is changed.

---

# 8. Files created

```text
backend/partner/src/main/java/com/sixpay/partner/application/port/input/PartnerIdentityQueryUseCase.java
backend/partner/src/main/java/com/sixpay/partner/application/view/PartnerIdentityView.java

backend/payment/src/main/java/com/sixpay/payment/application/port/output/partner/PartnerIdentityResolutionPort.java
backend/payment/src/main/java/com/sixpay/payment/application/port/output/partner/ResolvedPartnerIdentity.java
backend/payment/src/main/java/com/sixpay/payment/application/service/PartnerIdentityAlignmentService.java
backend/payment/src/main/java/com/sixpay/payment/application/service/PartnerIdentityResolutionFailure.java
backend/payment/src/main/java/com/sixpay/payment/application/service/PartnerIdentityResolutionException.java

backend/bootstrap/src/main/java/com/sixpay/bootstrap/integration/partner/PaymentPartnerIdentityModuleAdapter.java
backend/bootstrap/src/main/java/com/sixpay/bootstrap/integration/partner/PaymentPartnerIdentityConfiguration.java

documentation/domains/payment/planning/init-debit-alignment/INIT_2_PARTNER_IDENTITY_ALIGNMENT.md
```

Updated:

```text
documentation/domains/payment/planning/init-debit-alignment/README.md
```

---

# 9. Status

```text
INIT-2 — PUBLIC SURFACES IMPLEMENTED
BLOCKED ON PARTNER IDENTIFIER PERSISTENCE AND SECURITY→PARTNER LINK
```

This is intentionally not reported as fully complete.


## INIT-2A completion

The Partner business identity is explicit: `PartnerId` is the internal UUID and
`partnerIdentifier` is mandatory, stable and unique. The remaining INIT-2
concern is the reviewed Security/machine identity -> Partner link; INIT-2A does
not introduce a username/subject heuristic.
