#!/usr/bin/env python3
from pathlib import Path
import subprocess

EXPECTED_BRANCH = "feat/repository-baseline-consolidation-cleanup"
EXPECTED_HEAD = "225c97175dafc896c6e054482f8a6a819a7cf0a4"
ROOT = Path.cwd()

ADAPTER_TEST = Path("backend/bootstrap/src/test/java/com/sixpay/bootstrap/integration/customer/CustomerVerificationModuleAdapterTest.java")
INTERMODULE_TEST = Path("backend/bootstrap/src/test/java/com/sixpay/bootstrap/integration/customer/PaymentCustomerVerificationIntermoduleIntegrationTest.java")

def run(*args, check=True):
    return subprocess.run(args, cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=check)

def require(condition, message):
    if not condition:
        raise RuntimeError(message)

require((ROOT / ".git").exists(), "Exécuter depuis la racine du repository Git.")
branch = run("git", "branch", "--show-current").stdout.strip()
head = run("git", "rev-parse", "HEAD").stdout.strip()
require(branch == EXPECTED_BRANCH, f"Branche inattendue: {branch}")
require(head == EXPECTED_HEAD, f"HEAD inattendu: {head}")

remote = run("git", "ls-remote", "origin", f"refs/heads/{EXPECTED_BRANCH}").stdout.strip()
require(remote, "HEAD distant introuvable.")
remote_head = remote.split()[0]
require(remote_head == EXPECTED_HEAD, f"Le HEAD distant a changé: {remote_head}. Arrêt sans modification.")

for target in (ADAPTER_TEST, INTERMODULE_TEST):
    require((ROOT / target).is_file(), f"Fichier absent: {target}")

# CustomerVerificationModuleAdapterTest
path = ROOT / ADAPTER_TEST
source = path.read_text(encoding="utf-8")

if "VerifiedBankingIdentity" not in source:
    anchor = "import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;\n"
    addition = anchor + (
        "import com.sixpay.customer.verification.application.port.output.VerifiedBankingAccount;\n"
        "import com.sixpay.customer.verification.application.port.output.VerifiedBankingIdentity;\n"
    )
    require(anchor in source, "Ancre imports adapter test introuvable.")
    source = source.replace(anchor, addition, 1)

old_call = '''                    Instant.parse("2026-08-03T18:30:01Z"),
                    Instant.parse("2026-08-03T18:35:01Z"),
                    Instant.parse("2026-08-03T18:30:02Z")
            );
'''
new_call = '''                    Instant.parse("2026-08-03T18:30:01Z"),
                    Instant.parse("2026-08-03T18:35:01Z"),
                    Instant.parse("2026-08-03T18:30:02Z"),
                    "AMPLITUDE-CUSTOMER-001",
                    "AMPLITUDE-ACCOUNT-001",
                    verifiedIdentity(),
                    verifiedAccount()
            );
'''
if old_call in source:
    source = source.replace(old_call, new_call, 1)
elif new_call not in source:
    raise RuntimeError("Appel VerifyCustomerResult.of adapter test non reconnu.")

if "private static VerifiedBankingIdentity verifiedIdentity()" not in source:
    marker = "    private static CustomerVerificationRequest request() {\n"
    helpers = '''    private static VerifiedBankingIdentity verifiedIdentity() {
        return new VerifiedBankingIdentity(
                "AMPLITUDE-CUSTOMER-001",
                "CUSTOMER-001",
                "AMPLITUDE",
                "M0123456",
                "Ada Lovelace",
                "+237600000001",
                "ada.lovelace@example.test",
                "COMPLETE",
                java.util.List.of(),
                Instant.parse("2026-08-03T18:00:00Z"),
                Instant.parse("2026-08-03T18:30:01Z")
        );
    }

    private static VerifiedBankingAccount verifiedAccount() {
        return new VerifiedBankingAccount(
                "AMPLITUDE-ACCOUNT-001",
                "AMPLITUDE-CUSTOMER-001",
                "AMPLITUDE",
                "****************0123",
                "XAF",
                "CURRENT",
                "ACTIVE",
                java.util.List.of(),
                Instant.parse("2026-08-03T18:30:01Z")
        );
    }

'''
    require(marker in source, "Ancre helper adapter test introuvable.")
    source = source.replace(marker, helpers + marker, 1)

path.write_text(source, encoding="utf-8", newline="\n")
print(f"UPDATED {ADAPTER_TEST}")

# PaymentCustomerVerificationIntermoduleIntegrationTest
path = ROOT / INTERMODULE_TEST
source = path.read_text(encoding="utf-8")

if "VerifiedBankingIdentity" not in source:
    anchors = [
        "import com.sixpay.customer.verification.application.port.input.VerifyCustomerResult;\n",
        "import com.sixpay.customer.verification.application.port.input.VerifyCustomerCommand;\n",
    ]
    anchor = next((a for a in anchors if a in source), None)
    require(anchor is not None, "Ancre imports intermodule test introuvable.")
    addition = anchor + (
        "import com.sixpay.customer.verification.application.port.output.VerifiedBankingAccount;\n"
        "import com.sixpay.customer.verification.application.port.output.VerifiedBankingIdentity;\n"
    )
    source = source.replace(anchor, addition, 1)

old_call = '''                command.accountBindingFingerprint(),
                OBSERVED_AT,
                OBSERVED_AT.plusSeconds(300),
                COMPLETED_AT
        );
'''
new_call = '''                command.accountBindingFingerprint(),
                OBSERVED_AT,
                OBSERVED_AT.plusSeconds(300),
                COMPLETED_AT,
                outcome == VerificationOutcome.VERIFIED
                        ? "AMPLITUDE-CUSTOMER-001"
                        : null,
                outcome == VerificationOutcome.VERIFIED
                        ? "AMPLITUDE-ACCOUNT-001"
                        : null,
                outcome == VerificationOutcome.VERIFIED
                        ? verifiedIdentity()
                        : null,
                outcome == VerificationOutcome.VERIFIED
                        ? verifiedAccount()
                        : null
        );
'''
if old_call in source:
    source = source.replace(old_call, new_call, 1)
elif new_call not in source:
    raise RuntimeError("Appel VerifyCustomerResult.of intermodule test non reconnu.")

if "private static VerifiedBankingIdentity verifiedIdentity()" not in source:
    marker = "    private static CustomerVerificationRequest paymentRequest() {\n"
    helpers = '''    private static VerifiedBankingIdentity verifiedIdentity() {
        return new VerifiedBankingIdentity(
                "AMPLITUDE-CUSTOMER-001",
                "CUSTOMER-001",
                "AMPLITUDE",
                "M0123456",
                "Ada Lovelace",
                "+237600000001",
                "ada.lovelace@example.test",
                "COMPLETE",
                java.util.List.of(),
                OBSERVED_AT.minusSeconds(60),
                OBSERVED_AT
        );
    }

    private static VerifiedBankingAccount verifiedAccount() {
        return new VerifiedBankingAccount(
                "AMPLITUDE-ACCOUNT-001",
                "AMPLITUDE-CUSTOMER-001",
                "AMPLITUDE",
                "****************0123",
                "XAF",
                "CURRENT",
                "ACTIVE",
                java.util.List.of(),
                OBSERVED_AT
        );
    }

'''
    require(marker in source, "Ancre helper intermodule test introuvable.")
    source = source.replace(marker, helpers + marker, 1)

path.write_text(source, encoding="utf-8", newline="\n")
print(f"UPDATED {INTERMODULE_TEST}")

diff = run("git", "diff", "--check", check=False)
require(diff.returncode == 0, "git diff --check a échoué:\n" + diff.stdout + diff.stderr)

print()
print("Fixtures bootstrap alignées sur VerifyCustomerResult R4.")
print("Aucune logique runtime, contrat, migration ou frontière de module modifiée.")
print("Aucun commit, push, PR ou changement de branche effectué.")
print()
print("Validation ciblée depuis backend/:")
print('mvn -pl bootstrap -am -Dsurefire.failIfNoSpecifiedTests=false "-Dtest=CustomerVerificationModuleAdapterTest,PaymentCustomerVerificationIntermoduleIntegrationTest" test')
print("Puis: mvn -pl bootstrap -am verify")
print("Enfin: mvn verify")
