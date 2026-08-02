#!/usr/bin/env python3
from pathlib import Path
import re
import shutil
import sys

repo = Path(sys.argv[1]).resolve()

java_root = (
    repo
    / "backend/payment/src/main/java/com/sixpay/payment/"
      "infrastructure"
)

old_root = java_root / "outbox"
new_root = java_root / "callback/relay"
new_root.mkdir(parents=True, exist_ok=True)

files = [
    "ClaimedPaymentOutboxEvent.java",
    "PaymentCallbackOutboxCoordinator.java",
    "PaymentCallbackOutboxRelay.java",
    "PaymentCallbackPlan.java",
    "PaymentCallbackPlanFactory.java",
]

for filename in files:
    source = old_root / filename
    target = new_root / filename

    if source.is_file():
        content = source.read_text(encoding="utf-8")
        content = content.replace(
            "package com.sixpay.payment.infrastructure.outbox;",
            "package com.sixpay.payment.infrastructure.callback.relay;"
        )

        # The relocated callback relay consumes the transport-neutral outbox.
        imports = []
        if filename in {
            "PaymentCallbackOutboxCoordinator.java",
            "PaymentCallbackPlanFactory.java",
        }:
            imports.extend([
                "import com.sixpay.payment.infrastructure.outbox.PaymentOutboxEntity;",
                "import com.sixpay.payment.infrastructure.outbox.PaymentOutboxRepository;",
            ])

        if imports:
            package_line = (
                "package com.sixpay.payment.infrastructure.callback.relay;\n"
            )
            content = content.replace(
                package_line,
                package_line + "\n" + "\n".join(imports) + "\n",
                1
            )

        target.write_text(
            content,
            encoding="utf-8",
            newline="\n"
        )
        source.unlink()
    elif not target.is_file():
        raise SystemExit(
            f"Missing callback relay file: {source}"
        )

entity = old_root / "PaymentOutboxEntity.java"
entity_content = entity.read_text(encoding="utf-8")

for method in (
    "void claim(",
    "void markPublished(",
    "void markFailed(",
    "void markDead(",
):
    entity_content = entity_content.replace(
        "    " + method,
        "    public " + method,
        1
    )

entity.write_text(
    entity_content,
    encoding="utf-8",
    newline="\n"
)

architecture_test = (
    repo
    / "backend/payment/src/test/java/com/sixpay/payment/"
      "architecture/PaymentAsyncCallbackArchitectureTest.java"
)

if architecture_test.is_file():
    content = architecture_test.read_text(encoding="utf-8")
    content = content.replace(
        '"infrastructure/outbox/"\n'
        '                                + "PaymentCallbackOutboxRelay.java"',
        '"infrastructure/callback/relay/"\n'
        '                                + "PaymentCallbackOutboxRelay.java"'
    )
    architecture_test.write_text(
        content,
        encoding="utf-8",
        newline="\n"
    )

# Guard against stale compiled classes.
target = repo / "backend/payment/target"
if target.exists():
    shutil.rmtree(target)

print("Callback relay moved outside the transport-neutral outbox package.")
