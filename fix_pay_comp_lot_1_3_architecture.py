from pathlib import Path
import subprocess


EXPECTED_HEAD = "5e020d66e6cdc4e3ae695e96ae55763fd9f67402"

ROOT = Path(__file__).resolve().parent
PAYMENT = ROOT / "backend/payment"

VERIFY_COMMAND = PAYMENT / (
    "src/main/java/com/sixpay/payment/application/command/"
    "VerifyPaymentConfirmationCommand.java"
)
CONFIRMATION_SERVICE = PAYMENT / (
    "src/main/java/com/sixpay/payment/application/service/"
    "PaymentConfirmationService.java"
)
BANKING_ARCH_TEST = PAYMENT / (
    "src/test/java/com/sixpay/payment/architecture/"
    "PaymentBankingAdaptersArchitectureTest.java"
)
FOUNDATION_ARCH_TEST = PAYMENT / (
    "src/test/java/com/sixpay/payment/architecture/"
    "PaymentFoundationArchitectureTest.java"
)


def git(*args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def replace_once(path: Path, old: str, new: str) -> None:
    require(path.is_file(), f"Missing file: {path.relative_to(ROOT)}")
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    require(
        count == 1,
        f"Expected exactly one target in {path.relative_to(ROOT)}, found {count}",
    )
    path.write_text(
        text.replace(old, new, 1),
        encoding="utf-8",
        newline="\n",
    )
    print(f"UPDATED  {path.relative_to(ROOT)}")


def replace_file(path: Path, expected_marker: str, content: str) -> None:
    require(path.is_file(), f"Missing file: {path.relative_to(ROOT)}")
    current = path.read_text(encoding="utf-8")
    require(
        expected_marker in current,
        f"Expected LOT 1.3 marker not found in {path.relative_to(ROOT)}",
    )
    path.write_text(content, encoding="utf-8", newline="\n")
    print(f"UPDATED  {path.relative_to(ROOT)}")


# ---------------------------------------------------------------------------
# Preflight
# ---------------------------------------------------------------------------

head = git("rev-parse", "HEAD")
require(
    head == EXPECTED_HEAD,
    f"Unexpected HEAD: {head}. Expected {EXPECTED_HEAD}",
)

for required in (
    VERIFY_COMMAND,
    CONFIRMATION_SERVICE,
    BANKING_ARCH_TEST,
    FOUNDATION_ARCH_TEST,
):
    require(
        required.is_file(),
        f"Required file is missing: {required.relative_to(ROOT)}",
    )

print(f"HEAD OK   {head}")
print("Correcting the already-applied LOT 1.3 patch.")
print()


# ---------------------------------------------------------------------------
# 1. Verify command must respect repository command/query architecture:
#    immutable record or enum.
#
# Keep OTP transient:
# - defensive copy in canonical constructor
# - defensive copy in accessor
# - redacted toString
# ---------------------------------------------------------------------------

replace_file(
    VERIFY_COMMAND,
    "public final class VerifyPaymentConfirmationCommand",
    """package com.sixpay.payment.application.command;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Arrays;
import java.util.Objects;

/**
 * Public verify-confirmation application command.
 *
 * <p>The OTP is transient in-memory input only. The record performs a
 * defensive copy on construction and access, and its textual representation
 * always redacts the OTP.</p>
 */
public record VerifyPaymentConfirmationCommand(
        PublicPaymentReference paymentReference,
        CorrelationId correlationId,
        IdempotencyKey idempotencyKey,
        char[] otp
) {

    public VerifyPaymentConfirmationCommand {
        paymentReference = Objects.requireNonNull(
                paymentReference,
                "Payment reference"
        );
        correlationId = Objects.requireNonNull(
                correlationId,
                "Correlation ID"
        );
        idempotencyKey = Objects.requireNonNull(
                idempotencyKey,
                "Idempotency key"
        );

        Objects.requireNonNull(otp, "OTP");
        if (otp.length == 0) {
            throw new IllegalArgumentException(
                    "OTP must not be empty"
            );
        }

        otp = Arrays.copyOf(otp, otp.length);
    }

    /**
     * Returns a defensive copy so the record never exposes its internal
     * transient OTP array.
     */
    @Override
    public char[] otp() {
        return Arrays.copyOf(otp, otp.length);
    }

    @Override
    public String toString() {
        return "VerifyPaymentConfirmationCommand[paymentReference="
                + paymentReference
                + ", correlationId="
                + correlationId
                + ", idempotencyKey=<opaque>, otp=<redacted>]";
    }
}
""",
)


# ---------------------------------------------------------------------------
# 2. PaymentConfirmationService must not become an unconditional Spring bean
#    before LOT 1.5 provides the PaymentConfirmationGateway adapter.
#
# This follows the existing PaymentCustomerVerificationService convention.
# ---------------------------------------------------------------------------

replace_once(
    CONFIRMATION_SERVICE,
    """import org.springframework.stereotype.Service;

import java.util.Objects;
""",
    """import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.Objects;
""",
)

replace_once(
    CONFIRMATION_SERVICE,
    """@Service
public class PaymentConfirmationService
""",
    """@Service
@ConditionalOnBean(PaymentConfirmationGateway.class)
public class PaymentConfirmationService
""",
)


# ---------------------------------------------------------------------------
# 3. Existing banking architecture inventory must recognize the newly approved
#    Payment Confirmation outbound boundary.
#
# We only extend the exact-file allowlist. No existing banking rule is removed.
# ---------------------------------------------------------------------------

replace_once(
    BANKING_ARCH_TEST,
    """                "LookupGateway.java",
                "PostingGateway.java",
""",
    """                "LookupGateway.java",
                "PaymentConfirmationBankResult.java",
                "PaymentConfirmationGateway.java",
                "PostingGateway.java",
""",
)


# ---------------------------------------------------------------------------
# 4. Existing focused-service inventory must recognize the approved
#    PaymentConfirmationService.
#
# Again, only the allowlist is extended; all existing size/dependency rules
# remain unchanged.
# ---------------------------------------------------------------------------

replace_once(
    FOUNDATION_ARCH_TEST,
    """                "PaymentAuthorizationService.java",
                "PaymentCustomerVerificationRequestFactory.java",
""",
    """                "PaymentAuthorizationService.java",
                "PaymentConfirmationService.java",
                "PaymentCustomerVerificationRequestFactory.java",
""",
)


# ---------------------------------------------------------------------------
# Final report
# ---------------------------------------------------------------------------

print()
print("LOT 1.3 corrective patch applied.")
print()
print("Corrected:")
print(" - VerifyPaymentConfirmationCommand is now a record")
print(" - PaymentConfirmationService is conditional on PaymentConfirmationGateway")
print(" - banking architecture allowlist includes confirmation port/result")
print(" - focused-service architecture allowlist includes confirmation service")
print()
print("Current git status:")
print(git("status", "--short"))
print()
print("No commit, push, branch change or remote modification was performed.")
