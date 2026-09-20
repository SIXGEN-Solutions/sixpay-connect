from pathlib import Path
import subprocess

EXPECTED_BRANCH = "feat/baseline-overall-payment"
EXPECTED_HEAD = "4627c8ba430e152b34697255fab8009347ae2d91"

def run(*args):
    return subprocess.check_output(args, text=True).strip()

def require(cond, msg):
    if not cond:
        raise RuntimeError(msg)

root = Path.cwd()
branch = run("git", "branch", "--show-current")
head = run("git", "rev-parse", "HEAD")

require(branch == EXPECTED_BRANCH, f"Unexpected branch: {branch}. Expected {EXPECTED_BRANCH}")
require(head == EXPECTED_HEAD, f"Unexpected HEAD: {head}. Expected {EXPECTED_HEAD}")

status = run("git", "status", "--short")
if status:
    print("INFO: existing working-tree changes detected:")
    print(status)

payment_readme = root / "backend/payment/README.md"
architecture_test = root / "backend/payment/src/test/java/com/sixpay/payment/architecture/PaymentPartnerContractEvolutionArchitectureTest.java"

readme_text = payment_readme.read_text(encoding="utf-8")
section = """## External partner contract evolution

SIXPAY does not expose a generic external Partner Payment contract solely to
anticipate future integrations.

Each concrete external Partner integration may keep its own approved wire
contract and anti-corruption boundary while mapping to the provider-neutral
Payment application API.

A shared SIXPAY Partner Payment contract may be introduced only after at least
two concrete Partner integrations demonstrate a stable common external-contract
need. That decision requires explicit architecture and contract approval and
must not be inferred from internal Payment abstractions.

Until such evidence exists, TRESOR PAY remains a concrete external integration,
not the template for a universal Partner protocol.
"""

if "## External partner contract evolution" not in readme_text:
    payment_readme.write_text(readme_text.rstrip() + "\n\n" + section + "\n", encoding="utf-8", newline="\n")
    print(f"UPDATED {payment_readme}")
else:
    print(f"UNCHANGED {payment_readme}")

test_content = """package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPartnerContractEvolutionArchitectureTest {

    private static final Path PAYMENT_API =
            Path.of("src/main/java/com/sixpay/payment/api");
    private static final Path CONTRACTS =
            Path.of("../../documentation/contracts");

    @Test
    void paymentDoesNotPreemptivelyExposeGenericPartnerWireTypes() throws Exception {
        List<String> forbiddenTypeNames = List.of(
                "PartnerPaymentRequest.java",
                "PartnerPaymentResponse.java",
                "GenericPartnerPaymentRequest.java",
                "GenericPartnerPaymentResponse.java",
                "StandardPartnerPaymentRequest.java",
                "StandardPartnerPaymentResponse.java"
        );

        try (Stream<Path> paths = Files.walk(PAYMENT_API)) {
            List<String> observed = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(forbiddenTypeNames::contains)
                    .toList();

            assertThat(observed)
                    .as("generic external Partner wire types require a demonstrated second Partner and explicit approval")
                    .isEmpty();
        }
    }

    @Test
    void noGenericPartnerPaymentContractExistsWithoutExplicitGovernanceDecision()
            throws Exception {
        if (!Files.exists(CONTRACTS)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(CONTRACTS)) {
            List<Path> genericContracts = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.contains("partner")
                                && name.contains("payment")
                                && !path.toString().replace('\\\\', '/').contains("/tresorpay/");
                    })
                    .toList();

            assertThat(genericContracts)
                    .as("a shared external Partner Payment contract must be an explicit future governance decision")
                    .isEmpty();
        }
    }
}
"""
architecture_test.parent.mkdir(parents=True, exist_ok=True)
architecture_test.write_text(test_content, encoding="utf-8", newline="\n")
print(f"UPDATED {architecture_test}")

for path in [
    root / "documentation/contracts/partner",
    root / "backend/payment/src/main/java/com/sixpay/payment/api/partner/common",
    root / "backend/payment/src/main/java/com/sixpay/payment/api/partner/generic",
]:
    require(not path.exists(), f"Premature generic partner surface detected: {path}")

print("\nP8 guard applied.")
print("No external API, contract registry entry, OpenAPI contract, schema, migration, commit or push was created.")
print("\nFocused validation:")
print("  cd backend")
print("  mvn -pl payment -am -Dtest=PaymentPartnerContractEvolutionArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test")
print("\nBroader validation:")
print("  mvn -pl integration,partner,payment -am test")
print("  cd ..")
print("  py scripts/verify_documentation_final.py")
print("  git diff --check")
print("  git status --short")
