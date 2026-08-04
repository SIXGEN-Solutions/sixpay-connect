package com.sixpay.customer.observation.domain.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObservedCustomerDomainArchitectureTest {

    private static final Path DOMAIN_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/domain"
    );

    @Test
    void domainRemainsFrameworkFreeAndCapabilityIndependent()
            throws Exception {

        try (var paths = Files.walk(DOMAIN_ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return List.of(
                                            "import org.springframework.",
                                            "import jakarta.",
                                            "import org.hibernate.",
                                            "import java.net.",
                                            "import java.sql.",
                                            "import com.sixpay.payment.",
                                            "import com.sixpay.customer.verification.",
                                            "RestClient",
                                            "WebClient",
                                            "HttpClient",
                                            "@Entity",
                                            "@Repository",
                                            "@Service",
                                            "@Component",
                                            "Instant.now(",
                                            "UUID.randomUUID("
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains " + token
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Observed Customer domain violations: "
                            + violations
            );
        }
    }

    @Test
    void accountModelContainsNoRawAccountConcept() throws Exception {
        String source = Files.readString(
                DOMAIN_ROOT.resolve(
                        "model/ObservedAccountReference.java"
                )
        );

        for (String forbidden : List.of(
                "accountNumber",
                "ribDebiteur",
                "rawAccount",
                "iban",
                "IntegrationAccountToken",
                "BankingAccountAccessReference"
        )) {
            assertFalse(source.contains(forbidden));
        }

        assertTrue(source.contains("accountBindingFingerprint"));
        assertTrue(source.contains("maskedValue"));
    }
}
