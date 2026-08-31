#!/usr/bin/env python3
from pathlib import Path
import subprocess

EXPECTED_BRANCH = "feat/repository-baseline-consolidation-cleanup"
EXPECTED_HEAD = "9dea0ebccfaa3ec0019ea21a0e543c23b12571b0"

TARGET = Path(
    "backend/payment/src/main/java/com/sixpay/payment/"
    "configuration/PaymentMvpPolicyConfiguration.java"
)

def run(*args, check=True):
    return subprocess.run(
        args,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=check,
    )

def require(condition, message):
    if not condition:
        raise RuntimeError(message)

require(Path(".git").exists(), "Exécuter depuis la racine du repository Git.")

branch = run("git", "branch", "--show-current").stdout.strip()
head = run("git", "rev-parse", "HEAD").stdout.strip()

require(branch == EXPECTED_BRANCH,
        f"Branche inattendue: {branch}. Attendue: {EXPECTED_BRANCH}")
require(head == EXPECTED_HEAD,
        f"HEAD inattendu: {head}. Attendu: {EXPECTED_HEAD}")
require(TARGET.exists(), f"Fichier absent: {TARGET}")

source = TARGET.read_text(encoding="utf-8")

imports = (
    "import com.sixpay.payment.domain.model.FailureCategory;\n"
    "import com.sixpay.payment.domain.model.RetryDisposition;\n"
)

anchor = "import com.sixpay.payment.domain.model.FinancialInstitutionCode;\n"

if imports in source:
    print("Imports déjà présents.")
else:
    require(anchor in source, "Import d'ancrage introuvable.")
    source = source.replace(anchor, anchor + imports, 1)
    TARGET.write_text(source, encoding="utf-8", newline="\n")
    print(f"UPDATED {TARGET}")

diff_check = run("git", "diff", "--check", check=False)
require(
    diff_check.returncode == 0,
    "git diff --check a échoué:\n"
    + diff_check.stdout
    + diff_check.stderr,
)

print()
print("Correctif imports PaymentMvpPolicyConfiguration appliqué.")
print("Aucun commit, push, PR ou changement de branche effectué.")
print()
print("Validation ciblée depuis backend/:")
print("mvn -pl payment -am -DskipTests compile")
print()
print("Puis:")
print(
    'mvn -pl payment -am -Dsurefire.failIfNoSpecifiedTests=false '
    '"-Dtest=PaymentFoundationArchitectureTest,'
    'PaymentPostPersistenceOrchestrationServiceTest,'
    'PaymentInternalIntegrationEventListenerTest,'
    'PaymentMvpPolicyConfigurationTest,'
    'PaymentCustomerVerificationServiceTest" test'
)
