from pathlib import Path
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
GATE = ROOT / "frontend/scripts/verify-contract-consolidation.mjs"

EXPECTED_BRANCH = "feat/repository-baseline-consolidation"


def fail(message: str) -> None:
    print(f"ERROR: {message}")
    sys.exit(1)


def require(path: Path) -> str:
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")


def main() -> None:
    engineering = require(ENGINEERING)

    if EXPECTED_BRANCH not in engineering:
        fail(
            f"ENGINEERING_CONTEXT.md does not declare {EXPECTED_BRANCH}."
        )

    source = require(GATE)

    old_name_use = "for (const oldName of oldContractNames) {"

    if old_name_use not in source:
        print(
            "No residual oldContractNames scan found; "
            "the validator is already corrected."
        )
        return

    usage_index = source.index(old_name_use)

    # The obsolete whole-repository scan is the enclosing
    # `for (const file of walk(repoRoot)) { ... }` immediately preceding
    # the oldContractNames usage.
    loop_start_token = "for (const file of walk(repoRoot)) {"
    loop_start = source.rfind(
        loop_start_token,
        0,
        usage_index,
    )

    if loop_start < 0:
        fail(
            "Could not locate enclosing whole-repository stale-reference loop."
        )

    # It is the final validation block before failures are reported.
    failures_token = "if (failures.length > 0) {"
    failures_start = source.find(
        failures_token,
        usage_index,
    )

    if failures_start < 0:
        fail(
            "Could not locate final failure-report block after stale scan."
        )

    obsolete_block = source[loop_start:failures_start]

    # Safety: never delete a block unless it is clearly the retired
    # filename scanner.
    required_obsolete_tokens = [
        "walk(repoRoot)",
        "oldContractNames",
        "stale reference to removed contract",
    ]

    for token in required_obsolete_tokens:
        if token not in obsolete_block:
            fail(
                "Safety check failed: candidate block is not the expected "
                f"legacy scanner (missing {token!r})."
            )

    updated = (
        source[:loop_start].rstrip()
        + "\n\n"
        + source[failures_start:]
    )

    # Remove now-dead `textualExtensions` declaration when it exists and is
    # no longer referenced elsewhere.
    declaration_start_token = "const textualExtensions = new Set(["
    declaration_start = updated.find(declaration_start_token)

    if declaration_start >= 0:
        after_decl = updated.find("]);", declaration_start)

        if after_decl >= 0:
            after_decl += len("]);")

            remaining_without_decl = (
                updated[:declaration_start]
                + updated[after_decl:]
            )

            if "textualExtensions" not in remaining_without_decl:
                # Extend deletion through following whitespace.
                declaration_end = after_decl
                while (
                    declaration_end < len(updated)
                    and updated[declaration_end] in "\r\n "
                ):
                    declaration_end += 1

                updated = (
                    updated[:declaration_start]
                    + updated[declaration_end:]
                )

    # Hard invariants after cleanup.
    forbidden_tokens = [
        "oldContractNames",
        "stale reference to removed contract",
    ]

    for token in forbidden_tokens:
        if token in updated:
            fail(
                f"Residual legacy scanner token still present: {token}"
            )

    durable_tokens = [
        "isForbiddenHistoricalContractArtifact",
        "registeredPhysicalPaths",
        "isCanonicalPhysicalContract",
        "contractsByPhysicalPath",
    ]

    for token in durable_tokens:
        if token not in updated:
            fail(
                "Safety check failed: durable FS-2.2 validation disappeared: "
                + token
            )

    GATE.write_text(updated, encoding="utf-8")

    print("FS-2.2.5 residual stale-reference scanner removed.")
    print()
    print("Preserved:")
    print(" - historical artifact pattern gate")
    print(" - registry <-> filesystem integrity")
    print(" - registry semantic normalization")
    print(" - consolidated-contract cardinality")
    print()
    print("Next:")
    print("  cd frontend")
    print("  npm run verify:contract-consolidation")


if __name__ == "__main__":
    main()
