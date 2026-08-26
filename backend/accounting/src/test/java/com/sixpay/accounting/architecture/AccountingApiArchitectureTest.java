package com.sixpay.accounting.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountingApiArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/accounting/"
                    + "infrastructure/accountingapi"
    );

    @Test
    void submissionUsesConfiguredIdempotencyHeader()
            throws Exception {
        String source = normalize(
                Files.readString(
                        ROOT.resolve(
                                "client/RestAccountingBatchClient.java"
                        )
                )
        );

        assertTrue(
                source.contains(
                        "properties.contract()"
                                + ".idempotencyHeader()"
                )
        );
        assertTrue(
                source.contains(
                        "batch.idempotencyKey().value()"
                )
        );
    }

    @Test
    void submissionNeverRetriesUnknownSideEffect()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "client/RestAccountingBatchClient.java"
                )
        );

        for (String forbidden : List.of(
                "RetryTemplate",
                "@Retryable",
                "RetryingIntegrationExecutor",
                "while (",
                "for (int attempt"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden retry mechanism: "
                            + forbidden
            );
        }

        assertTrue(
                source.contains(
                        "AccountingSubmissionOutcomeUnknownException"
                )
        );
        assertTrue(
                normalize(source).contains(
                        "status==429||status>=500"
                )
        );
    }

    @Test
    void accountingApiContainsNoTfjOrSftpImplementation()
            throws Exception {
        String source;

        try (var paths = Files.walk(ROOT)) {
            source = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .reduce("", String::concat)
                    .toLowerCase();
        }

        for (String forbidden : List.of(
                "jsch",
                "sftpclient",
                "debitaccount",
                "creditaccount",
                "tfjfile"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Unexpected provider implementation: "
                            + forbidden
            );
        }
    }

    private static String normalize(
            String source
    ) {
        return source
                .replace(" ", "")
                .replace("\t", "")
                .replace("\r", "")
                .replace("\n", "");
    }
}
