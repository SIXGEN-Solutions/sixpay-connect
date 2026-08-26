#!/usr/bin/env python3
"""Add persistence sections without replacing existing README content.

Existing backend README files are preserved byte-for-byte except for the
new section appended at the end. Bootstrap, common and shared-kernel READMEs
are created only when they do not exist.
"""

from __future__ import annotations

import argparse
import hashlib
import shutil
import tempfile
from pathlib import Path
from textwrap import dedent


SECTIONS = {
    "backend/accounting/README.md": """
## Persistence ownership

Accounting owns these production tables:

| Table | Purpose |
|---|---|
| accounting_batches | Accounting batch identity and submission state |
| accounting_batch_items | Payment items assigned to a batch |
| accounting_batch_tracking | Batch reconciliation tracking |
| accounting_batch_item_tracking | Item-level reconciliation tracking |

Schema:
backend/accounting/src/main/resources/db/migration/V400__accounting_baseline.sql
""",
    "backend/administration/README.md": """
## Persistence ownership

Administration owns these production tables:

| Table | Purpose |
|---|---|
| operational_incident | Operational incident state |
| operational_incident_timeline | Incident timeline entries |

Security-owned users, identities, credentials and authorization tables are not
duplicated by Administration.

Schema:
backend/administration/src/main/resources/db/migration/V800__administration_baseline.sql
""",
    "backend/customer/README.md": """
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
""",
    "backend/integration/README.md": """
## Persistence ownership

Integration owns no production business tables.

Outbox records remain owned by the originating business module. Integration
only claims and transports those records. Test-only migration fixtures do not
define production ownership.
""",
    "backend/notification/README.md": """
## Persistence ownership

Notification owns these production table families:

| Table/family | Purpose |
|---|---|
| notification_deliveries | Partner notification delivery state |
| sixpay.operational_notification_deliveries | Operational notification state |
| sixpay.operational_notification_attempts | Delivery attempts |
| sixpay.operational_notification_replays | Replay requests |

Schema:
backend/notification/src/main/resources/db/migration/V600__notification_baseline.sql
""",
    "backend/partner/README.md": """
## Persistence ownership

Partner owns these production tables:

| Table | Purpose |
|---|---|
| partners | Partner aggregate |
| partner_authorized_perimeters | Partner access perimeters |
| partner_validation_thresholds | Current validation thresholds |
| partner_validation_threshold_history | Immutable threshold history |
| partner_audit | Partner audit records |
| partner_idempotency | Mutation idempotency records |
| partner_outbox_events | Partner integration events |

Schema:
backend/partner/src/main/resources/db/migration/V100__partner_baseline.sql
""",
    "backend/payment/README.md": """
## Persistence ownership

Payment owns these production tables:

| Table | Purpose |
|---|---|
| payments | Payment aggregate and lifecycle |
| payment_audit | Immutable Payment audit |
| payment_outbox_events | Payment integration events |
| payment_idempotency | Command idempotency and replay data |
| payment_observed_customer_link | Link to an ObservedCustomer projection |

Payment does not own Customer, CustomerSubscription, Accounting or Reporting
tables.

Schema:
backend/payment/src/main/resources/db/migration/V300__payment_baseline.sql
""",
    "backend/reporting/README.md": """
## Persistence ownership

Reporting owns these production tables:

| Table | Purpose |
|---|---|
| reporting_payment_audit_evidence | Payment audit read evidence |
| reporting_payment_audit_export_job | Controlled audit export jobs |

Reporting does not update Payment-owned tables or persist financial state.

Schema:
backend/reporting/src/main/resources/db/migration/V500__reporting_baseline.sql
""",
    "backend/security/README.md": """
## Persistence ownership

Security owns these production tables:

| Table/family | Purpose |
|---|---|
| security_user_accounts | Canonical SIXPAY accounts |
| security_user_identities | Local and external identity links |
| security_local_users | Local credentials and state |
| security_user_roles | Role assignments |
| security_user_permissions | Permission assignments |
| security_password_history | Password history |
| security_authentication_audit | Authentication audit |
| security_audit_events | Security and authorization audit |

Administration exposes management HTTP boundaries but does not own these tables.

Schema:
backend/security/src/main/resources/db/migration/V700__security_baseline.sql
""",
    "backend/tests/README.md": """
## Persistence ownership

The tests module owns no production tables or Flyway baseline. Test-only
migration fixtures may exist, but production schema ownership remains with the
owning backend module.
""",
}


NEW_READMES = {
    "backend/bootstrap/README.md": """
# Bootstrap Module

## Purpose

Bootstrap is the executable composition module for SIXPAY CONNECT. It assembles
the platform and business modules, loads runtime configuration, starts Spring
Boot and exposes the application entry point.

Bootstrap owns composition and runtime wiring. It does not own business rules,
business persistence or provider-specific mappings.
""",
    "backend/common/README.md": """
# Common Module

## Purpose

Common contains small framework-independent technical contracts shared across
SIXPAY CONNECT modules, such as correlation, time, identifier and Outbox
source abstractions.

Common remains domain-neutral and owns no business persistence.
""",
    "backend/shared-kernel/README.md": """
# Shared Kernel Module

## Purpose

Shared Kernel contains intentionally shared domain primitives, such as
aggregate roots, domain events, domain exceptions and shared value objects.

New business behavior remains in its owning module. Shared Kernel owns no
business persistence.
""",
}


def normalized(value: str) -> str:
    return dedent(value).strip() + "\n"


def append_once(path: Path, section: str) -> bool:
    original = path.read_text(encoding="utf-8")
    if "## Persistence ownership" in original:
        return False

    separator = "" if original.endswith("\n\n") else "\n"
    updated = original + separator + normalized(section)
    path.write_text(updated, encoding="utf-8", newline="")
    return True


def validate(root: Path, before_hashes: dict[Path, str]) -> None:
    required_tokens = {
        "backend/accounting/README.md": ("accounting_batches", "accounting_batch_items"),
        "backend/administration/README.md": ("operational_incident",),
        "backend/customer/README.md": ("customer_management_customer", "customer_management_subscription"),
        "backend/notification/README.md": ("operational_notification_deliveries",),
        "backend/partner/README.md": ("partners", "partner_outbox_events"),
        "backend/payment/README.md": ("payments", "payment_outbox_events"),
        "backend/reporting/README.md": ("reporting_payment_audit_evidence",),
        "backend/security/README.md": ("security_user_accounts",),
    }
    for relative, expected in SECTIONS.items():
        path = root / relative
        text = path.read_text(encoding="utf-8")
        if "## Persistence ownership" not in text:
            raise RuntimeError("Missing persistence section: " + relative)
        for token in required_tokens.get(relative, ()):
            if token not in text:
                raise RuntimeError(f"{token} missing from {relative}")

    for relative in NEW_READMES:
        path = root / relative
        if not path.is_file() or "## Purpose" not in path.read_text(encoding="utf-8"):
            raise RuntimeError("Invalid generated README: " + relative)

    for path, before in before_hashes.items():
        if not path.is_file():
            raise RuntimeError("Existing README disappeared: " + str(path))
        after = path.read_bytes()
        if hashlib.sha256(after).hexdigest() == before:
            continue
        if "## Persistence ownership" not in path.read_text(encoding="utf-8"):
            raise RuntimeError("Unexpected change in existing README: " + str(path))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("repository", nargs="?", default=".",
                        help="repository root (default: current directory)")
    root = Path(parser.parse_args().repository).resolve()

    existing = {root / relative for relative in SECTIONS}
    missing = [str(path.relative_to(root)) for path in existing if not path.is_file()]
    if missing:
        raise SystemExit("Missing existing backend README:\n- " + "\n- ".join(missing))

    backup_dir = Path(tempfile.mkdtemp(prefix="sixpay-readme-additive-"))
    before_hashes = {}
    changed = []

    for path in existing:
        before_hashes[path] = hashlib.sha256(path.read_bytes()).hexdigest()
        shutil.copy2(path, backup_dir / path.name)

    for relative, section in SECTIONS.items():
        if append_once(root / relative, section):
            changed.append(relative)

    for relative, content in NEW_READMES.items():
        path = root / relative
        if not path.exists():
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(normalized(content), encoding="utf-8", newline="")
            changed.append(relative)

    validate(root, before_hashes)

    print("Additive backend README update completed.")
    for relative in changed:
        print(" - " + relative)
    if not changed:
        print(" - no changes (already aligned)")
    print("Existing README content was preserved.")
    print("Temporary backups: " + str(backup_dir))
    print("No commit or push was performed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
