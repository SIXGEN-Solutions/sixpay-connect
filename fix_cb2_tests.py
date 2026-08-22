#!/usr/bin/env python3
from pathlib import Path
import subprocess
import sys

ROOT = Path.cwd()
TARGET_BRANCH = "feat/sixpay-customer-management-baseline"

def run(*args):
    return subprocess.run(args, cwd=ROOT, text=True, capture_output=True)

def require_repo():
    r = run("git", "rev-parse", "--show-toplevel")
    if r.returncode != 0:
        raise SystemExit("Run from the sixpay-connect Git repository.")
    repo_root = Path(r.stdout.strip()).resolve()
    if repo_root != ROOT.resolve():
        raise SystemExit(f"Run from repository root: {repo_root}")
    branch = run("git", "branch", "--show-current").stdout.strip()
    if branch != TARGET_BRANCH:
        raise SystemExit(
            f"Wrong branch {branch!r}; expected {TARGET_BRANCH!r}"
        )

def read(rel):
    p = ROOT / rel
    if not p.exists():
        raise SystemExit(f"Missing expected file: {rel}")
    return p.read_text(encoding="utf-8")

def write(rel, text):
    p = ROOT / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8", newline="\n")
    print(f"[write] {rel}")

def replace_once(text, old, new, label):
    if new in text:
        print(f"[skip] {label}: already applied")
        return text
    count = text.count(old)
    if count != 1:
        raise SystemExit(
            f"[stop] {label}: expected exactly one match, found {count}"
        )
    print(f"[ok]   {label}")
    return text.replace(old, new, 1)

require_repo()

# 1. PaymentBankingAdaptersArchitectureTest
rel = (
    "backend/payment/src/test/java/com/sixpay/payment/architecture/"
    "PaymentBankingAdaptersArchitectureTest.java"
)
text = read(rel)

old = '''    @Test
    void historicalAdaptersRemainConditionalOnGenericClient()
            throws IOException {

        for (String adapterName : List.of(
                "AmplitudeVerificationAdapter.java",
                "AmplitudeFundsAdapter.java",
                "AmplitudePostingAdapter.java",
                "AmplitudeLookupAdapter.java",
                "AmplitudeReversalAdapter.java"
        )) {
            assertHistoricalAdapter(
                    adapterName,
                    "AmplitudeBankingClient"
            );
        }
    }
'''

new = '''    @Test
    void accountAndFundsAdaptersUseNarrowClient()
            throws IOException {

        for (String adapterName : List.of(
                "AmplitudeVerificationAdapter.java",
                "AmplitudeFundsAdapter.java"
        )) {
            Path adapter = AMPLITUDE_ROOT.resolve(adapterName);
            String source = normalizeSource(
                    readRequiredSource(adapter)
            );

            assertTrue(
                    source.contains(
                            "@ConditionalOnBean("
                                    + "AmplitudeAccountFundsClient.class)"
                    )
            );
            assertTrue(
                    source.contains("AmplitudeAccountFundsClient")
            );
        }

        for (String removedAdapter : List.of(
                "AmplitudePostingAdapter.java",
                "AmplitudeLookupAdapter.java",
                "AmplitudeReversalAdapter.java"
        )) {
            assertFalse(
                    Files.exists(
                            AMPLITUDE_ROOT.resolve(removedAdapter)
                    ),
                    () -> removedAdapter
                            + " must be removed after CB-2 consolidation"
            );
        }

        assertFalse(
                Files.exists(
                        AMPLITUDE_ROOT.resolve(
                                "Amplitude"
                                        + "BankingClient.java"
                        )
                ),
                "Generic Amplitude banking facade must not exist"
        );
    }
'''
text = replace_once(
    text, old, new,
    "migrate banking adapter architecture expectations"
)

old_helper = '''    private static void assertHistoricalAdapter(
            String fileName,
            String expectedClient
    ) throws IOException {

        Path adapter = AMPLITUDE_ROOT.resolve(
                fileName
        );

        if (!Files.isRegularFile(adapter)) {
            return;
        }

        String source = normalizeSource(
                Files.readString(adapter)
        );

        assertTrue(
                source.contains(
                        "@ConditionalOnBean("
                                + expectedClient
                                + ".class)"
                ),
                () -> fileName
                        + " must be conditional on "
                        + expectedClient
        );

        assertTrue(
                source.contains(expectedClient)
        );
    }

'''
if old_helper in text:
    text = text.replace(old_helper, "", 1)
    print("[ok]   remove historical adapter helper")
else:
    print("[skip] historical adapter helper already absent")

write(rel, text)

# 2. PaymentCompensationArchitectureTest
rel = (
    "backend/payment/src/test/java/com/sixpay/payment/architecture/"
    "PaymentCompensationArchitectureTest.java"
)
text = read(rel)

old = '''    @Test
    void genericReversalAdapterRemainsAvailable()
            throws Exception {
        String source = Files.readString(
                AMPLITUDE.resolve(
                        "AmplitudeReversalAdapter.java"
                )
        );

        assertTrue(
                source.contains(
                        "AmplitudeBankingClient.class"
                )
        );
        assertTrue(
                source.contains(
                        "client.reversePayment(request)"
                )
        );
    }
'''

new = '''    @Test
    void genericReversalAdapterIsRemoved() {
        assertFalse(
                Files.exists(
                        AMPLITUDE.resolve(
                                "AmplitudeReversalAdapter.java"
                        )
                )
        );

        assertFalse(
                Files.exists(
                        AMPLITUDE.resolve(
                                "Amplitude"
                                        + "BankingClient.java"
                        )
                )
        );
    }
'''
text = replace_once(
    text, old, new,
    "remove generic reversal compatibility expectation"
)
write(rel, text)

# 3. PaymentPostingArchitectureTest
rel = (
    "backend/payment/src/test/java/com/sixpay/payment/architecture/"
    "PaymentPostingArchitectureTest.java"
)
text = read(rel)

old = '''    @Test
    void genericAdapterRemainsAvailableForExistingTestAndStubClients()
            throws Exception {

        String source = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/payment/"
                                + "infrastructure/banking/amplitude/"
                                + "AmplitudePostingAdapter.java"
                )
        );

        assertTrue(
                source.contains(
                        "AmplitudeBankingClient.class"
                )
        );
        assertTrue(
                source.contains(
                        "client.postPayment(request)"
                )
        );
    }
'''

new = '''    @Test
    void genericPostingAdapterIsRemoved() {

        Path genericAdapter = Path.of(
                "src/main/java/com/sixpay/payment/"
                        + "infrastructure/banking/amplitude/"
                        + "AmplitudePostingAdapter.java"
        );

        assertFalse(
                Files.exists(genericAdapter)
        );
    }
'''
text = replace_once(
    text, old, new,
    "remove generic posting compatibility expectation"
)

text = text.replace(
    '        assertFalse(\n'
    '                source.contains("AmplitudeBankingClient")\n'
    '        );\n',
    '        assertFalse(\n'
    '                source.contains(\n'
    '                        "Amplitude" + "BankingClient"\n'
    '                )\n'
    '        );\n',
    1
)
write(rel, text)

# 4. PaymentEndToEndIntegrationIT
rel = (
    "backend/payment/src/test/java/com/sixpay/payment/infrastructure/outbox/"
    "PaymentEndToEndIntegrationIT.java"
)
text = read(rel)

text = replace_once(
    text,
    "import com.sixpay.payment.infrastructure.banking.amplitude.AmplitudeBankingClient;",
    "import com.sixpay.payment.infrastructure.banking.amplitude.posting.AmplitudePostingClient;",
    "E2E posting client import",
)
text = replace_once(
    text,
    "    private AmplitudeBankingClient amplitudeClient;",
    "    private AmplitudePostingClient amplitudeClient;",
    "E2E posting client field",
)
text = replace_once(
    text,
    "        when(amplitudeClient.postPayment(postingRequest))",
    "        when(amplitudeClient.post(postingRequest))",
    "E2E posting stub invocation",
)
text = replace_once(
    text,
    "                .postPayment(postingRequest);",
    "                .post(postingRequest);",
    "E2E posting verification invocation",
)
text = replace_once(
    text,
    '''        AmplitudeBankingClient amplitudeBankingClient() {
            return Mockito.mock(
                    AmplitudeBankingClient.class
            );
        }
''',
    '''        AmplitudePostingClient amplitudePostingClient() {
            return Mockito.mock(
                    AmplitudePostingClient.class
            );
        }
''',
    "E2E posting mock bean",
)
write(rel, text)

# 5. Fix CB-2 guard test if already generated
rel = (
    "backend/payment/src/test/java/com/sixpay/payment/architecture/"
    "CoreBankingClientConsolidationTest.java"
)
p = ROOT / rel
if p.exists():
    text = p.read_text(encoding="utf-8")
    text = text.replace(
        '"AmplitudeBankingClient.class"',
        '"Amplitude" + "BankingClient.class"',
    )
    write(rel, text)

# 6. Mechanical migration for remaining posting-only test doubles
test_root = ROOT / "backend/payment/src/test/java"
for p in test_root.rglob("*.java"):
    text = p.read_text(encoding="utf-8")
    literal = "AmplitudeBankingClient"
    if literal not in text:
        continue

    if (
        "postPayment(" in text
        and "verifyCustomerAndAccount(" not in text
        and "checkPaymentExecution(" not in text
        and "findPostingByIdempotencyKey(" not in text
        and "findPostingByBankReference(" not in text
        and "reversePayment(" not in text
    ):
        updated = text
        updated = updated.replace(
            "import com.sixpay.payment.infrastructure.banking.amplitude."
            "AmplitudeBankingClient;",
            "import com.sixpay.payment.infrastructure.banking.amplitude.posting."
            "AmplitudePostingClient;",
        )
        updated = updated.replace(
            "AmplitudeBankingClient", "AmplitudePostingClient"
        )
        updated = updated.replace(".postPayment(", ".post(")
        if updated != text:
            p.write_text(updated, encoding="utf-8", newline="\n")
            print(
                "[auto] posting-only test double migrated: "
                + str(p.relative_to(ROOT))
            )

# 7. Final residual scan
needle = "AmplitudeBankingClient"
violations = []
for p in (ROOT / "backend").rglob("*"):
    if not p.is_file():
        continue
    if p.suffix not in {".java", ".kt", ".xml", ".properties", ".yaml", ".yml"}:
        continue
    try:
        data = p.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        continue
    if needle in data:
        violations.append(str(p.relative_to(ROOT)))

print("\nCB-2 test migration applied.")

if violations:
    print("\n[attention] Literal residual references remain:")
    for item in violations:
        print("  " + item)
    print(
        "\nThese were not changed automatically because their intent must be "
        "reviewed rather than guessed."
    )
    sys.exit(2)

print("\n[ok] No literal AmplitudeBankingClient references remain under backend.")
print("\nRun:")
print('  git grep -n "AmplitudeBankingClient" -- backend')
print("  ./mvnw -pl payment -am test")
print("  ./mvnw -pl customer,payment -am test")
print("  git diff --check")
