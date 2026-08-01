package com.sixpay.payment.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentPersistenceArchitectureTest {

    private static final Path DOMAIN_ROOT =
            Path.of("src/main/java/com/sixpay/payment/domain");
    private static final Path PERSISTENCE_ROOT =
            Path.of(
                    "src/main/java/com/sixpay/payment/"
                            + "infrastructure/persistence"
            );
    private static final Path MIGRATION =
            Path.of(
                    "src/main/resources/db/migration/"
                            + "V2026080101__create_payment_persistence.sql"
            );

    @Test
    void domainRepositoryRemainsFrameworkFree()
            throws IOException {
        String repository = Files.readString(
                DOMAIN_ROOT.resolve(
                        "repository/PaymentRepository.java"
                )
        );

        assertFalse(repository.contains("org.springframework"));
        assertFalse(repository.contains("jakarta.persistence"));
        assertFalse(repository.contains("PaymentJpaEntity"));
    }

    @Test
    void persistenceLayerDoesNotContainApplicationServices()
            throws IOException {
        try (var paths = Files.walk(PERSISTENCE_ROOT)) {
            List<Path> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            String source = Files.readString(path);
                            return source.contains(
                                    "com.sixpay.payment.application"
                            ) || source.contains(
                                    "class PaymentApplicationService"
                            );
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Persistence depends on application: "
                            + violations
            );
        }
    }

    @Test
    void entityUsesSeparateDomainAndPersistenceVersions()
            throws IOException {
        String entity = Files.readString(
                PERSISTENCE_ROOT.resolve(
                        "PaymentJpaEntity.java"
                )
        );

        assertTrue(entity.contains("businessVersion"));
        assertTrue(entity.contains("@Version"));
        assertTrue(entity.contains("persistenceVersion"));
        assertTrue(entity.contains("PaymentState"));
        assertFalse(entity.contains("extends Payment"));
    }

    @Test
    void mapperReconstitutesThroughDomainFactory()
            throws IOException {
        String mapper = Files.readString(
                PERSISTENCE_ROOT.resolve(
                        "PaymentPersistenceMapper.java"
                )
        );

        assertTrue(mapper.contains("Payment.reconstitute(state)"));
        assertTrue(mapper.contains("PaymentStateDocument"));
        assertFalse(mapper.contains("Payment.receive("));
    }

    @Test
    void migrationProtectsIdentityVersionAndJsonPayload()
            throws IOException {
        String sql = Files.readString(MIGRATION);

        assertTrue(sql.contains("PRIMARY KEY (payment_id)"));
        assertTrue(sql.contains(
                "UNIQUE (payment_source, external_payment_reference)"
        ));
        assertTrue(sql.contains("business_version > 0"));
        assertTrue(sql.contains("persistence_version"));
        assertTrue(sql.contains("state_payload"));
        assertTrue(sql.contains("jsonb_typeof(state_payload)"));
    }
}
