package com.sixpay.payment.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentPersistenceArchitectureTest {

    private static final Path DOMAIN_ROOT =
            Path.of(
                    "src/main/java/com/sixpay/payment/domain"
            );

    private static final Path PERSISTENCE_ROOT =
            Path.of(
                    "src/main/java/com/sixpay/payment/"
                            + "infrastructure/persistence"
            );

    private static final Path MIGRATION =
            Path.of(
                    "src/main/resources/db/migration/"
                            + "V300__payment_baseline.sql"
            );

    @Test
    void domainRepositoryRemainsFrameworkFree()
            throws IOException {

        String repository = Files.readString(
                DOMAIN_ROOT.resolve(
                        "repository/PaymentRepository.java"
                )
        );

        assertFalse(
                repository.contains("org.springframework")
        );

        assertFalse(
                repository.contains("jakarta.persistence")
        );

        assertFalse(
                repository.contains("PaymentJpaEntity")
        );
    }

    @Test
    void persistenceMayImplementOnlyApplicationOutboundPorts()
            throws IOException {

        List<String> forbiddenApplicationDependencies =
                List.of(
                        "com.sixpay.payment.application.command",
                        "com.sixpay.payment.application.query",
                        "com.sixpay.payment.application.view",
                        "com.sixpay.payment.application.port.in",
                        "com.sixpay.payment.application.service"
                );

        List<String> violations;

        try (Stream<Path> paths =
                     Files.walk(PERSISTENCE_ROOT)) {

            violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return forbiddenApplicationDependencies
                                    .stream()
                                    .filter(source::contains)
                                    .map(forbidden ->
                                            path
                                                    + " depends on "
                                                    + forbidden
                                    );

                        } catch (IOException exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();
        }

        assertEquals(
                List.of(),
                violations,
                "Persistence may depend only on "
                        + "application.port.out"
        );
    }

    @Test
    void persistenceAdaptersImplementExpectedOutboundPorts()
            throws IOException {

        String repositoryAdapter =
                Files.readString(
                        PERSISTENCE_ROOT.resolve(
                                "PaymentRepositoryAdapter.java"
                        )
                );

        String atomicPersistenceAdapter =
                Files.readString(
                        PERSISTENCE_ROOT.resolve(
                                "PaymentAtomicPersistenceAdapter.java"
                        )
                );

        assertTrue(
                repositoryAdapter.contains(
                        "PaymentLookupPort"
                ),
                "PaymentRepositoryAdapter must implement "
                        + "PaymentLookupPort"
        );

        assertTrue(
                atomicPersistenceAdapter.contains(
                        "PaymentAtomicPersistencePort"
                ),
                "PaymentAtomicPersistenceAdapter must implement "
                        + "PaymentAtomicPersistencePort"
        );

        assertFalse(
                repositoryAdapter.contains(
                        "com.sixpay.payment.application.service"
                )
        );

        assertFalse(
                atomicPersistenceAdapter.contains(
                        "com.sixpay.payment.application.service"
                )
        );
    }

    @Test
    void persistenceContainsNoApplicationService()
            throws IOException {

        try (Stream<Path> paths =
                     Files.walk(PERSISTENCE_ROOT)) {

            List<Path> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return source.contains(
                                    "class PaymentApplicationService"
                            ) || source.contains(
                                    "@Service"
                            );

                        } catch (IOException exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();

            assertEquals(
                    List.of(),
                    violations,
                    "Persistence must not contain "
                            + "application services"
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

        assertTrue(
                entity.contains("businessVersion")
        );

        assertTrue(
                entity.contains("@Version")
        );

        assertTrue(
                entity.contains("persistenceVersion")
        );

        assertTrue(
                entity.contains("PaymentState")
        );

        assertFalse(
                entity.contains("extends Payment")
        );
    }

    @Test
    void mapperReconstitutesThroughDomainFactory()
            throws IOException {

        String mapper = Files.readString(
                PERSISTENCE_ROOT.resolve(
                        "PaymentPersistenceMapper.java"
                )
        );

        assertTrue(
                mapper.contains(
                        "Payment.reconstitute(state)"
                )
        );

        assertTrue(
                mapper.contains(
                        "PaymentStateDocument"
                )
        );

        assertFalse(
                mapper.contains(
                        "Payment.receive("
                )
        );
    }

    @Test
    void persistencePackageContainsOnlyAuthorizedTypes()
            throws IOException {

        Set<String> authorizedFiles = Set.of(
                "PaymentAtomicPersistenceAdapter.java",
                "PaymentJpaEntity.java",
                "PaymentPersistenceException.java",
                "PaymentPersistenceMapper.java",
                "PaymentRepositoryAdapter.java",
                "PaymentSpringDataRepository.java",
                "PaymentStateDocument.java",
                "package-info.java"
        );

        List<String> actualFiles;

        try (Stream<Path> paths =
                     Files.list(PERSISTENCE_ROOT)) {

            actualFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .map(path ->
                            path.getFileName().toString()
                    )
                    .sorted()
                    .toList();
        }

        assertEquals(
                authorizedFiles.stream()
                        .sorted()
                        .toList(),
                actualFiles,
                "Unexpected type in Payment persistence package"
        );
    }

    @Test
    void migrationProtectsIdentityVersionAndJsonPayload()
            throws IOException {

        String sql = Files.readString(MIGRATION);

        assertTrue(
                sql.contains(
                        "PRIMARY KEY (payment_id)"
                )
        );

        assertTrue(
                sql.contains(
                        "UNIQUE "
                                + "(payment_source, "
                                + "external_payment_reference)"
                )
        );

        assertTrue(
                sql.contains(
                        "business_version > 0"
                )
        );

        assertTrue(
                sql.contains(
                        "persistence_version"
                )
        );

        assertTrue(
                sql.contains(
                        "state_payload"
                )
        );

        assertTrue(
                sql.contains(
                        "jsonb_typeof(state_payload)"
                )
        );
    }
}