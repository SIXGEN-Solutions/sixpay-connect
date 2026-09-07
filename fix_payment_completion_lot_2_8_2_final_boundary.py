#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path.cwd()
BASE_SHA = "464ebda2f569197aab5694ec4dde84eb6044934e"

PORT = ROOT / (
    "backend/payment/src/main/java/com/sixpay/payment/application/port/output/banking/"
    "PaymentEventExecutionPort.java"
)
ADAPTER = ROOT / (
    "backend/payment/src/main/java/com/sixpay/payment/infrastructure/banking/"
    "amplitude/posting/AmplitudePaymentEventAdapter.java"
)
ORCH = ROOT / (
    "backend/payment/src/main/java/com/sixpay/payment/application/service/"
    "PaymentEventLifecycleOrchestrationService.java"
)

def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)

def git(*args: str, check: bool = True):
    return subprocess.run(
        ["git", *args],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=check,
    )

require((ROOT / ".git").exists(), "Run from repository root.")
head = git("rev-parse", "HEAD").stdout.strip()
require(
    git("merge-base", "--is-ancestor", BASE_SHA, "HEAD", check=False).returncode == 0,
    f"Local HEAD {head} is not based on requested SHA {BASE_SHA}",
)

for path in (PORT, ADAPTER, ORCH):
    require(path.is_file(), f"Missing file: {path.relative_to(ROOT)}")

# 1. Add provider-neutral application exception as a nested type of the port.
port = PORT.read_text(encoding="utf-8")
if "final class PaymentEventOutcomeUnknownException" not in port:
    anchor = "\n}\n"
    addition = '''
    final class PaymentEventOutcomeUnknownException
            extends RuntimeException {

        public PaymentEventOutcomeUnknownException(
                String message,
                Throwable cause
        ) {
            super(message, cause);
        }

        public PaymentEventOutcomeUnknownException(
                String message
        ) {
            super(message);
        }
    }
'''
    require(port.endswith(anchor), "Unexpected PaymentEventExecutionPort ending.")
    port = port[:-2] + addition + "}\n"
PORT.write_text(port, encoding="utf-8", newline="\n")

# 2. Infrastructure adapter translates provider-specific exception.
adapter = ADAPTER.read_text(encoding="utf-8")

infra_import = (
    "import com.sixpay.payment.infrastructure.banking.amplitude.posting.error."
    "PostingOutcomeUnknownException;\n"
)
if infra_import not in adapter:
    import_anchor = (
        "import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto."
        "AmplitudePaymentEventResult;\n"
    )
    require(import_anchor in adapter, "Adapter import anchor not found.")
    adapter = adapter.replace(
        import_anchor,
        import_anchor + infra_import,
        1,
    )

old_call = '''        AmplitudePaymentEventResult providerResult =
                client.execute(
                        mapper.toRequest(
                                command.snapshot(),
                                new AmplitudePaymentEventMappingContext(
                                        context.operationCode(),
                                        context.eventNumber(),
                                        context.accountingDate(),
                                        context.nightMode(),
                                        PAYMENT_NATURE,
                                        context.technicalUser(),
                                        context.requestedAt()
                                )
                        ),
                        command.context().correlationId().value(),
                        command.context().financialInstitutionCode().value(),
                        command.idempotencyKey().value()
                );

        return toApplicationResult(providerResult);
'''

new_call = '''        try {
            AmplitudePaymentEventResult providerResult =
                    client.execute(
                            mapper.toRequest(
                                    command.snapshot(),
                                    new AmplitudePaymentEventMappingContext(
                                            context.operationCode(),
                                            context.eventNumber(),
                                            context.accountingDate(),
                                            context.nightMode(),
                                            PAYMENT_NATURE,
                                            context.technicalUser(),
                                            context.requestedAt()
                                    )
                            ),
                            command.context().correlationId().value(),
                            command.context().financialInstitutionCode().value(),
                            command.idempotencyKey().value()
                    );

            return toApplicationResult(providerResult);
        } catch (PostingOutcomeUnknownException exception) {
            throw new PaymentEventOutcomeUnknownException(
                    exception.getMessage(),
                    exception
            );
        }
'''

require(
    old_call in adapter or new_call in adapter,
    "Expected Amplitude execution block not found."
)
adapter = adapter.replace(old_call, new_call, 1)
ADAPTER.write_text(adapter, encoding="utf-8", newline="\n")

# 3. Application orchestrator no longer imports infrastructure exception.
orch = ORCH.read_text(encoding="utf-8")
orch = orch.replace(
    "import com.sixpay.payment.infrastructure.banking.amplitude.posting.error."
    "PostingOutcomeUnknownException;\n",
    ""
)
orch = orch.replace(
    "} catch (PostingOutcomeUnknownException unknown) {",
    "} catch (PaymentEventExecutionPort.PaymentEventOutcomeUnknownException unknown) {"
)
ORCH.write_text(orch, encoding="utf-8", newline="\n")

print("Applied final application/infrastructure boundary fix.")
print("Base SHA:", BASE_SHA)
print("Observed local HEAD:", head)
print("Modified:")
for path in (PORT, ADAPTER, ORCH):
    print(" -", path.relative_to(ROOT))
print("")
print("Run:")
print(" cd backend")
print(" mvn -pl payment -am -DskipTests compile")
print(
    " mvn -pl payment -am "
    "-Dtest=PaymentApplicationLayerArchitectureTest,"
    "PaymentFinalArchitectureGateTest,"
    "PaymentGoldenModuleStructureTest "
    "-Dsurefire.failIfNoSpecifiedTests=false test"
)
print(" mvn -pl payment -am test")
