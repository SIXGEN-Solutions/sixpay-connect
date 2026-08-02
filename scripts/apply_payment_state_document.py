#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(sys.argv[1]).resolve()

def load(relative):
    path = root / relative
    if not path.is_file():
        raise SystemExit(f"Missing expected file: {relative}")
    return path, path.read_text(encoding="utf-8")

def save(path, source):
    path.write_text(source, encoding="utf-8", newline="\n")

def replace_once(source, old, new, label):
    if new in source:
        return source
    if old not in source:
        raise SystemExit(f"Patch point changed: {label}")
    return source.replace(old, new, 1)

# ---------------------------------------------------------------------------
# PaymentState
# ---------------------------------------------------------------------------
path, source = load(
    "backend/payment/src/main/java/com/sixpay/payment/domain/model/"
    "PaymentState.java"
)

source = replace_once(
    source,
    """        if (reversalInstruction != null) {
            if (reversalAuthorizationEvidence == null
                    || reversalEvidence == null
                    || !reversalInstruction.instructionId().equals(
                            reversalEvidence.reversalInstructionId()
                    )
                    || !reversalInstruction.idempotencyKey().equals(
                            reversalEvidence
                                    .reversalCommandIdempotencyKey()
                    )) {
                throw new IllegalArgumentException(
                        "Reversal state is structurally inconsistent"
                );
            }
        }

        validateLifecycleCoherence();
""",
    """        if (reversalInstruction != null) {
            if (reversalAuthorizationEvidence == null
                    || reversalEvidence == null
                    || !reversalInstruction.instructionId().equals(
                            reversalEvidence.reversalInstructionId()
                    )
                    || !reversalInstruction.idempotencyKey().equals(
                            reversalEvidence
                                    .reversalCommandIdempotencyKey()
                    )) {
                throw new IllegalArgumentException(
                        "Reversal state is structurally inconsistent"
                );
            }
        }

        validateInitiationAndConfirmationCoherence();
        validateLifecycleCoherence();
""",
    "PaymentState validation invocation"
)

if "private void validateInitiationAndConfirmationCoherence()" not in source:
    marker = """    private void validateLifecycleCoherence() {
"""
    method = """    private void validateInitiationAndConfirmationCoherence() {
        if (status == PaymentStatus.PENDING_CONFIRMATION
                && initiationContext == null) {
            throw new IllegalArgumentException(
                    "PENDING_CONFIRMATION requires initiation context"
            );
        }

        if (customerConfirmationEvidence != null
                && initiationContext == null) {
            throw new IllegalArgumentException(
                    "Customer confirmation evidence requires initiation context"
            );
        }

        if (customerConfirmationEvidence != null) {
            Instant confirmedAt =
                    customerConfirmationEvidence.confirmedAt();

            if (confirmedAt.isBefore(receivedAt)) {
                throw new IllegalArgumentException(
                        "Customer confirmation evidence must not precede receipt"
                );
            }
            if (confirmedAt.isAfter(updatedAt)) {
                throw new IllegalArgumentException(
                        "Customer confirmation evidence must not follow updatedAt"
                );
            }
        }

        if (status == PaymentStatus.RECEIVED
                && customerConfirmationEvidence != null) {
            throw new IllegalArgumentException(
                    "RECEIVED must not contain customer confirmation evidence"
            );
        }

        if (status == PaymentStatus.PENDING_CONFIRMATION
                && customerConfirmationEvidence != null) {
            throw new IllegalArgumentException(
                    "PENDING_CONFIRMATION must not contain accepted confirmation evidence"
            );
        }

        /*
         * A state without initiationContext is a legacy pre-command-API state.
         * Once the new context exists, every state after confirmation must
         * retain the accepted bank evidence.
         */
        if (initiationContext != null
                && status != PaymentStatus.RECEIVED
                && status != PaymentStatus.PENDING_CONFIRMATION
                && customerConfirmationEvidence == null) {
            throw new IllegalArgumentException(
                    "Confirmed initiated Payment requires customer confirmation evidence"
            );
        }
    }

"""
    if marker not in source:
        raise SystemExit("PaymentState lifecycle method anchor changed")
    source = source.replace(marker, method + marker, 1)

save(path, source)

# ---------------------------------------------------------------------------
# PaymentStateDocument
# ---------------------------------------------------------------------------
path, source = load(
    "backend/payment/src/main/java/com/sixpay/payment/"
    "infrastructure/persistence/PaymentStateDocument.java"
)

source = replace_once(
    source,
    """    PaymentState toState() {
        if (schemaVersion < 1
                || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new PaymentPersistenceException(
                    "Unsupported Payment state schema version: "
                            + schemaVersion
            );
        }

        return PaymentState.builder()
""",
    """    PaymentState toState() {
        validateSchemaCompatibility();

        return PaymentState.builder()
""",
    "StateDocument toState validation"
)

if "private void validateSchemaCompatibility()" not in source:
    marker = """    PaymentState toState() {
"""
    helper = """    private void validateSchemaCompatibility() {
        if (schemaVersion < 1
                || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new PaymentPersistenceException(
                    "Unsupported Payment state schema version: "
                            + schemaVersion
            );
        }

        if (schemaVersion == 1
                && (initiationContext != null
                || customerConfirmationEvidence != null)) {
            throw new PaymentPersistenceException(
                    "Legacy Payment state payload must not contain "
                            + "initiation context or confirmation evidence"
            );
        }

        if (schemaVersion == CURRENT_SCHEMA_VERSION
                && status == PaymentStatus.PENDING_CONFIRMATION
                && initiationContext == null) {
            throw new PaymentPersistenceException(
                    "Payment state schema version 2 requires "
                            + "initiation context for PENDING_CONFIRMATION"
            );
        }

        if (schemaVersion == CURRENT_SCHEMA_VERSION
                && initiationContext != null
                && status != PaymentStatus.RECEIVED
                && status != PaymentStatus.PENDING_CONFIRMATION
                && customerConfirmationEvidence == null) {
            throw new PaymentPersistenceException(
                    "Payment state schema version 2 requires "
                            + "confirmation evidence after confirmation"
            );
        }
    }

"""
    if marker not in source:
        raise SystemExit("StateDocument method anchor changed")
    source = source.replace(marker, helper + marker, 1)

save(path, source)

print("PaymentState / PaymentStateDocument patch applied successfully")
