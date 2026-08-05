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

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/"
                    + "infrastructure/audit"
    );

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/"
                    + "V20260805.01__create_"
                    + "customer_observation_audit.sql"
    );

    @Test
    void auditInfrastructureContainsTheApprovedTypes()
            throws Exception {
        assertEquals(
                Set.of(
                        "ObservedCustomerAuditJpaEntity.java",
                        "package-info.java"
                ),
                javaFiles(ROOT.resolve("entity"))
        );

        assertEquals(
                Set.of(
                        "ObservedCustomerAuditSpringDataRepository.java",
                        "package-info.java"
                ),
                javaFiles(ROOT.resolve("repository"))
        );

        assertEquals(
                Set.of(
                        "JpaObservedCustomerAuditAdapter.java",
                        "package-info.java"
                ),
                javaFiles(ROOT.resolve("adapter"))
        );

        assertEquals(
                Set.of(
                        "ObservedCustomerAuditPersistenceMapper.java",
                        "package-info.java"
                ),
                javaFiles(ROOT.resolve("mapper"))
        );
    }

    @Test
    void entityIsImmutableFlatAndHasNoMutationMethods()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "entity/ObservedCustomerAuditJpaEntity.java"
                )
        );

        assertTrue(source.contains("@Immutable"));
        assertTrue(source.contains(
                "@Table("
        ));
        assertTrue(source.contains(
                "name = \"customer_observation_audit\""
        ));

        for (String forbidden : List.of(
                "@ManyToOne",
                "@OneToMany",
                "@OneToOne",
                "@JoinColumn",
                "setAudit",
                "setAction",
                "setOutcome",
                "setObservedCustomer",
                "setPayment",
                "setSourceEvent",
                "setActor",
                "setCorrelation",
                "setReason",
                "setOccurred",
                "setAuditVersion"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Mutable or relational audit entity: "
                            + forbidden
            );
        }
    }

    @Test
    void adapterOnlyAppendsAndNeverDeletesOrUpdates()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "adapter/"
                                + "JpaObservedCustomerAuditAdapter.java"
                )
        );

        assertTrue(source.contains(
                "repository.saveAndFlush(entity)"
        ));

        for (String forbidden : List.of(
                "repository.delete",
                "repository.deleteAll",
                "repository.saveAll",
                "entity.set",
                "update(",
                "remove("
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Non append-only adapter operation: "
                            + forbidden
            );
        }
    }

    @Test
    void migrationHasIndexesNoForeignKeyAndMutationGuards()
            throws Exception {
        String sql = Files.readString(MIGRATION);

        for (String required : List.of(
                "CREATE TABLE customer_observation_audit",
                "audit_id UUID PRIMARY KEY",
                "observed_customer_id UUID NULL",
                "source_event_id UUID NULL",
                "correlation_id VARCHAR(150) NOT NULL",
                "occurred_at TIMESTAMPTZ NOT NULL",
                "audit_version INTEGER NOT NULL",
                "idx_customer_observation_audit_customer",
                "idx_customer_observation_audit_source_event",
                "idx_customer_observation_audit_correlation",
                "idx_customer_observation_audit_occurred",
                "BEFORE UPDATE",
                "BEFORE DELETE",
                "append-only"
        )) {
            assertTrue(
                    sql.contains(required),
                    () -> "Missing audit migration rule: "
                            + required
            );
        }

        for (String forbidden : List.of(
                "FOREIGN KEY",
                "REFERENCES customer_observed_customer",
                "ON DELETE CASCADE",
                "details ",
                "message ",
                "payload ",
                "account_number",
                "account_binding_fingerprint"
        )) {
            assertFalse(
                    sql.contains(forbidden),
                    () -> "Forbidden audit migration concept: "
                            + forbidden
            );
        }
    }

    @Test
    void persistenceMapperUsesExplicitAuditSchemaVersion()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "mapper/"
                                + "ObservedCustomerAuditPersistenceMapper.java"
                )
        );

        assertTrue(source.contains(
                "CURRENT_AUDIT_VERSION = 1"
        ));
        assertTrue(source.contains(
                "ObservedCustomerAuditJpaEntity.create("
        ));
        assertFalse(source.contains("exception.getMessage()"));
        assertFalse(source.contains("Throwable"));
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
}
