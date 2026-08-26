package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerAuditPersistenceArchitectureTest {

    private static final Path AUDIT_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/"
                    + "infrastructure/audit"
    );

    private static final Path ADAPTER_ROOT =
            AUDIT_ROOT.resolve("adapter");

    private static final Path ENTITY_ROOT =
            AUDIT_ROOT.resolve("entity");

    private static final Path MAPPER_ROOT =
            AUDIT_ROOT.resolve("mapper");

    private static final Path REPOSITORY_ROOT =
            AUDIT_ROOT.resolve("repository");

    @Test
    void auditInfrastructureContainsTheApprovedTypes()
            throws Exception {

        assertEquals(
                Set.of(
                        "JpaObservedCustomerAuditAdapter.java",
                        "UuidObservedCustomerAuditIdGenerator.java",
                        "package-info.java"
                ),
                javaFiles(ADAPTER_ROOT)
        );

        assertEquals(
                Set.of(
                        "ObservedCustomerAuditJpaEntity.java",
                        "package-info.java"
                ),
                javaFiles(ENTITY_ROOT)
        );

        assertEquals(
                Set.of(
                        "ObservedCustomerAuditPersistenceMapper.java",
                        "package-info.java"
                ),
                javaFiles(MAPPER_ROOT)
        );

        assertEquals(
                Set.of(
                        "ObservedCustomerAuditSpringDataRepository.java",
                        "package-info.java"
                ),
                javaFiles(REPOSITORY_ROOT)
        );
    }

    @Test
    void auditInfrastructureDoesNotDependOnPaymentOrBanking()
            throws Exception {

        assertNoTokens(
                AUDIT_ROOT,
                List.of(
                        "import com.sixpay.payment.",
                        "Amplitude",
                        "amplitude",
                        "RestClient",
                        "WebClient",
                        "HttpClient"
                )
        );
    }

    @Test
    void auditEntityContainsNoSensitiveFields()
            throws Exception {

        String source = Files.readString(
                ENTITY_ROOT.resolve(
                        "ObservedCustomerAuditJpaEntity.java"
                )
        );

        for (String forbidden : List.of(
                "normalizedNiu",
                "legalName",
                "email",
                "phone",
                "accountNumber",
                "maskedAccountReference",
                "accountBindingFingerprint",
                "payload",
                "jwt",
                "apiKey",
                "cursor"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Sensitive audit entity field: "
                            + forbidden
            );
        }
    }

    @Test
    void auditRepositoryDoesNotExposeUpdateOrDeleteMethods()
            throws Exception {

        String source = Files.readString(
                REPOSITORY_ROOT.resolve(
                        "ObservedCustomerAuditSpringDataRepository.java"
                )
        );

        for (String forbidden : List.of(
                "deleteBy",
                "removeBy",
                "update ",
                "UPDATE ",
                "@Modifying",
                "deleteAll",
                "delete("
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden audit repository operation: "
                            + forbidden
            );
        }
    }

    @Test
    void adapterOnlyAppendsAuditRecords()
            throws Exception {

        String source = Files.readString(
                ADAPTER_ROOT.resolve(
                        "JpaObservedCustomerAuditAdapter.java"
                )
        );

        assertTrue(
                source.contains("void append("),
                "Audit adapter must expose append"
        );

        assertTrue(
                source.contains(
                        "repository.saveAndFlush(entity)"
                ),
                "Audit adapter must append and flush the audit row"
        );

        for (String forbidden : List.of(
                "repository.delete",
                "repository.deleteAll",
                "repository.findById",
                "repository.findAll",
                "entity.set",
                ".update("
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden audit adapter behavior: "
                            + forbidden
            );
        }
    }

    private static Set<String> javaFiles(
            Path root
    ) throws Exception {

        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .map(path ->
                            path.getFileName().toString()
                    )
                    .collect(Collectors.toSet());
        }
    }

    private static void assertNoTokens(
            Path root,
            List<String> forbidden
    ) throws Exception {

        try (Stream<Path> paths = Files.walk(root)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return forbidden.stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path
                                                    + " contains "
                                                    + token
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Audit persistence violations: "
                            + violations
            );
        }
    }
}