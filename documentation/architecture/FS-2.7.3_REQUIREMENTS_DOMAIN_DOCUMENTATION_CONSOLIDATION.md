# FS-2.7.3 — Requirements / Domain Documentation Consolidation

## Objective

Separate requirement-source ownership from current-state domain documentation
without inventing content or changing source precedence.

## Findings

### Requirements

The requirements tree currently contains source documents in two families:

```text
documentation/requirements/cdc/
documentation/requirements/user-stories/
```

The CDC area contains the interoperability requirements, the SIXPAY CDC and the
functional specification PDF.

The user-stories area contains the SIXPAY user stories DOCX.

These are preserved as requirement/reference sources.

### Domains

`documentation/domains/` currently contains only:

```text
documentation/domains/customer/.gitkeep
```

There is no substantial domain-documentation corpus to consolidate yet.

## Decisions

| Area | Decision |
|---|---|
| `requirements/cdc/*` | `KEEP_REFERENCE_SOURCE` |
| `requirements/user-stories/*` | `KEEP_REFERENCE_SOURCE` |
| `requirements/README.md` | `KEEP_CANONICAL` |
| `domains/README.md` | `KEEP_CANONICAL` |
| `domains/customer/.gitkeep` | `KEEP_PLACEHOLDER` |

No binary requirement source is deleted in FS-2.7.3.

## Boundary between documentation types

```text
Requirements
    = intent / functional source

Domains
    = validated durable business/domain knowledge

Architecture
    = technical/architectural decisions

Contracts
    = physical interfaces + registry lifecycle

Implementation
    = authoritative actual behavior
```

## Non-invention rule

FS-2.7.3 deliberately does not synthesize domain models from implementation
classes or AI notes. Missing domain documentation remains explicit rather than
being filled with assumptions.

## Follow-up

Later FS-2.7 cleanup may archive/delete requirement sources only when there is
proof their useful content is completely absorbed into validated canonical
artifacts.
