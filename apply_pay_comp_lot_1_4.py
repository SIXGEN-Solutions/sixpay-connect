#!/usr/bin/env python3
from pathlib import Path
import subprocess

ROOT = Path.cwd()
EXPECTED_BRANCH = "feat/repository-baseline-consolidation-cleanup"
REFERENCE_SHA = "3802cef33e676a810e0bb12676ba84fe6e2a74b5"
SELF = Path(__file__).name

def run(*args, check=True):
    return subprocess.run(
        args, cwd=ROOT, check=check, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.PIPE
    )

def preflight():
    if not (ROOT / ".git").exists():
        raise RuntimeError("Run this script from the repository root.")
    branch = run("git", "branch", "--show-current").stdout.strip()
    if branch != EXPECTED_BRANCH:
        raise RuntimeError(f"Wrong branch: {branch!r}; expected {EXPECTED_BRANCH!r}")
    if run("git", "merge-base", "--is-ancestor", REFERENCE_SHA, "HEAD", check=False).returncode != 0:
        raise RuntimeError(f"{REFERENCE_SHA} is not an ancestor of HEAD")
    dirty = []
    for line in run("git", "status", "--porcelain").stdout.splitlines():
        path = line[3:] if len(line) > 3 else ""
        if path != SELF:
            dirty.append(line)
    if dirty:
        raise RuntimeError("Working tree is not clean before LOT 1.4:\n" + "\n".join(dirty))

def write_new(path, content):
    target = ROOT / path
    if target.exists():
        raise RuntimeError(f"Refusing to overwrite existing file: {path}")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8", newline="\n")
    print("CREATE ", path)

def replace_file(path, marker, content):
    target = ROOT / path
    if not target.exists():
        raise RuntimeError(f"Expected existing file missing: {path}")
    current = target.read_text(encoding="utf-8")
    if marker not in current:
        raise RuntimeError(f"Baseline marker not found in {path}")
    target.write_text(content, encoding="utf-8", newline="\n")
    print("UPDATE ", path)

preflight()

write_new(
"backend/payment/src/main/java/com/sixpay/payment/application/port/output/idempotency/PaymentConfirmationIdempotencyPort.java",
r'''package com.sixpay.payment.application.port.output.idempotency;

import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.function.Supplier;

public interface PaymentConfirmationIdempotencyPort {

    PaymentConfirmationBankResult executeCreate(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            IdempotencyKey idempotencyKey,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    );

    PaymentConfirmationBankResult executeVerify(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference,
            IdempotencyKey idempotencyKey,
            char[] otp,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    );

    PaymentConfirmationBankResult executeReplace(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference,
            IdempotencyKey idempotencyKey,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    );
}
''')

write_new(
"backend/payment/src/main/java/com/sixpay/payment/application/port/output/idempotency/PaymentOtpHmacKeyProvider.java",
r'''package com.sixpay.payment.application.port.output.idempotency;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Runtime boundary for dedicated OTP idempotency HMAC keys.
 * First key = current write key; remaining keys = previous replay keys.
 */
public interface PaymentOtpHmacKeyProvider {

    List<SecretKey> verificationKeys();
}
''')

write_new(
"backend/payment/src/main/java/com/sixpay/payment/application/port/output/banking/PaymentConfirmationOutcomeUnknownException.java",
r'''package com.sixpay.payment.application.port.output.banking;

/**
 * External confirmation command may have been applied, but its result
 * was not received. Caller must perform authoritative recovery and never
 * blindly retry the original command.
 */
public final class PaymentConfirmationOutcomeUnknownException extends RuntimeException {

    private final String recoveryReference;

    public PaymentConfirmationOutcomeUnknownException(String message) {
        this(message, null, null);
    }

    public PaymentConfirmationOutcomeUnknownException(
            String message,
            String recoveryReference,
            Throwable cause
    ) {
        super(normalizeMessage(message), cause);
        this.recoveryReference = normalizeReference(recoveryReference);
    }

    public String recoveryReference() {
        return recoveryReference;
    }

    private static String normalizeMessage(String value) {
        if (value == null || value.isBlank()) {
            return "Payment confirmation external outcome is unknown";
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private static String normalizeReference(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= 150
                ? normalized
                : normalized.substring(0, 150);
    }
}
''')

write_new(
"backend/payment/src/main/java/com/sixpay/payment/application/service/PaymentConfirmationInProgressException.java",
r'''package com.sixpay.payment.application.service;

public final class PaymentConfirmationInProgressException extends RuntimeException {

    public PaymentConfirmationInProgressException() {
        super("Payment confirmation request is already in progress");
    }
}
''')

write_new(
"backend/payment/src/main/java/com/sixpay/payment/infrastructure/idempotency/PaymentConfirmationCanonicalizer.java",
r'''package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class PaymentConfirmationCanonicalizer {

    private static final String VERSION = "v1";

    public String create(
            PaymentId paymentId,
            PublicPaymentReference paymentReference
    ) {
        return String.join(
                "|",
                VERSION,
                "CREATE",
                Objects.requireNonNull(paymentId, "Payment ID").toString(),
                Objects.requireNonNull(paymentReference, "Payment reference").value()
        );
    }

    public String replace(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference
    ) {
        return String.join(
                "|",
                VERSION,
                "REPLACE",
                Objects.requireNonNull(paymentId, "Payment ID").toString(),
                Objects.requireNonNull(paymentReference, "Payment reference").value(),
                Objects.requireNonNull(
                        challengeReference,
                        "Confirmation challenge reference"
                ).value()
        );
    }
}
''')

write_new(
"backend/payment/src/main/java/com/sixpay/payment/infrastructure/idempotency/PaymentOtpIdempotencyFingerprint.java",
r'''package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Component
public final class PaymentOtpIdempotencyFingerprint {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final byte[] DOMAIN_SEPARATOR =
            "SIXPAY_PAYMENT_CONFIRMATION_VERIFY_V1"
                    .getBytes(StandardCharsets.UTF_8);
    private static final byte FIELD_SEPARATOR = 0;

    public List<String> fingerprints(
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference,
            char[] otp,
            List<SecretKey> verificationKeys
    ) {
        Objects.requireNonNull(paymentReference, "Payment reference");
        Objects.requireNonNull(challengeReference, "Challenge reference");
        Objects.requireNonNull(otp, "OTP");
        if (otp.length == 0) {
            throw new IllegalArgumentException("OTP must not be empty");
        }
        if (verificationKeys == null || verificationKeys.isEmpty()) {
            throw new IllegalStateException(
                    "At least one OTP HMAC verification key is required"
            );
        }

        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (SecretKey key : verificationKeys) {
            result.add(
                    fingerprint(
                            paymentReference,
                            challengeReference,
                            otp,
                            Objects.requireNonNull(key, "OTP HMAC key")
                    )
            );
        }
        return List.copyOf(result);
    }

    private String fingerprint(
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference,
            char[] otp,
            SecretKey key
    ) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(key);
            mac.update(DOMAIN_SEPARATOR);
            mac.update(FIELD_SEPARATOR);
            mac.update(paymentReference.value().getBytes(StandardCharsets.UTF_8));
            mac.update(FIELD_SEPARATOR);
            mac.update(challengeReference.value().getBytes(StandardCharsets.UTF_8));
            mac.update(FIELD_SEPARATOR);

            ByteBuffer encodedOtp =
                    StandardCharsets.UTF_8.encode(CharBuffer.wrap(otp));
            try {
                mac.update(encodedOtp);
            } finally {
                clear(encodedOtp);
            }

            return toLowercaseHex(mac.doFinal());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "HMAC-SHA256 is unavailable for OTP idempotency",
                    exception
            );
        }
    }

    private static void clear(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            Arrays.fill(buffer.array(), (byte) 0);
            return;
        }
        buffer.clear();
        while (buffer.hasRemaining()) {
            buffer.put((byte) 0);
        }
    }

    private static String toLowercaseHex(byte[] bytes) {
        try {
            StringBuilder value = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                value.append(Character.forDigit((current >>> 4) & 0x0f, 16));
                value.append(Character.forDigit(current & 0x0f, 16));
            }
            return value.toString();
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
''')

write_new(
"backend/payment/src/main/java/com/sixpay/payment/infrastructure/idempotency/PaymentConfirmationReplayCodec.java",
r'''package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.ConfirmationDeliveryChannel;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public final class PaymentConfirmationReplayCodec {

    private static final String VERSION = "v1";
    private static final String NULL = "-";

    public String encode(PaymentConfirmationBankResult result) {
        return String.join(
                "|",
                VERSION,
                encodeText(result.challengeReference().value()),
                result.status().name(),
                result.businessCode().name(),
                result.deliveryChannel() == null ? NULL : result.deliveryChannel().name(),
                result.sentAt() == null ? NULL : result.sentAt().toString(),
                result.expiresAt() == null ? NULL : result.expiresAt().toString(),
                result.verifiedAt() == null ? NULL : result.verifiedAt().toString()
        );
    }

    public PaymentConfirmationBankResult decode(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment confirmation replay payload is empty"
            );
        }
        String[] values = payload.split("\\|", -1);
        if (values.length != 8 || !VERSION.equals(values[0])) {
            throw new IllegalArgumentException(
                    "Unsupported Payment confirmation replay payload"
            );
        }

        return new PaymentConfirmationBankResult(
                new ConfirmationChallengeReference(decodeText(values[1])),
                ConfirmationChallengeStatus.valueOf(values[2]),
                ConfirmationBusinessCode.valueOf(values[3]),
                NULL.equals(values[4])
                        ? null
                        : ConfirmationDeliveryChannel.valueOf(values[4]),
                NULL.equals(values[5]) ? null : Instant.parse(values[5]),
                NULL.equals(values[6]) ? null : Instant.parse(values[6]),
                NULL.equals(values[7]) ? null : Instant.parse(values[7])
        );
    }

    private static String encodeText(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String value) {
        return new String(
                Base64.getUrlDecoder().decode(value),
                StandardCharsets.UTF_8
        );
    }
}
''')

write_new(
"backend/payment/src/main/java/com/sixpay/payment/infrastructure/idempotency/PaymentIdempotencyExecutionDecision.java",
r'''package com.sixpay.payment.infrastructure.idempotency;

import java.util.Objects;

public record PaymentIdempotencyExecutionDecision(
        PaymentIdempotencyDecision decision,
        String requestHash
) {
    public PaymentIdempotencyExecutionDecision {
        decision = Objects.requireNonNull(decision, "Idempotency decision");
        if (requestHash == null || !requestHash.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Effective request hash must be lowercase SHA-256/HMAC hex"
            );
        }
    }
}
''')

write_new(
"backend/payment/src/main/java/com/sixpay/payment/infrastructure/idempotency/PaymentIdempotencyTransactionExecutor.java",
r'''package com.sixpay.payment.infrastructure.idempotency;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class PaymentIdempotencyTransactionExecutor {

    private final PaymentIdempotencyConcurrencyCoordinator coordinator;
    private final PaymentIdempotencyReplayStore replayStore;

    public PaymentIdempotencyTransactionExecutor(
            PaymentIdempotencyConcurrencyCoordinator coordinator,
            PaymentIdempotencyReplayStore replayStore
    ) {
        this.coordinator = Objects.requireNonNull(coordinator, "Coordinator");
        this.replayStore = Objects.requireNonNull(replayStore, "Replay store");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentIdempotencyExecutionDecision begin(
            String operation,
            String idempotencyKey,
            String preferredRequestHash,
            List<String> acceptedRequestHashes,
            Instant startedAt
    ) {
        return coordinator.executeLocked(
                operation,
                idempotencyKey,
                () -> {
                    PaymentIdempotencyReplayStore.BeginResult result =
                            replayStore.beginMatchingHashes(
                                    operation,
                                    idempotencyKey,
                                    preferredRequestHash,
                                    acceptedRequestHashes,
                                    startedAt
                            );
                    return new PaymentIdempotencyExecutionDecision(
                            result.decision(),
                            result.requestHash()
                    );
                }
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(
            String operation,
            String idempotencyKey,
            String requestHash,
            UUID paymentId,
            String responseStatus,
            String responsePayload,
            Instant completedAt
    ) {
        replayStore.complete(
                operation,
                idempotencyKey,
                requestHash,
                paymentId,
                responseStatus,
                responsePayload,
                completedAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOutcomeUnknown(
            String operation,
            String idempotencyKey,
            String requestHash,
            UUID paymentId,
            String recoveryReference,
            String recoveryReason,
            Instant unknownOutcomeAt
    ) {
        replayStore.markOutcomeUnknown(
                operation,
                idempotencyKey,
                requestHash,
                paymentId,
                recoveryReference,
                recoveryReason,
                unknownOutcomeAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(
            String operation,
            String idempotencyKey,
            String requestHash,
            String failureReason,
            Instant failedAt
    ) {
        replayStore.fail(
                operation,
                idempotencyKey,
                requestHash,
                failureReason,
                failedAt
        );
    }
}
''')

write_new(
"backend/payment/src/main/java/com/sixpay/payment/infrastructure/idempotency/PaymentConfirmationIdempotencyAdapter.java",
r'''package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationOutcomeUnknownException;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyPort;
import com.sixpay.payment.application.port.output.idempotency.PaymentOtpHmacKeyProvider;
import com.sixpay.payment.application.service.PaymentConfirmationInProgressException;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Component
@ConditionalOnBean(PaymentOtpHmacKeyProvider.class)
public class PaymentConfirmationIdempotencyAdapter
        implements PaymentConfirmationIdempotencyPort {

    static final String CREATE_OPERATION = "PAYMENT_CONFIRMATION_CREATE";
    static final String VERIFY_OPERATION = "PAYMENT_CONFIRMATION_VERIFY";
    static final String REPLACE_OPERATION = "PAYMENT_CONFIRMATION_REPLACE";

    private final PaymentConfirmationCanonicalizer canonicalizer;
    private final PaymentIdempotencyHasher hasher;
    private final PaymentOtpIdempotencyFingerprint otpFingerprint;
    private final PaymentOtpHmacKeyProvider hmacKeyProvider;
    private final PaymentIdempotencyTransactionExecutor transactions;
    private final PaymentConfirmationReplayCodec replayCodec;
    private final TimeProvider timeProvider;

    public PaymentConfirmationIdempotencyAdapter(
            PaymentConfirmationCanonicalizer canonicalizer,
            PaymentIdempotencyHasher hasher,
            PaymentOtpIdempotencyFingerprint otpFingerprint,
            PaymentOtpHmacKeyProvider hmacKeyProvider,
            PaymentIdempotencyTransactionExecutor transactions,
            PaymentConfirmationReplayCodec replayCodec,
            TimeProvider timeProvider
    ) {
        this.canonicalizer = Objects.requireNonNull(canonicalizer);
        this.hasher = Objects.requireNonNull(hasher);
        this.otpFingerprint = Objects.requireNonNull(otpFingerprint);
        this.hmacKeyProvider = Objects.requireNonNull(hmacKeyProvider);
        this.transactions = Objects.requireNonNull(transactions);
        this.replayCodec = Objects.requireNonNull(replayCodec);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    @Override
    public PaymentConfirmationBankResult executeCreate(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            IdempotencyKey idempotencyKey,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    ) {
        String requestHash = hasher.hash(
                canonicalizer.create(paymentId, paymentReference)
        );
        return execute(
                CREATE_OPERATION,
                paymentId,
                idempotencyKey,
                requestHash,
                List.of(requestHash),
                newRequest,
                recovery
        );
    }

    @Override
    public PaymentConfirmationBankResult executeVerify(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference,
            IdempotencyKey idempotencyKey,
            char[] otp,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    ) {
        Objects.requireNonNull(otp, "OTP");
        char[] localOtp = Arrays.copyOf(otp, otp.length);
        try {
            List<String> hashes = otpFingerprint.fingerprints(
                    paymentReference,
                    challengeReference,
                    localOtp,
                    hmacKeyProvider.verificationKeys()
            );
            return execute(
                    VERIFY_OPERATION,
                    paymentId,
                    idempotencyKey,
                    hashes.get(0),
                    hashes,
                    newRequest,
                    recovery
            );
        } finally {
            Arrays.fill(localOtp, '\0');
        }
    }

    @Override
    public PaymentConfirmationBankResult executeReplace(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference,
            IdempotencyKey idempotencyKey,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    ) {
        String requestHash = hasher.hash(
                canonicalizer.replace(
                        paymentId,
                        paymentReference,
                        challengeReference
                )
        );
        return execute(
                REPLACE_OPERATION,
                paymentId,
                idempotencyKey,
                requestHash,
                List.of(requestHash),
                newRequest,
                recovery
        );
    }

    private PaymentConfirmationBankResult execute(
            String operation,
            PaymentId paymentId,
            IdempotencyKey idempotencyKey,
            String preferredHash,
            List<String> acceptedHashes,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    ) {
        String key = idempotencyKey.value();
        PaymentIdempotencyExecutionDecision execution =
                transactions.begin(
                        operation,
                        key,
                        preferredHash,
                        acceptedHashes,
                        timeProvider.now()
                );

        return switch (execution.decision().kind()) {
            case REPLAY -> replay(paymentId, execution.decision());
            case IN_PROGRESS -> throw new PaymentConfirmationInProgressException();
            case OUTCOME_UNKNOWN -> recoverAndComplete(
                    operation,
                    paymentId,
                    key,
                    execution.requestHash(),
                    recovery
            );
            case NEW -> executeNew(
                    operation,
                    paymentId,
                    key,
                    execution.requestHash(),
                    newRequest,
                    recovery
            );
        };
    }

    private PaymentConfirmationBankResult executeNew(
            String operation,
            PaymentId paymentId,
            String idempotencyKey,
            String requestHash,
            Supplier<PaymentConfirmationBankResult> newRequest,
            Supplier<PaymentConfirmationBankResult> recovery
    ) {
        try {
            PaymentConfirmationBankResult result =
                    Objects.requireNonNull(newRequest.get());
            complete(
                    operation,
                    paymentId,
                    idempotencyKey,
                    requestHash,
                    result
            );
            return result;
        } catch (PaymentConfirmationOutcomeUnknownException unknown) {
            transactions.markOutcomeUnknown(
                    operation,
                    idempotencyKey,
                    requestHash,
                    paymentId.value(),
                    unknown.recoveryReference(),
                    unknown.getMessage(),
                    timeProvider.now()
            );
            return recoverAndComplete(
                    operation,
                    paymentId,
                    idempotencyKey,
                    requestHash,
                    recovery
            );
        } catch (RuntimeException failure) {
            try {
                transactions.fail(
                        operation,
                        idempotencyKey,
                        requestHash,
                        failure.getMessage(),
                        timeProvider.now()
                );
            } catch (RuntimeException persistenceFailure) {
                failure.addSuppressed(persistenceFailure);
            }
            throw failure;
        }
    }

    private PaymentConfirmationBankResult recoverAndComplete(
            String operation,
            PaymentId paymentId,
            String idempotencyKey,
            String requestHash,
            Supplier<PaymentConfirmationBankResult> recovery
    ) {
        PaymentConfirmationBankResult result =
                Objects.requireNonNull(recovery.get());

        complete(
                operation,
                paymentId,
                idempotencyKey,
                requestHash,
                result
        );
        return result;
    }

    private void complete(
            String operation,
            PaymentId paymentId,
            String idempotencyKey,
            String requestHash,
            PaymentConfirmationBankResult result
    ) {
        transactions.complete(
                operation,
                idempotencyKey,
                requestHash,
                paymentId.value(),
                result.status().name(),
                replayCodec.encode(result),
                timeProvider.now()
        );
    }

    private PaymentConfirmationBankResult replay(
            PaymentId paymentId,
            PaymentIdempotencyDecision decision
    ) {
        if (!paymentId.value().equals(decision.paymentId())) {
            throw new IllegalStateException(
                    "Payment confirmation replay Payment ID mismatch"
            );
        }
        PaymentConfirmationBankResult result =
                replayCodec.decode(decision.responsePayload());
        if (!result.status().name().equals(decision.responseStatus())) {
            throw new IllegalStateException(
                    "Payment confirmation replay status mismatch"
            );
        }
        return result;
    }
}
''')

replace_file(
"backend/payment/src/main/java/com/sixpay/payment/infrastructure/idempotency/PaymentIdempotencyReplayStore.java",
"public class PaymentIdempotencyReplayStore",
r'''package com.sixpay.payment.infrastructure.idempotency;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentIdempotencyReplayStore {

    private final PaymentIdempotencyRepository repository;

    public PaymentIdempotencyReplayStore(
            PaymentIdempotencyRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public PaymentIdempotencyDecision begin(
            String operation,
            String idempotencyKey,
            String requestHash,
            Instant startedAt
    ) {
        return beginMatchingHashes(
                operation,
                idempotencyKey,
                requestHash,
                List.of(requestHash),
                startedAt
        ).decision();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public BeginResult beginMatchingHashes(
            String operation,
            String idempotencyKey,
            String preferredRequestHash,
            List<String> acceptedRequestHashes,
            Instant startedAt
    ) {
        validate(operation, idempotencyKey, preferredRequestHash);
        Objects.requireNonNull(startedAt);

        LinkedHashSet<String> accepted = normalizeHashes(acceptedRequestHashes);
        if (!accepted.contains(preferredRequestHash)) {
            throw new IllegalArgumentException(
                    "Preferred request hash must be accepted"
            );
        }

        Optional<PaymentIdempotencyEntity> existing =
                repository.findByOperationAndIdempotencyKey(
                        operation,
                        idempotencyKey
                );

        if (existing.isEmpty()) {
            repository.saveAndFlush(
                    PaymentIdempotencyEntity.start(
                            operation,
                            idempotencyKey,
                            preferredRequestHash,
                            startedAt
                    )
            );
            return new BeginResult(
                    PaymentIdempotencyDecision.newRequest(),
                    preferredRequestHash
            );
        }

        PaymentIdempotencyEntity entity = existing.orElseThrow();
        if (!accepted.contains(entity.requestHash())) {
            throw new PaymentIdempotencyConflictException(
                    operation,
                    idempotencyKey
            );
        }

        PaymentIdempotencyDecision decision =
                switch (entity.status()) {
                    case COMPLETED ->
                            PaymentIdempotencyDecision.replay(entity);
                    case IN_PROGRESS ->
                            PaymentIdempotencyDecision.inProgress();
                    case OUTCOME_UNKNOWN ->
                            PaymentIdempotencyDecision.outcomeUnknown(entity);
                    case FAILED -> {
                        entity.restart(startedAt);
                        repository.saveAndFlush(entity);
                        yield PaymentIdempotencyDecision.newRequest();
                    }
                };

        return new BeginResult(decision, entity.requestHash());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markOutcomeUnknown(
            String operation,
            String idempotencyKey,
            String requestHash,
            UUID paymentId,
            String recoveryReference,
            String recoveryReason,
            Instant unknownOutcomeAt
    ) {
        PaymentIdempotencyEntity entity =
                requireExisting(operation, idempotencyKey);
        requireSameHash(entity, operation, idempotencyKey, requestHash);
        entity.markOutcomeUnknown(
                paymentId,
                recoveryReference,
                recoveryReason,
                unknownOutcomeAt
        );
        repository.saveAndFlush(entity);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(
            String operation,
            String idempotencyKey,
            String requestHash,
            UUID paymentId,
            String responseStatus,
            String responsePayload,
            Instant completedAt
    ) {
        PaymentIdempotencyEntity entity =
                requireExisting(operation, idempotencyKey);
        requireSameHash(entity, operation, idempotencyKey, requestHash);
        entity.complete(
                paymentId,
                responseStatus,
                responsePayload,
                completedAt
        );
        repository.saveAndFlush(entity);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void fail(
            String operation,
            String idempotencyKey,
            String requestHash,
            String failureReason,
            Instant failedAt
    ) {
        PaymentIdempotencyEntity entity =
                requireExisting(operation, idempotencyKey);
        requireSameHash(entity, operation, idempotencyKey, requestHash);
        entity.fail(failureReason, failedAt);
        repository.saveAndFlush(entity);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentIdempotencyDecision> findReplay(
            String operation,
            String idempotencyKey,
            String requestHash
    ) {
        validate(operation, idempotencyKey, requestHash);

        return repository
                .findByOperationAndIdempotencyKey(operation, idempotencyKey)
                .map(entity -> {
                    requireSameHash(
                            entity,
                            operation,
                            idempotencyKey,
                            requestHash
                    );
                    return switch (entity.status()) {
                        case COMPLETED ->
                                PaymentIdempotencyDecision.replay(entity);
                        case OUTCOME_UNKNOWN ->
                                PaymentIdempotencyDecision.outcomeUnknown(entity);
                        case IN_PROGRESS, FAILED ->
                                PaymentIdempotencyDecision.inProgress();
                    };
                });
    }

    private PaymentIdempotencyEntity requireExisting(
            String operation,
            String idempotencyKey
    ) {
        return repository
                .findByOperationAndIdempotencyKey(operation, idempotencyKey)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Idempotency record does not exist"
                        )
                );
    }

    private static void requireSameHash(
            PaymentIdempotencyEntity entity,
            String operation,
            String idempotencyKey,
            String requestHash
    ) {
        if (!entity.requestHash().equals(requestHash)) {
            throw new PaymentIdempotencyConflictException(
                    operation,
                    idempotencyKey
            );
        }
    }

    private static LinkedHashSet<String> normalizeHashes(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one accepted request hash is required"
            );
        }
        LinkedHashSet<String> hashes = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || !value.matches("^[0-9a-f]{64}$")) {
                throw new IllegalArgumentException(
                        "Accepted request hash must be lowercase SHA-256/HMAC hex"
                );
            }
            hashes.add(value);
        }
        return hashes;
    }

    private static void validate(
            String operation,
            String idempotencyKey,
            String requestHash
    ) {
        requireText(operation, 160, "Idempotency operation");
        requireText(idempotencyKey, 150, "Idempotency key");
        if (requestHash == null || !requestHash.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Request hash must be a lowercase SHA-256 value"
            );
        }
    }

    private static void requireText(
            String value,
            int maximumLength,
            String label
    ) {
        if (value == null
                || value.isBlank()
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(
                    label + " must be non-blank and at most "
                            + maximumLength + " characters"
            );
        }
    }

    public record BeginResult(
            PaymentIdempotencyDecision decision,
            String requestHash
    ) {
        public BeginResult {
            decision = Objects.requireNonNull(decision);
            if (requestHash == null || !requestHash.matches("^[0-9a-f]{64}$")) {
                throw new IllegalArgumentException(
                        "Effective request hash must be lowercase SHA-256/HMAC hex"
                );
            }
        }
    }
}
''')

replace_file(
"backend/payment/src/main/java/com/sixpay/payment/application/service/PaymentConfirmationService.java",
"public class PaymentConfirmationService",
r'''package com.sixpay.payment.application.service;

import com.sixpay.payment.application.command.CreatePaymentConfirmationCommand;
import com.sixpay.payment.application.command.ResendPaymentConfirmationCommand;
import com.sixpay.payment.application.command.VerifyPaymentConfirmationCommand;
import com.sixpay.payment.application.port.input.CreatePaymentConfirmationUseCase;
import com.sixpay.payment.application.port.input.ReadPaymentConfirmationUseCase;
import com.sixpay.payment.application.port.input.ResendPaymentConfirmationUseCase;
import com.sixpay.payment.application.port.input.VerifyPaymentConfirmationUseCase;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.banking.BankingIdempotencyKey;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyPort;
import com.sixpay.payment.application.query.ReadPaymentConfirmationQuery;
import com.sixpay.payment.application.view.PaymentConfirmationView;
import com.sixpay.payment.domain.model.ConfirmationChallenge;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;

@Service
@ConditionalOnBean({
        PaymentConfirmationGateway.class,
        PaymentConfirmationIdempotencyPort.class
})
public class PaymentConfirmationService
        implements CreatePaymentConfirmationUseCase,
        ReadPaymentConfirmationUseCase,
        VerifyPaymentConfirmationUseCase,
        ResendPaymentConfirmationUseCase {

    private final PaymentLookupPort paymentLookupPort;
    private final PaymentConfirmationGateway confirmationGateway;
    private final PaymentConfirmationIdempotencyPort idempotencyPort;

    public PaymentConfirmationService(
            PaymentLookupPort paymentLookupPort,
            PaymentConfirmationGateway confirmationGateway,
            PaymentConfirmationIdempotencyPort idempotencyPort
    ) {
        this.paymentLookupPort = Objects.requireNonNull(paymentLookupPort);
        this.confirmationGateway = Objects.requireNonNull(confirmationGateway);
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort);
    }

    @Override
    public PaymentConfirmationView create(
            CreatePaymentConfirmationCommand command
    ) {
        Objects.requireNonNull(command);

        Payment payment = requirePayment(command.paymentReference());
        requirePendingConfirmation(payment);

        payment.toState()
                .confirmationChallenge()
                .filter(ConfirmationChallenge::active)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Payment already has an active confirmation challenge"
                    );
                });

        BankingRequestContext context =
                bankingContext(payment, command.correlationId());
        BankingIdempotencyKey bankKey =
                bankingIdempotencyKey(command.idempotencyKey().value());

        PaymentConfirmationBankResult result =
                idempotencyPort.executeCreate(
                        payment.id(),
                        payment.publicPaymentReference(),
                        command.idempotencyKey(),
                        () -> confirmationGateway.create(
                                new PaymentConfirmationGateway.CreateRequest(
                                        payment,
                                        context,
                                        bankKey
                                )
                        ),
                        () -> confirmationGateway.recover(
                                new PaymentConfirmationGateway.RecoveryRequest(
                                        context,
                                        bankKey
                                )
                        )
                );

        return PaymentConfirmationView.from(
                payment.publicPaymentReference(),
                result
        );
    }

    @Override
    public PaymentConfirmationView read(
            ReadPaymentConfirmationQuery query
    ) {
        Objects.requireNonNull(query);

        Payment payment = requirePayment(query.paymentReference());
        ConfirmationChallenge challenge = requireCurrentChallenge(payment);

        PaymentConfirmationBankResult result =
                confirmationGateway.lookup(
                        new PaymentConfirmationGateway.LookupRequest(
                                payment.id(),
                                payment.publicPaymentReference(),
                                challenge.challengeReference(),
                                bankingContext(payment, query.correlationId())
                        )
                );

        return PaymentConfirmationView.from(
                payment.publicPaymentReference(),
                result
        );
    }

    @Override
    public PaymentConfirmationView verify(
            VerifyPaymentConfirmationCommand command
    ) {
        Objects.requireNonNull(command);

        Payment payment = requirePayment(command.paymentReference());
        requirePendingConfirmation(payment);
        ConfirmationChallenge challenge = requireCurrentChallenge(payment);

        BankingRequestContext context =
                bankingContext(payment, command.correlationId());
        BankingIdempotencyKey bankKey =
                bankingIdempotencyKey(command.idempotencyKey().value());

        char[] otp = command.otp();
        try {
            PaymentConfirmationBankResult result =
                    idempotencyPort.executeVerify(
                            payment.id(),
                            payment.publicPaymentReference(),
                            challenge.challengeReference(),
                            command.idempotencyKey(),
                            otp,
                            () -> confirmationGateway.verify(
                                    new PaymentConfirmationGateway.VerifyRequest(
                                            payment.id(),
                                            payment.publicPaymentReference(),
                                            challenge.challengeReference(),
                                            context,
                                            bankKey,
                                            otp
                                    )
                            ),
                            () -> confirmationGateway.recover(
                                    new PaymentConfirmationGateway.RecoveryRequest(
                                            context,
                                            bankKey
                                    )
                            )
                    );

            return PaymentConfirmationView.from(
                    payment.publicPaymentReference(),
                    result
            );
        } finally {
            Arrays.fill(otp, '\0');
        }
    }

    @Override
    public PaymentConfirmationView resend(
            ResendPaymentConfirmationCommand command
    ) {
        Objects.requireNonNull(command);

        Payment payment = requirePayment(command.paymentReference());
        requirePendingConfirmation(payment);
        ConfirmationChallenge challenge = requireCurrentChallenge(payment);

        BankingRequestContext context =
                bankingContext(payment, command.correlationId());
        BankingIdempotencyKey bankKey =
                bankingIdempotencyKey(command.idempotencyKey().value());

        PaymentConfirmationBankResult result =
                idempotencyPort.executeReplace(
                        payment.id(),
                        payment.publicPaymentReference(),
                        challenge.challengeReference(),
                        command.idempotencyKey(),
                        () -> confirmationGateway.replace(
                                new PaymentConfirmationGateway.ReplaceRequest(
                                        payment.id(),
                                        payment.publicPaymentReference(),
                                        challenge.challengeReference(),
                                        context,
                                        bankKey
                                )
                        ),
                        () -> confirmationGateway.recover(
                                new PaymentConfirmationGateway.RecoveryRequest(
                                        context,
                                        bankKey
                                )
                        )
                );

        return PaymentConfirmationView.from(
                payment.publicPaymentReference(),
                result
        );
    }

    private Payment requirePayment(PublicPaymentReference paymentReference) {
        return paymentLookupPort
                .findByPublicPaymentReference(paymentReference)
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found: " + paymentReference
                ));
    }

    private static void requirePendingConfirmation(Payment payment) {
        if (payment.status() != PaymentStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException(
                    "Payment confirmation operation requires PENDING_CONFIRMATION"
            );
        }
    }

    private static ConfirmationChallenge requireCurrentChallenge(
            Payment payment
    ) {
        return payment.toState()
                .confirmationChallenge()
                .orElseThrow(() -> new IllegalStateException(
                        "Payment has no current confirmation challenge"
                ));
    }

    private static BankingRequestContext bankingContext(
            Payment payment,
            com.sixpay.common.context.CorrelationId correlationId
    ) {
        return new BankingRequestContext(
                correlationId,
                payment.toState().financialInstitutionCode()
        );
    }

    private static BankingIdempotencyKey bankingIdempotencyKey(String value) {
        return new BankingIdempotencyKey(value);
    }
}
''')

write_new(
"backend/payment/src/test/java/com/sixpay/payment/infrastructure/idempotency/PaymentOtpIdempotencyFingerprintTest.java",
r'''package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentOtpIdempotencyFingerprintTest {

    private final PaymentOtpIdempotencyFingerprint fingerprint =
            new PaymentOtpIdempotencyFingerprint();

    @Test
    void persistsOnlyHmacEquivalentNotRawOtpHash() throws Exception {
        char[] otp = "123456".toCharArray();
        SecretKey key = new SecretKeySpec(
                "current-secret-key-material-32-bytes"
                        .getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        List<String> hashes = fingerprint.fingerprints(
                PublicPaymentReference.of("PAY-IDEM-FP-001"),
                new ConfirmationChallengeReference("challenge-fp-001"),
                otp,
                List.of(key)
        );

        assertEquals(1, hashes.size());
        assertTrue(hashes.get(0).matches("^[0-9a-f]{64}$"));

        byte[] rawDigest = MessageDigest.getInstance("SHA-256")
                .digest("123456".getBytes(StandardCharsets.UTF_8));
        StringBuilder rawHash = new StringBuilder();
        for (byte value : rawDigest) {
            rawHash.append(String.format("%02x", value));
        }
        assertNotEquals(rawHash.toString(), hashes.get(0));
    }

    @Test
    void supportsPreviousKeysForReplayRecognition() {
        List<String> hashes = fingerprint.fingerprints(
                PublicPaymentReference.of("PAY-IDEM-FP-002"),
                new ConfirmationChallengeReference("challenge-fp-002"),
                "654321".toCharArray(),
                List.of(
                        new SecretKeySpec(
                                "current-secret-key-material-32-bytes"
                                        .getBytes(StandardCharsets.UTF_8),
                                "HmacSHA256"
                        ),
                        new SecretKeySpec(
                                "previous-secret-key-material-32byt"
                                        .getBytes(StandardCharsets.UTF_8),
                                "HmacSHA256"
                        )
                )
        );

        assertEquals(2, hashes.size());
        assertNotEquals(hashes.get(0), hashes.get(1));
    }
}
''')

write_new(
"backend/payment/src/test/java/com/sixpay/payment/infrastructure/idempotency/PaymentConfirmationReplayCodecTest.java",
r'''package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.ConfirmationDeliveryChannel;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PaymentConfirmationReplayCodecTest {

    @Test
    void roundTripsStableResultWithoutOtp() {
        PaymentConfirmationReplayCodec codec =
                new PaymentConfirmationReplayCodec();

        PaymentConfirmationBankResult expected =
                new PaymentConfirmationBankResult(
                        new ConfirmationChallengeReference("challenge-replay-001"),
                        ConfirmationChallengeStatus.ACTIVE,
                        ConfirmationBusinessCode.CHALLENGE_ACTIVE,
                        ConfirmationDeliveryChannel.SMS,
                        Instant.parse("2026-08-30T12:00:00Z"),
                        Instant.parse("2026-08-30T12:05:00Z"),
                        null
                );

        String encoded = codec.encode(expected);

        assertEquals(expected, codec.decode(encoded));
        assertFalse(encoded.contains("123456"));
    }
}
''')

write_new(
"backend/payment/src/test/java/com/sixpay/payment/infrastructure/idempotency/PaymentConfirmationIdempotencyAdapterTest.java",
r'''package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationOutcomeUnknownException;
import com.sixpay.payment.application.port.output.idempotency.PaymentOtpHmacKeyProvider;
import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentConfirmationIdempotencyAdapterTest {

    @Test
    void completedVerifyReplayNeverCallsOriginalBankVerification() {
        PaymentConfirmationCanonicalizer canonicalizer =
                new PaymentConfirmationCanonicalizer();
        PaymentIdempotencyHasher hasher = new PaymentIdempotencyHasher();
        PaymentOtpIdempotencyFingerprint fingerprint =
                new PaymentOtpIdempotencyFingerprint();
        PaymentOtpHmacKeyProvider keys = () -> List.of(
                new SecretKeySpec(
                        "current-secret-key-material-32-bytes"
                                .getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"
                )
        );
        PaymentIdempotencyTransactionExecutor tx =
                mock(PaymentIdempotencyTransactionExecutor.class);
        PaymentConfirmationReplayCodec codec =
                new PaymentConfirmationReplayCodec();
        TimeProvider time = mock(TimeProvider.class);

        PaymentId paymentId = new PaymentId(
                UUID.fromString("11111111-1111-4111-8111-111111111111")
        );
        PublicPaymentReference paymentReference =
                PublicPaymentReference.of("PAY-IDEM-VERIFY-001");
        ConfirmationChallengeReference challenge =
                new ConfirmationChallengeReference("challenge-verify-001");
        IdempotencyKey key = IdempotencyKey.of("idem-verify-001");
        char[] otp = "123456".toCharArray();

        List<String> hashes = fingerprint.fingerprints(
                paymentReference,
                challenge,
                otp,
                keys.verificationKeys()
        );

        PaymentConfirmationBankResult replayed =
                new PaymentConfirmationBankResult(
                        challenge,
                        ConfirmationChallengeStatus.ACTIVE,
                        ConfirmationBusinessCode.OTP_INVALID,
                        null,
                        null,
                        null,
                        null
                );

        PaymentIdempotencyDecision decision =
                new PaymentIdempotencyDecision(
                        PaymentIdempotencyDecision.Kind.REPLAY,
                        paymentId.value(),
                        replayed.status().name(),
                        codec.encode(replayed),
                        null,
                        null,
                        null
                );

        when(time.now()).thenReturn(Instant.parse("2026-08-30T12:00:00Z"));
        when(tx.begin(
                eq(PaymentConfirmationIdempotencyAdapter.VERIFY_OPERATION),
                eq(key.value()),
                eq(hashes.get(0)),
                eq(hashes),
                any(Instant.class)
        )).thenReturn(
                new PaymentIdempotencyExecutionDecision(
                        decision,
                        hashes.get(0)
                )
        );

        AtomicInteger bankCalls = new AtomicInteger();
        AtomicInteger recoveryCalls = new AtomicInteger();

        PaymentConfirmationIdempotencyAdapter adapter =
                new PaymentConfirmationIdempotencyAdapter(
                        canonicalizer,
                        hasher,
                        fingerprint,
                        keys,
                        tx,
                        codec,
                        time
                );

        PaymentConfirmationBankResult actual =
                adapter.executeVerify(
                        paymentId,
                        paymentReference,
                        challenge,
                        key,
                        otp,
                        () -> {
                            bankCalls.incrementAndGet();
                            return replayed;
                        },
                        () -> {
                            recoveryCalls.incrementAndGet();
                            return replayed;
                        }
                );

        assertEquals(replayed, actual);
        assertEquals(0, bankCalls.get());
        assertEquals(0, recoveryCalls.get());
    }

    @Test
    void uncertainCreateMarksUnknownThenUsesRecoveryWithoutBlindRetry() {
        PaymentConfirmationCanonicalizer canonicalizer =
                new PaymentConfirmationCanonicalizer();
        PaymentIdempotencyHasher hasher = new PaymentIdempotencyHasher();
        PaymentOtpIdempotencyFingerprint fingerprint =
                new PaymentOtpIdempotencyFingerprint();
        PaymentOtpHmacKeyProvider keys = List::of;
        PaymentIdempotencyTransactionExecutor tx =
                mock(PaymentIdempotencyTransactionExecutor.class);
        PaymentConfirmationReplayCodec codec =
                new PaymentConfirmationReplayCodec();
        TimeProvider time = mock(TimeProvider.class);

        PaymentId paymentId = new PaymentId(
                UUID.fromString("22222222-2222-4222-8222-222222222222")
        );
        PublicPaymentReference paymentReference =
                PublicPaymentReference.of("PAY-IDEM-CREATE-001");
        IdempotencyKey key = IdempotencyKey.of("idem-create-001");

        String requestHash = hasher.hash(
                canonicalizer.create(paymentId, paymentReference)
        );

        when(time.now()).thenReturn(
                Instant.parse("2026-08-30T12:00:00Z"),
                Instant.parse("2026-08-30T12:00:01Z"),
                Instant.parse("2026-08-30T12:00:02Z")
        );
        when(tx.begin(
                eq(PaymentConfirmationIdempotencyAdapter.CREATE_OPERATION),
                eq(key.value()),
                eq(requestHash),
                eq(List.of(requestHash)),
                any(Instant.class)
        )).thenReturn(
                new PaymentIdempotencyExecutionDecision(
                        PaymentIdempotencyDecision.newRequest(),
                        requestHash
                )
        );

        AtomicInteger createCalls = new AtomicInteger();
        AtomicInteger recoveryCalls = new AtomicInteger();

        PaymentConfirmationBankResult recovered =
                new PaymentConfirmationBankResult(
                        new ConfirmationChallengeReference(
                                "challenge-create-recovered"
                        ),
                        ConfirmationChallengeStatus.ACTIVE,
                        ConfirmationBusinessCode.CHALLENGE_ACTIVE,
                        null,
                        null,
                        null,
                        null
                );

        PaymentConfirmationIdempotencyAdapter adapter =
                new PaymentConfirmationIdempotencyAdapter(
                        canonicalizer,
                        hasher,
                        fingerprint,
                        keys,
                        tx,
                        codec,
                        time
                );

        PaymentConfirmationBankResult actual =
                adapter.executeCreate(
                        paymentId,
                        paymentReference,
                        key,
                        () -> {
                            createCalls.incrementAndGet();
                            throw new PaymentConfirmationOutcomeUnknownException(
                                    "create response timeout"
                            );
                        },
                        () -> {
                            recoveryCalls.incrementAndGet();
                            return recovered;
                        }
                );

        assertEquals(recovered, actual);
        assertEquals(1, createCalls.get());
        assertEquals(1, recoveryCalls.get());

        verify(tx).markOutcomeUnknown(
                eq(PaymentConfirmationIdempotencyAdapter.CREATE_OPERATION),
                eq(key.value()),
                eq(requestHash),
                eq(paymentId.value()),
                isNull(),
                eq("create response timeout"),
                any(Instant.class)
        );
    }

    @Test
    void existingUnknownReplacementPerformsOnlyRecovery() {
        PaymentConfirmationCanonicalizer canonicalizer =
                new PaymentConfirmationCanonicalizer();
        PaymentIdempotencyHasher hasher = new PaymentIdempotencyHasher();
        PaymentOtpIdempotencyFingerprint fingerprint =
                new PaymentOtpIdempotencyFingerprint();
        PaymentOtpHmacKeyProvider keys = List::of;
        PaymentIdempotencyTransactionExecutor tx =
                mock(PaymentIdempotencyTransactionExecutor.class);
        PaymentConfirmationReplayCodec codec =
                new PaymentConfirmationReplayCodec();
        TimeProvider time = mock(TimeProvider.class);

        PaymentId paymentId = new PaymentId(
                UUID.fromString("33333333-3333-4333-8333-333333333333")
        );
        PublicPaymentReference paymentReference =
                PublicPaymentReference.of("PAY-IDEM-REPLACE-001");
        ConfirmationChallengeReference challenge =
                new ConfirmationChallengeReference("challenge-replace-old");
        IdempotencyKey key = IdempotencyKey.of("idem-replace-001");

        String requestHash = hasher.hash(
                canonicalizer.replace(
                        paymentId,
                        paymentReference,
                        challenge
                )
        );

        PaymentIdempotencyDecision unknown =
                new PaymentIdempotencyDecision(
                        PaymentIdempotencyDecision.Kind.OUTCOME_UNKNOWN,
                        paymentId.value(),
                        null,
                        null,
                        null,
                        "replacement response timeout",
                        Instant.parse("2026-08-30T12:00:00Z")
                );

        when(time.now()).thenReturn(
                Instant.parse("2026-08-30T12:01:00Z"),
                Instant.parse("2026-08-30T12:01:01Z")
        );
        when(tx.begin(
                eq(PaymentConfirmationIdempotencyAdapter.REPLACE_OPERATION),
                eq(key.value()),
                eq(requestHash),
                eq(List.of(requestHash)),
                any(Instant.class)
        )).thenReturn(
                new PaymentIdempotencyExecutionDecision(
                        unknown,
                        requestHash
                )
        );

        AtomicInteger replaceCalls = new AtomicInteger();
        AtomicInteger recoveryCalls = new AtomicInteger();

        PaymentConfirmationBankResult recovered =
                new PaymentConfirmationBankResult(
                        new ConfirmationChallengeReference(
                                "challenge-replace-new"
                        ),
                        ConfirmationChallengeStatus.ACTIVE,
                        ConfirmationBusinessCode.CHALLENGE_ACTIVE,
                        null,
                        null,
                        null,
                        null
                );

        PaymentConfirmationIdempotencyAdapter adapter =
                new PaymentConfirmationIdempotencyAdapter(
                        canonicalizer,
                        hasher,
                        fingerprint,
                        keys,
                        tx,
                        codec,
                        time
                );

        PaymentConfirmationBankResult actual =
                adapter.executeReplace(
                        paymentId,
                        paymentReference,
                        challenge,
                        key,
                        () -> {
                            replaceCalls.incrementAndGet();
                            return recovered;
                        },
                        () -> {
                            recoveryCalls.incrementAndGet();
                            return recovered;
                        }
                );

        assertEquals(recovered, actual);
        assertEquals(0, replaceCalls.get());
        assertEquals(1, recoveryCalls.get());
    }
}
''')

print("LOT 1.4 patch generated into the local working tree.")
print("No commit/push/branch/PR/remote operation performed.")
print("Run targeted validation next from backend:")
print("mvn -pl payment -Dtest=PaymentOtpIdempotencyFingerprintTest,PaymentConfirmationReplayCodecTest,PaymentConfirmationIdempotencyAdapterTest test")
