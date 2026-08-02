package com.sixpay.payment.contracts;

import com.sixpay.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentOpenApiContractValidationTest {

    /**
     * Maven and IntelliJ execute this module's tests from:
     *
     * sixpay-connect/backend/payment
     *
     * The repository documentation directory is therefore two levels above.
     */
    private static final Path CONTRACT = Path.of(
            "..",
            "..",
            "documentation",
            "contracts",
            "internal",
            "payment-query-api-v1.yaml"
    ).normalize();

    @Test
    void paymentQueryContractIsOpenApi31AndReadOnly()
            throws IOException {

        Map<String, Object> document = readContract();

        assertEquals(
                "3.1.0",
                document.get("openapi")
        );

        Map<String, Object> info =
                map(document, "info");

        Map<String, Object> metadata =
                map(info, "x-sixpay-contract");

        assertEquals(
                "payment",
                metadata.get("domain")
        );

        assertEquals(
                Boolean.TRUE,
                metadata.get("readOnly")
        );

        Map<String, Object> paths =
                map(document, "paths");

        assertFalse(paths.isEmpty());

        paths.forEach((path, rawOperations) -> {
            Map<String, Object> operations =
                    castMap(rawOperations);

            assertEquals(
                    Set.of("get"),
                    operations.keySet(),
                    "Payment Query API must remain read-only: "
                            + path
            );
        });
    }

    @Test
    void paymentStatusSchemaMatchesAuthoritativeDomain()
            throws IOException {

        Map<String, Object> document =
                readContract();

        Map<String, Object> components =
                map(document, "components");

        Map<String, Object> schemas =
                map(components, "schemas");

        Map<String, Object> statusSchema =
                map(schemas, "PaymentStatus");

        Object rawEnum =
                statusSchema.get("enum");

        assertNotNull(
                rawEnum,
                "PaymentStatus enum is missing"
        );

        @SuppressWarnings("unchecked")
        Set<String> contractStatuses =
                new LinkedHashSet<>(
                        (List<String>) rawEnum
                );

        Set<String> implementationStatuses =
                Arrays.stream(PaymentStatus.values())
                        .map(Enum::name)
                        .collect(
                                Collectors.toCollection(
                                        LinkedHashSet::new
                                )
                        );

        assertEquals(
                implementationStatuses,
                contractStatuses,
                """
                payment-query-api-v1.yaml PaymentStatus must exactly
                match the authoritative PaymentStatus enum
                """
        );
    }

    @Test
    void requiredPaymentQueryOperationsExist()
            throws IOException {

        Map<String, Object> paths =
                map(
                        readContract(),
                        "paths"
                );

        assertTrue(
                paths.containsKey(
                        "/internal/api/v1/payments"
                )
        );

        assertTrue(
                paths.containsKey(
                        "/internal/api/v1/payments/{paymentId}"
                )
        );

        assertEquals(
                "searchPayments",
                operationId(
                        paths,
                        "/internal/api/v1/payments"
                )
        );

        assertEquals(
                "getPayment",
                operationId(
                        paths,
                        "/internal/api/v1/payments/{paymentId}"
                )
        );
    }

    private static String operationId(
            Map<String, Object> paths,
            String path
    ) {
        Map<String, Object> operations =
                castMap(paths.get(path));

        Map<String, Object> get =
                castMap(operations.get("get"));

        return String.valueOf(
                get.get("operationId")
        );
    }

    private static Map<String, Object> readContract()
            throws IOException {

        Path absoluteContract =
                CONTRACT.toAbsolutePath().normalize();

        assertTrue(
                Files.isRegularFile(absoluteContract),
                () -> "Missing Payment OpenAPI contract: "
                        + absoluteContract
                        + System.lineSeparator()
                        + "Test working directory: "
                        + Path.of("")
                        .toAbsolutePath()
                        .normalize()
        );

        try (InputStream input =
                     Files.newInputStream(absoluteContract)) {

            Map<String, Object> document =
                    new Yaml().load(input);

            assertNotNull(
                    document,
                    "Payment OpenAPI contract is empty"
            );

            return document;
        }
    }

    private static Map<String, Object> map(
            Map<String, Object> source,
            String key
    ) {
        Object value =
                source.get(key);

        assertNotNull(
                value,
                "Missing YAML key: " + key
        );

        return castMap(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(
            Object value
    ) {
        return (Map<String, Object>) value;
    }
}