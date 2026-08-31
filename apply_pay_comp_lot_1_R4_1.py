#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

EXPECTED_BRANCH = "feat/repository-baseline-consolidation-cleanup"
EXPECTED_HEAD = "846cf6a7d7e7f3bca3701df423e9e61974442682"

DOCUMENT = Path(
    "backend/payment/src/main/java/com/sixpay/payment/"
    "infrastructure/persistence/PaymentStateDocument.java"
)
ARCH_TEST = Path(
    "backend/payment/src/test/java/com/sixpay/payment/"
    "infrastructure/persistence/PaymentStateDocumentSchemaArchitectureTest.java"
)


def run(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        args,
        check=check,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def git_text(*args: str) -> str:
    return run("git", *args).stdout.strip()


def require_target_clean(path: Path) -> None:
    unstaged = run("git", "diff", "--quiet", "--", str(path), check=False)
    staged = run("git", "diff", "--cached", "--quiet", "--", str(path), check=False)
    require(
        unstaged.returncode == 0 and staged.returncode == 0,
        f"Le fichier ciblé contient déjà des changements locaux: {path}. "
        "Le script refuse de les écraser."
    )


require(Path(".git").exists(), "Exécuter ce script depuis la racine du repository Git.")

branch = git_text("branch", "--show-current")
head = git_text("rev-parse", "HEAD")

require(
    branch == EXPECTED_BRANCH,
    f"Branche inattendue: {branch!r}. Attendue: {EXPECTED_BRANCH!r}"
)
require(
    head == EXPECTED_HEAD,
    f"HEAD inattendu: {head}. Attendu: {EXPECTED_HEAD}"
)

for target in (DOCUMENT, ARCH_TEST):
    require(target.exists(), f"Fichier attendu absent: {target}")
    require_target_clean(target)

document = DOCUMENT.read_text(encoding="utf-8")

require(
    "static final int CURRENT_SCHEMA_VERSION = 4;" in document,
    "Le document n'est pas au schema v4 attendu."
)

old_guard = '''        if (schemaVersion >= 2
                && initiationContext != null
                && status != PaymentStatus.RECEIVED
                && status != PaymentStatus.PENDING_CONFIRMATION
                && customerConfirmationEvidence == null) {
            throw new PaymentPersistenceException(
                    "Payment state schema version "
                            + schemaVersion
                            + " requires confirmation evidence after "
                            + "confirmation"
            );
        }
    }

    PaymentState toState() {
'''

new_guard = '''        /*
         * Schema v2 predates ConfirmationChallenge. Preserve its historical
         * representation exactly: any state beyond RECEIVED or
         * PENDING_CONFIRMATION requires CustomerConfirmationEvidence.
         */
        if (schemaVersion == 2
                && initiationContext != null
                && status != PaymentStatus.RECEIVED
                && status != PaymentStatus.PENDING_CONFIRMATION
                && customerConfirmationEvidence == null) {
            throw new PaymentPersistenceException(
                    "Payment state schema version 2 requires confirmation "
                            + "evidence after confirmation"
            );
        }

        /*
         * Starting with schema v3, ConfirmationChallenge is persisted and a
         * verified challenge is a valid replacement for the legacy
         * CustomerConfirmationEvidence. Banking verification may legitimately
         * be pending before customer confirmation.
         *
         * REJECTED and FAILED are intentionally excluded because both can be
         * reached before successful OTP confirmation.
         */
        if (schemaVersion >= 3
                && initiationContext != null
                && requiresVerifiedConfirmation(status)
                && customerConfirmationEvidence == null
                && !hasVerifiedConfirmationChallenge()) {
            throw new PaymentPersistenceException(
                    "Payment state schema version "
                            + schemaVersion
                            + " requires verified confirmation after "
                            + "confirmation"
            );
        }
    }

    private static boolean requiresVerifiedConfirmation(
            PaymentStatus status
    ) {
        return switch (status) {
            case AUTHORIZATION_CHECKING,
                    FUNDS_CONTROL_PENDING,
                    TREASURY_ACCOUNT_RESOLUTION_PENDING,
                    APPROVED_FOR_POSTING,
                    POSTING_PENDING,
                    POSTING_OUTCOME_UNKNOWN,
                    DEBIT_CONFIRMED,
                    POSTED_PENDING_TFJ,
                    REVERSAL_REQUIRED,
                    REVERSAL_PENDING,
                    REVERSAL_OUTCOME_UNKNOWN,
                    TREASURY_INTEGRATED,
                    REVERSED -> true;
            case RECEIVED,
                    BANKING_VERIFICATION_PENDING,
                    PENDING_CONFIRMATION,
                    REJECTED,
                    FAILED -> false;
        };
    }

    private boolean hasVerifiedConfirmationChallenge() {
        return confirmationChallenge != null
                && confirmationChallenge.status()
                        == ConfirmationChallengeStatus.VERIFIED
                && confirmationChallenge.verifiedAt() != null;
    }

    PaymentState toState() {
'''

require(
    old_guard in document,
    "Le garde de compatibilité attendu n'a pas été trouvé. "
    "Le fichier a peut-être déjà évolué."
)

document = document.replace(old_guard, new_guard, 1)
DOCUMENT.write_text(document, encoding="utf-8", newline="\n")

arch = ARCH_TEST.read_text(encoding="utf-8")

old_block = '''        /*
         * Validation shared by schemas v2 and v3 must remain present
         * without hard-coding a version-specific error message.
         */
        assertTrue(source.contains(
                "schemaVersion >= 2"
        ));

        assertTrue(source.contains(
                "requires initiation context for"
        ));

        assertTrue(source.contains(
                "requires confirmation evidence after"
        ));

        assertTrue(source.contains(
                "PaymentInitiationContext initiationContext"
        ));

        assertTrue(source.contains(
                "CustomerConfirmationEvidence customerConfirmationEvidence"
        ));
'''

new_block = '''        /*
         * Schema v2 preserves the legacy confirmation representation.
         */
        assertTrue(source.contains(
                "schemaVersion == 2"
        ));

        assertTrue(source.contains(
                "requires initiation context for"
        ));

        assertTrue(source.contains(
                "Payment state schema version 2 requires confirmation"
        ));

        /*
         * Starting with schema v3, a VERIFIED ConfirmationChallenge is a
         * valid post-confirmation proof and BANKING_VERIFICATION_PENDING
         * remains a legitimate pre-confirmation state.
         */
        assertTrue(source.contains(
                "schemaVersion >= 3"
        ));

        assertTrue(source.contains(
                "requiresVerifiedConfirmation(status)"
        ));

        assertTrue(source.contains(
                "BANKING_VERIFICATION_PENDING"
        ));

        assertTrue(source.contains(
                "ConfirmationChallengeStatus.VERIFIED"
        ));

        assertTrue(source.contains(
                "confirmationChallenge.verifiedAt() != null"
        ));

        assertTrue(source.contains(
                "PaymentInitiationContext initiationContext"
        ));

        assertTrue(source.contains(
                "CustomerConfirmationEvidence customerConfirmationEvidence"
        ));
'''

require(
    old_block in arch,
    "Le bloc de test d'architecture attendu n'a pas été trouvé."
)

arch = arch.replace(old_block, new_block, 1)
ARCH_TEST.write_text(arch, encoding="utf-8", newline="\n")

diff_check = run("git", "diff", "--check", check=False)
require(
    diff_check.returncode == 0,
    "git diff --check a échoué:\n" + diff_check.stdout + diff_check.stderr
)

print("LOT 1.R4.1 — correctif appliqué localement.")
print()
print("Fichiers modifiés:")
print(f"  - {DOCUMENT}")
print(f"  - {ARCH_TEST}")
print()
print("Aucun commit, push, PR ou changement de branche n'a été effectué.")
print()
print("Validation ciblée recommandée depuis backend/:")
print(
    '  mvn -pl payment -am -Dsurefire.failIfNoSpecifiedTests=false '
    '"-Dtest=PaymentStateDocumentV4Test,'
    'PaymentStateDocumentSchemaArchitectureTest" test'
)
print()
print("Puis:")
print("  mvn -pl payment -am verify")
print()
print("Et enfin, si le gate Payment est vert:")
print("  mvn verify")
