#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(sys.argv[1]).resolve()

def load(relative):
    path = root / relative
    if not path.is_file():
        raise SystemExit(f"Missing file: {relative}")
    return path, path.read_text(encoding="utf-8")

def save(path, content):
    path.write_text(content, encoding="utf-8", newline="\n")

# 1. Keep the production reception path mandatory while preserving existing
# aggregate callers and fixtures that start authorization from RECEIVED.
path, source = load(
    "backend/payment/src/main/java/com/sixpay/payment/domain/model/Payment.java"
)

current = '''    public void startAuthorizationChecking(Instant startedAt) {
        recordCustomerConfirmation(startedAt);
    }
'''

replacement = '''    public void startAuthorizationChecking(Instant startedAt) {
        Objects.requireNonNull(startedAt, "Started instant");

        if (state.status() == PaymentStatus.AUTHORIZATION_CHECKING) {
            return;
        }
        if (state.status() == PaymentStatus.PENDING_CONFIRMATION) {
            recordCustomerConfirmation(startedAt);
            return;
        }

        /*
         * Backward-compatible domain entry for existing internal workflows and
         * test fixtures. New externally received payments are persisted by
         * PaymentReceptionService in PENDING_CONFIRMATION, so the TresorPay
         * command path cannot bypass customer confirmation.
         */
        requireStatus(
                "startAuthorizationChecking",
                PaymentStatus.RECEIVED
        );

        PaymentState next = nextBuilder(
                PaymentStatus.AUTHORIZATION_CHECKING,
                startedAt
        ).failure(null).build();

        EventBatch batch = new EventBatch(next, startedAt);
        commit(
                next,
                List.of(
                        new PaymentAuthorizationCheckingStarted(
                                batch.metadata(),
                                startedAt
                        )
                )
        );
    }
'''

if current in source:
    source = source.replace(current, replacement, 1)
elif replacement not in source:
    raise SystemExit(
        "Unexpected Payment.startAuthorizationChecking implementation"
    )
save(path, source)

# 2. Update exact closed-status catalogue.
path, source = load(
    "backend/payment/src/test/java/com/sixpay/payment/domain/model/"
    "PaymentClassificationTest.java"
)
source = source.replace(
    "void paymentStatusContainsExactlyTheSeventeenIa1Values()",
    "void paymentStatusContainsExactlyTheEighteenValues()"
)
if "PaymentStatus.PENDING_CONFIRMATION" not in source:
    expected = '''                        PaymentStatus.RECEIVED,
                        PaymentStatus.AUTHORIZATION_CHECKING,
'''
    replacement_status = '''                        PaymentStatus.RECEIVED,
                        PaymentStatus.PENDING_CONFIRMATION,
                        PaymentStatus.AUTHORIZATION_CHECKING,
'''
    if expected not in source:
        raise SystemExit("PaymentClassificationTest status list changed")
    source = source.replace(expected, replacement_status, 1)
save(path, source)

# 3. Update kernel state count and test naming.
path, source = load(
    "backend/payment/src/test/java/com/sixpay/payment/domain/validation/"
    "PaymentDomainKernelCatalogueTest.java"
)
source = source.replace(
    "void kernelContainsSeventeenStatesAndFourTerminalStates()",
    "void kernelContainsEighteenStatesAndFourTerminalStates()"
)
source = source.replace(
    "assertEquals(17, PaymentStatus.values().length);",
    "assertEquals(18, PaymentStatus.values().length);"
)
save(path, source)

# 4. Use a real Money value in the new lifecycle test.
path, source = load(
    "backend/payment/src/test/java/com/sixpay/payment/domain/model/"
    "PaymentPendingConfirmationLifecycleTest.java"
)

if "import java.math.BigDecimal;" not in source:
    source = source.replace(
        "import java.time.Instant;\n",
        "import java.math.BigDecimal;\nimport java.time.Instant;\n",
        1
    )

source = source.replace(
    "        Money amount = Mockito.mock(Money.class);\n",
    '''        Money amount = Money.of(
                new BigDecimal("600000"),
                "XAF"
        );
'''
)
source = source.replace(
    "        when(amount.isPositive()).thenReturn(true);\n",
    ""
)
save(path, source)

print("PENDING_CONFIRMATION test correction applied")
