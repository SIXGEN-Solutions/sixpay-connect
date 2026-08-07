package com.sixpay.accounting.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountingArchitectureTest {

    private static final Path ROOT =
            Path.of(
                    "src/main/java/com/sixpay/accounting"
            );

    @Test
    void accountingDoesNotDependOnPaymentModule()
            throws Exception {
        String sources = readAllSources();

        assertFalse(
                sources.contains(
                        "import com.sixpay.payment."
                )
        );
    }

    @Test
    void lotFiveSixOneContainsNoTfjOrSftpImplementation()
            throws Exception {
        String sources = readAllSources()
                .toLowerCase();

        for (String forbidden : List.of(
                "sftp",
                "jsch",
                "tfjfile",
                "debitaccount",
                "creditaccount"
        )) {
            assertFalse(
                    sources.contains(forbidden),
                    () -> "Unexpected provider concern: "
                            + forbidden
            );
        }
    }

    @Test
    void domainRemainsFrameworkAgnostic()
            throws Exception {
        Path domain = ROOT.resolve("domain");

        try (var paths = Files.walk(domain)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path -> {
                        try {
                            String source =
                                    Files.readString(path);
                            return source.contains(
                                    "org.springframework."
                            ) || source.contains(
                                    "jakarta.persistence."
                            );
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Framework leaked into domain: "
                            + violations
            );
        }
    }

    private static String readAllSources()
            throws Exception {
        try (var paths = Files.walk(ROOT)) {
            return paths
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
                    .reduce("", String::concat);
        }
    }
}
