package com.sixpay.payment.contracts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentPackagedOpenApiContractTest {

    private static final Path AUTHORITATIVE_CONTRACT =
            Path.of(
                    "..",
                    "..",
                    "documentation",
                    "contracts",
                    "internal",
                    "payment-query-api-v1.yaml"
            ).normalize();

    private static final Path PACKAGED_CONTRACT =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "openapi",
                    "payment-query-api-v1.yaml"
            );

    @Test
    void packagedContractMatchesAuthoritativeContract()
            throws Exception {

        assertTrue(
                Files.isRegularFile(AUTHORITATIVE_CONTRACT),
                "Authoritative Payment contract is missing"
        );

        assertTrue(
                Files.isRegularFile(PACKAGED_CONTRACT),
                "Packaged Payment contract is missing"
        );

        assertEquals(
                normalize(
                        Files.readString(
                                AUTHORITATIVE_CONTRACT
                        )
                ),
                normalize(
                        Files.readString(
                                PACKAGED_CONTRACT
                        )
                ),
                """
                The packaged Payment OpenAPI contract must exactly
                match the authoritative documentation contract
                """
        );
    }

    private static String normalize(String source) {
        return source
                .replace("\r\n", "\n")
                .strip();
    }
}