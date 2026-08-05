package com.sixpay.customer.observation.infrastructure.query.cursor;

import com.sixpay.customer.observation.application.exception
        .InvalidObservedCustomerCursorException;
import com.sixpay.customer.observation.application.port.output.query
        .ObservedCustomerCursorCodec;
import com.sixpay.customer.observation.application.query
        .ListObservedCustomerPaymentsQuery;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerCursor;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentPosition;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchPosition;
import com.sixpay.customer.observation.application.query
        .SearchObservedCustomersQuery;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Versioned HMAC-SHA-256 cursor codec with a stable binary payload.
 *
 * <p>The token contains only pagination metadata and a SHA-256 fingerprint
 * of the query. NIU, legal name and account values are never embedded.</p>
 */
public final class HmacObservedCustomerCursorCodec
        implements ObservedCustomerCursorCodec {

    private static final int MAGIC = 0x53495843;
    private static final int SCHEMA_VERSION = 1;
    private static final byte SEARCH_TYPE = 1;
    private static final byte PAYMENT_TYPE = 2;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MINIMUM_KEY_BYTES = 32;

    private final byte[] key;

    public HmacObservedCustomerCursorCodec(
            byte[] authenticationKey
    ) {
        Objects.requireNonNull(
                authenticationKey,
                "authenticationKey is required"
        );

        if (authenticationKey.length < MINIMUM_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "cursor authentication key must contain at least "
                            + MINIMUM_KEY_BYTES
                            + " bytes"
            );
        }

        key = authenticationKey.clone();
    }

    @Override
    public ObservedCustomerSearchCriteria decodeSearch(
            SearchObservedCustomersQuery query
    ) {
        Objects.requireNonNull(query, "query is required");

        if (query.cursor() == null) {
            return searchCriteria(
                    query,
                    null
            );
        }

        byte[] payload = authenticate(
                query.cursor()
        );

        try (DataInputStream input =
                     new DataInputStream(
                             new ByteArrayInputStream(payload)
                     )) {

            requireHeader(input, SEARCH_TYPE);

            String encodedSort = input.readUTF();
            Instant snapshotAt = readInstant(input);
            Instant lastSortValue = readInstant(input);
            ObservedCustomerId lastCustomerId =
                    ObservedCustomerId.of(
                            readUuid(input)
                    );
            byte[] encodedFingerprint =
                    readFingerprint(input);

            requireFullyConsumed(input);

            if (!query.sort().name().equals(encodedSort)) {
                throw invalid(
                        "cursor sort does not match the request"
                );
            }

            if (!query.snapshotAt().equals(snapshotAt)) {
                throw invalid(
                        "cursor snapshot does not match the request"
                );
            }

            requireFingerprint(
                    encodedFingerprint,
                    searchFingerprint(query)
            );

            return searchCriteria(
                    query,
                    new ObservedCustomerSearchPosition(
                            lastSortValue,
                            lastCustomerId
                    )
            );
        } catch (InvalidObservedCustomerCursorException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw invalid(
                    "Observed Customer search cursor is malformed",
                    exception
            );
        }
    }

    @Override
    public ObservedCustomerPaymentCriteria decodePayments(
            ListObservedCustomerPaymentsQuery query
    ) {
        Objects.requireNonNull(query, "query is required");

        if (query.cursor() == null) {
            return paymentCriteria(
                    query,
                    null
            );
        }

        byte[] payload = authenticate(
                query.cursor()
        );

        try (DataInputStream input =
                     new DataInputStream(
                             new ByteArrayInputStream(payload)
                     )) {

            requireHeader(input, PAYMENT_TYPE);

            Instant snapshotAt = readInstant(input);
            Instant lastCreatedAt = readInstant(input);
            UUID lastPaymentId = readUuid(input);
            ObservedCustomerId encodedCustomerId =
                    ObservedCustomerId.of(
                            readUuid(input)
                    );
            byte[] encodedFingerprint =
                    readFingerprint(input);

            requireFullyConsumed(input);

            if (!query.snapshotAt().equals(snapshotAt)) {
                throw invalid(
                        "cursor snapshot does not match the request"
                );
            }

            if (!query.observedCustomerId().equals(
                    encodedCustomerId
            )) {
                throw invalid(
                        "cursor customer does not match the request"
                );
            }

            requireFingerprint(
                    encodedFingerprint,
                    paymentFingerprint(query)
            );

            return paymentCriteria(
                    query,
                    new ObservedCustomerPaymentPosition(
                            lastCreatedAt,
                            lastPaymentId
                    )
            );
        } catch (InvalidObservedCustomerCursorException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw invalid(
                    "Observed Customer payment cursor is malformed",
                    exception
            );
        }
    }

    @Override
    public ObservedCustomerCursor encodeSearch(
            ObservedCustomerSearchCriteria criteria,
            ObservedCustomerSearchPosition position
    ) {
        Objects.requireNonNull(criteria, "criteria is required");
        Objects.requireNonNull(position, "position is required");

        byte[] payload = writePayload(output -> {
            writeHeader(output, SEARCH_TYPE);
            output.writeUTF(criteria.sort().name());
            writeInstant(output, criteria.snapshotAt());
            writeInstant(output, position.lastSortValue());
            writeUuid(
                    output,
                    position.lastObservedCustomerId().value()
            );
            output.write(
                    searchFingerprint(criteria)
            );
        });

        return token(payload);
    }

    @Override
    public ObservedCustomerCursor encodePayments(
            ObservedCustomerPaymentCriteria criteria,
            ObservedCustomerPaymentPosition position
    ) {
        Objects.requireNonNull(criteria, "criteria is required");
        Objects.requireNonNull(position, "position is required");

        byte[] payload = writePayload(output -> {
            writeHeader(output, PAYMENT_TYPE);
            writeInstant(output, criteria.snapshotAt());
            writeInstant(
                    output,
                    position.lastPaymentCreatedAt()
            );
            writeUuid(output, position.lastPaymentId());
            writeUuid(
                    output,
                    criteria.observedCustomerId().value()
            );
            output.write(
                    paymentFingerprint(criteria)
            );
        });

        return token(payload);
    }

    private ObservedCustomerCursor token(byte[] payload) {
        String encodedPayload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload);

        String encodedSignature = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(sign(payload));

        return new ObservedCustomerCursor(
                encodedPayload + "." + encodedSignature
        );
    }

    private byte[] authenticate(
            ObservedCustomerCursor cursor
    ) {
        String[] parts = cursor.value().split("\\.", -1);

        if (parts.length != 2
                || parts[0].isBlank()
                || parts[1].isBlank()) {
            throw invalid(
                    "Observed Customer cursor format is invalid"
            );
        }

        try {
            byte[] payload = Base64.getUrlDecoder()
                    .decode(parts[0]);
            byte[] signature = Base64.getUrlDecoder()
                    .decode(parts[1]);

            if (!MessageDigest.isEqual(
                    sign(payload),
                    signature
            )) {
                throw invalid(
                        "Observed Customer cursor signature is invalid"
                );
            }

            return payload;
        } catch (IllegalArgumentException exception) {
            throw invalid(
                    "Observed Customer cursor encoding is invalid",
                    exception
            );
        }
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(
                    new SecretKeySpec(
                            key,
                            HMAC_ALGORITHM
                    )
            );
            return mac.doFinal(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Cannot initialize cursor authentication",
                    exception
            );
        }
    }

    private static ObservedCustomerSearchCriteria
            searchCriteria(
                    SearchObservedCustomersQuery query,
                    ObservedCustomerSearchPosition position
            ) {
        return new ObservedCustomerSearchCriteria(
                query.normalizedNiu(),
                query.legalName(),
                query.financialInstitutionCode(),
                query.lastPaymentStatus(),
                query.lastFailureReasonCode(),
                query.firstObservedFrom(),
                query.firstObservedTo(),
                query.lastObservedFrom(),
                query.lastObservedTo(),
                query.paymentFrom(),
                query.paymentTo(),
                query.sort(),
                query.size(),
                query.snapshotAt(),
                position
        );
    }

    private static ObservedCustomerPaymentCriteria
            paymentCriteria(
                    ListObservedCustomerPaymentsQuery query,
                    ObservedCustomerPaymentPosition position
            ) {
        return new ObservedCustomerPaymentCriteria(
                query.observedCustomerId(),
                query.status(),
                query.createdFrom(),
                query.createdTo(),
                query.size(),
                query.snapshotAt(),
                position
        );
    }

    private static byte[] searchFingerprint(
            SearchObservedCustomersQuery query
    ) {
        return fingerprint(output -> {
            writeNullableText(output, query.normalizedNiu());
            writeNullableText(output, query.legalName());
            writeNullableText(
                    output,
                    query.financialInstitutionCode()
            );
            writeNullableText(
                    output,
                    query.lastPaymentStatus() == null
                            ? null
                            : query.lastPaymentStatus().name()
            );
            writeNullableText(
                    output,
                    query.lastFailureReasonCode()
            );
            writeNullableInstant(
                    output,
                    query.firstObservedFrom()
            );
            writeNullableInstant(
                    output,
                    query.firstObservedTo()
            );
            writeNullableInstant(
                    output,
                    query.lastObservedFrom()
            );
            writeNullableInstant(
                    output,
                    query.lastObservedTo()
            );
            writeNullableInstant(output, query.paymentFrom());
            writeNullableInstant(output, query.paymentTo());
            output.writeUTF(query.sort().name());
            output.writeInt(query.size());
            writeInstant(output, query.snapshotAt());
        });
    }

    private static byte[] searchFingerprint(
            ObservedCustomerSearchCriteria criteria
    ) {
        return fingerprint(output -> {
            writeNullableText(
                    output,
                    criteria.normalizedNiu()
            );
            writeNullableText(output, criteria.legalName());
            writeNullableText(
                    output,
                    criteria.financialInstitutionCode()
            );
            writeNullableText(
                    output,
                    criteria.lastPaymentStatus() == null
                            ? null
                            : criteria.lastPaymentStatus().name()
            );
            writeNullableText(
                    output,
                    criteria.lastFailureReasonCode()
            );
            writeNullableInstant(
                    output,
                    criteria.firstObservedFrom()
            );
            writeNullableInstant(
                    output,
                    criteria.firstObservedTo()
            );
            writeNullableInstant(
                    output,
                    criteria.lastObservedFrom()
            );
            writeNullableInstant(
                    output,
                    criteria.lastObservedTo()
            );
            writeNullableInstant(
                    output,
                    criteria.paymentFrom()
            );
            writeNullableInstant(
                    output,
                    criteria.paymentTo()
            );
            output.writeUTF(criteria.sort().name());
            output.writeInt(criteria.size());
            writeInstant(output, criteria.snapshotAt());
        });
    }

    private static byte[] paymentFingerprint(
            ListObservedCustomerPaymentsQuery query
    ) {
        return fingerprint(output -> {
            writeUuid(
                    output,
                    query.observedCustomerId().value()
            );
            writeNullableText(
                    output,
                    query.status() == null
                            ? null
                            : query.status().name()
            );
            writeNullableInstant(output, query.createdFrom());
            writeNullableInstant(output, query.createdTo());
            output.writeInt(query.size());
            writeInstant(output, query.snapshotAt());
        });
    }

    private static byte[] paymentFingerprint(
            ObservedCustomerPaymentCriteria criteria
    ) {
        return fingerprint(output -> {
            writeUuid(
                    output,
                    criteria.observedCustomerId().value()
            );
            writeNullableText(
                    output,
                    criteria.status() == null
                            ? null
                            : criteria.status().name()
            );
            writeNullableInstant(
                    output,
                    criteria.createdFrom()
            );
            writeNullableInstant(
                    output,
                    criteria.createdTo()
            );
            output.writeInt(criteria.size());
            writeInstant(output, criteria.snapshotAt());
        });
    }

    private static byte[] fingerprint(
            IoWriter writer
    ) {
        byte[] canonical = writePayload(writer);

        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(canonical);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Cannot calculate cursor fingerprint",
                    exception
            );
        } finally {
            Arrays.fill(canonical, (byte) 0);
        }
    }

    private static byte[] writePayload(
            IoWriter writer
    ) {
        try {
            ByteArrayOutputStream buffer =
                    new ByteArrayOutputStream();

            try (DataOutputStream output =
                         new DataOutputStream(buffer)) {
                writer.write(output);
                output.flush();
            }

            return buffer.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot encode cursor payload",
                    exception
            );
        }
    }

    private static void writeHeader(
            DataOutputStream output,
            byte type
    ) throws IOException {
        output.writeInt(MAGIC);
        output.writeInt(SCHEMA_VERSION);
        output.writeByte(type);
    }

    private static void requireHeader(
            DataInputStream input,
            byte expectedType
    ) throws IOException {
        if (input.readInt() != MAGIC) {
            throw invalid("cursor magic is invalid");
        }

        int version = input.readInt();

        if (version != SCHEMA_VERSION) {
            throw invalid(
                    "cursor schema version is not supported"
            );
        }

        if (input.readByte() != expectedType) {
            throw invalid(
                    "cursor type does not match the request"
            );
        }
    }

    private static byte[] readFingerprint(
            DataInputStream input
    ) throws IOException {
        byte[] value = input.readNBytes(32);

        if (value.length != 32) {
            throw invalid(
                    "cursor query fingerprint is incomplete"
            );
        }

        return value;
    }

    private static void requireFingerprint(
            byte[] encoded,
            byte[] expected
    ) {
        if (!MessageDigest.isEqual(encoded, expected)) {
            throw invalid(
                    "cursor query does not match the request"
            );
        }
    }

    private static void requireFullyConsumed(
            DataInputStream input
    ) throws IOException {
        if (input.available() != 0) {
            throw invalid(
                    "cursor contains unexpected trailing data"
            );
        }
    }

    private static void writeInstant(
            DataOutputStream output,
            Instant value
    ) throws IOException {
        output.writeLong(value.getEpochSecond());
        output.writeInt(value.getNano());
    }

    private static Instant readInstant(
            DataInputStream input
    ) throws IOException {
        return Instant.ofEpochSecond(
                input.readLong(),
                input.readInt()
        );
    }

    private static void writeNullableInstant(
            DataOutputStream output,
            Instant value
    ) throws IOException {
        output.writeBoolean(value != null);

        if (value != null) {
            writeInstant(output, value);
        }
    }

    private static void writeNullableText(
            DataOutputStream output,
            String value
    ) throws IOException {
        output.writeBoolean(value != null);

        if (value != null) {
            byte[] bytes = value.getBytes(
                    StandardCharsets.UTF_8
            );
            output.writeInt(bytes.length);
            output.write(bytes);
        }
    }

    private static void writeUuid(
            DataOutputStream output,
            UUID value
    ) throws IOException {
        output.writeLong(value.getMostSignificantBits());
        output.writeLong(value.getLeastSignificantBits());
    }

    private static UUID readUuid(
            DataInputStream input
    ) throws IOException {
        return new UUID(
                input.readLong(),
                input.readLong()
        );
    }

    private static InvalidObservedCustomerCursorException
            invalid(String message) {
        return new InvalidObservedCustomerCursorException(
                message
        );
    }

    private static InvalidObservedCustomerCursorException
            invalid(
                    String message,
                    Throwable cause
            ) {
        return new InvalidObservedCustomerCursorException(
                message,
                cause
        );
    }

    @FunctionalInterface
    private interface IoWriter {

        void write(
                DataOutputStream output
        ) throws IOException;
    }
}
