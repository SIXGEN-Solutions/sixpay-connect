package com.sixpay.customer.verification.domain.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerVerificationAggregateArchitectureTest {

    private static final Path DOMAIN_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/verification/domain"
    );

    @Test
    void aggregateUsesNamedOperationsAndNoGenericSetters()
            throws Exception {

        String source = Files.readString(
                DOMAIN_ROOT.resolve("model/CustomerVerification.java")
        );

        assertTrue(source.contains(
                "static CustomerVerification request("
        ));
        assertTrue(source.contains(
                "static CustomerVerification reconstitute("
        ));
        assertTrue(source.contains(
                "CustomerVerificationResult complete("
        ));

        for (String forbidden : List.of(
                "setStatus(",
                "setOutcome(",
                "transitionTo(",
                "Instant.now(",
                "UUID.randomUUID("
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Aggregate contains forbidden operation: "
                            + forbidden
            );
        }
    }

    @Test
    void onlyOneFinalDomainEventContractIsIntroduced()
            throws Exception {

        Path eventRoot = DOMAIN_ROOT.resolve("event");

        try (var paths = Files.list(eventRoot)) {
            List<String> eventFiles = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".java"))
                    .filter(name -> !name.equals("package-info.java"))
                    .toList();

            assertEqualsIgnoringOrder(
                    List.of(
                            "CustomerVerificationDomainEvent.java",
                            "CustomerVerificationCompleted.java"
                    ),
                    eventFiles
            );
        }

        assertFalse(Files.exists(
                eventRoot.resolve(
                        "CustomerVerificationRejected.java"
                )
        ));
        assertFalse(Files.exists(
                eventRoot.resolve(
                        "CustomerVerificationIndeterminate.java"
                )
        ));
    }

    private static void assertEqualsIgnoringOrder(
            List<String> expected,
            List<String> actual
    ) {
        assertTrue(
                expected.size() == actual.size()
                        && actual.containsAll(expected),
                () -> "Expected " + expected + " but found " + actual
        );
    }
}
