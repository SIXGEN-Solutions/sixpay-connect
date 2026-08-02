package com.sixpay.payment.infrastructure.initiation;

import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.payment.application.command.InitiateDebitBeneficiaryCommand;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.port.out.initiation.PaymentInitiationPreparationPort;
import com.sixpay.payment.application.port.out.initiation.PreparedPaymentInitiation;
import com.sixpay.payment.domain.model.CallbackEndpoint;
import com.sixpay.payment.domain.model.DebtorAccountReference;
import com.sixpay.payment.domain.model.ExternalPaymentReference;
import com.sixpay.payment.domain.model.ExternalSubscriptionReference;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.NewPaymentIntent;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.model.PaymentRequestIdentity;
import com.sixpay.payment.domain.model.PaymentSource;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.model.RequestFingerprint;
import com.sixpay.payment.domain.model.TreasuryAllocation;
import com.sixpay.payment.domain.model.TreasuryAllocationIntent;
import com.sixpay.payment.domain.model.TreasuryBeneficiaryReference;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public final class PaymentInitiationPreparationAdapter
        implements PaymentInitiationPreparationPort {

    private final IdentifierGenerator<UUID> identifierGenerator;

    public PaymentInitiationPreparationAdapter(
            IdentifierGenerator<UUID> identifierGenerator
    ) {
        this.identifierGenerator =
                Objects.requireNonNull(identifierGenerator);
    }

    @Override
    public PreparedPaymentInitiation prepare(
            InitiateDebitCommand command,
            String requestHash,
            Instant receivedAt
    ) {
        Objects.requireNonNull(command);
        Objects.requireNonNull(receivedAt);
        requireSha256(requestHash);

        UUID generatedId = identifierGenerator.generate();
        PaymentId paymentId = new PaymentId(generatedId);

        FinancialInstitutionCode institution =
                FinancialInstitutionCode.of(
                        institutionCode(command.debtorRib())
                );

        Money total = Money.of(
                command.totalAmount(),
                command.currency()
        );

        TreasuryAllocationIntent allocations =
                new TreasuryAllocationIntent(
                        allocations(command),
                        total
                );

        String allocationHash =
                sha256(canonicalAllocations(command));

        NewPaymentIntent intent = new NewPaymentIntent(
                PaymentSource.TRESOR_PAY,
                ExternalPaymentReference.of(command.endToEndId()),
                ExternalSubscriptionReference.of(
                        subscriptionReference(command)
                ),
                new PaymentRequestIdentity(
                        IdempotencyKey.of(command.idempotencyKey()),
                        RequestFingerprint.of(requestHash),
                        command.correlationId()
                ),
                institution,
                debtorReference(command, institution),
                total,
                allocations,
                EvidenceFingerprint.of(
                        "v1:sha256:" + allocationHash
                ),
                new PaymentInitiationContext(
                        command.partnerLoginName(),
                        command.applicationId(),
                        command.debtorName(),
                        command.claimType(),
                        command.taxpayerIdentifier(),
                        command.requestedExecutionAt(),
                        CallbackEndpoint.of(command.callbackUrl())
                )
        );

        return new PreparedPaymentInitiation(
                paymentId,
                publicReference(generatedId),
                intent,
                receivedAt
        );
    }

    private static DebtorAccountReference debtorReference(
            InitiateDebitCommand command,
            FinancialInstitutionCode institution
    ) {
        String hash = sha256(
                command.partnerLoginName()
                        + "|"
                        + command.debtorRib()
        );

        return new DebtorAccountReference(
                institution,
                "acct:v1:" + hash,
                maskedRib(command.debtorRib()),
                "v1:" + hash
        );
    }

    private static List<TreasuryAllocation> allocations(
            InitiateDebitCommand command
    ) {
        return command.beneficiaries()
                .stream()
                .map(item -> new TreasuryAllocation(
                        beneficiaryReference(item),
                        Money.of(
                                item.amount(),
                                command.currency()
                        )
                ))
                .toList();
    }

    private static TreasuryBeneficiaryReference beneficiaryReference(
            InitiateDebitBeneficiaryCommand item
    ) {
        return TreasuryBeneficiaryReference.of(
                "BEN_" + sha256(item.rib()).substring(0, 32)
        );
    }

    private static String canonicalAllocations(
            InitiateDebitCommand command
    ) {
        return command.beneficiaries()
                .stream()
                .sorted(
                        Comparator.comparing(
                                InitiateDebitBeneficiaryCommand::rib
                        )
                )
                .map(item ->
                        item.rib()
                                + "="
                                + item.amount()
                                .stripTrailingZeros()
                                .toPlainString()
                )
                .collect(Collectors.joining("|"));
    }

    private static String subscriptionReference(
            InitiateDebitCommand command
    ) {
        return command.applicationId() == null
                ? command.partnerLoginName()
                : command.partnerLoginName()
                + ":"
                + command.applicationId();
    }

    private static String institutionCode(String rib) {
        int separator = rib.indexOf('-');

        if (separator <= 0) {
            throw new IllegalArgumentException(
                    "Debtor RIB must start with an institution code"
            );
        }

        return rib.substring(0, separator)
                .toUpperCase(Locale.ROOT);
    }

    private static String maskedRib(String rib) {
        String compact = rib.replace("-", "");
        String suffix = compact.length() <= 4
                ? compact
                : compact.substring(compact.length() - 4);

        return "RIB-****-" + suffix;
    }

    private static PublicPaymentReference publicReference(
            UUID identifier
    ) {
        String encoded = identifier.toString()
                .replace("-", "")
                .substring(0, 26)
                .toUpperCase(Locale.ROOT);

        return PublicPaymentReference.of("PAY-" + encoded);
    }

    private static void requireSha256(String value) {
        if (value == null
                || !value.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Request hash must contain 64 lowercase hexadecimal characters"
            );
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}
