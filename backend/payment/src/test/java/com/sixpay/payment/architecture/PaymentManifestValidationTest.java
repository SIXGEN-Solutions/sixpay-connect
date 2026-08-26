package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentManifestValidationTest {

    private static final Path REPOSITORY_ROOT =
            Path.of("..", "..").normalize();

    private static final Path MANIFEST_PATH =
            REPOSITORY_ROOT.resolve(
                    "documentation/ai/payment/"
                            + "AI_CONTEXT_MANIFEST.yaml"
            );

    @Test
    void authoritativeContextIsDeclared()
            throws IOException {

        Map<String, Object> context =
                requiredMap(loadManifest(), "context");

        assertEquals(
                "payment",
                requiredString(context, "domain")
        );
        assertEquals(
                "feat/payment-domain-generation-brief",
                requiredString(context, "branch")
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(context, "normative")
        );
        assertFalse(
                requiredString(context, "status").isBlank()
        );
    }

    @Test
    void globalGenerationRemainsDisabled()
            throws IOException {

        Map<String, Object> manifest = loadManifest();
        Map<String, Object> authorization =
                authorization(manifest);

        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        manifest,
                        "codeGenerationAllowed"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        authorization,
                        "globalCodeGenerationAllowed"
                )
        );
    }

    @Test
    void activeIncrementIsGenericAndExplicit()
            throws IOException {

        Map<String, Object> authorization =
                authorization(loadManifest());

        assertEquals(
                "ACTIVE",
                requiredString(authorization, "status")
        );
        assertEquals(
                "PAYMENT_DOMAIN_ONLY",
                requiredString(authorization, "scope")
        );

        String currentIncrement =
                requiredString(
                        authorization,
                        "currentIncrement"
                );

        assertTrue(
                currentIncrement.startsWith("LOT_"),
                () -> "Invalid increment: "
                        + currentIncrement
        );

        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        authorization,
                        "currentIncrementCodeGenerationAllowed"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        authorization,
                        "futureIncrementActivationRequired"
                )
        );
    }

    @Test
    void authorizationListsAreNonEmptyAndNonContradictory()
            throws IOException {

        Map<String, Object> authorization =
                authorization(loadManifest());

        List<String> programPaths =
                requiredStringList(
                        authorization,
                        "programAllowedPaths"
                );
        List<String> incrementPaths =
                requiredStringList(
                        authorization,
                        "currentIncrementAllowedPaths"
                );
        List<String> allowed =
                requiredStringList(
                        authorization,
                        "allowedChangeKinds"
                );
        List<String> forbidden =
                requiredStringList(
                        authorization,
                        "forbiddenChangeKinds"
                );

        assertFalse(programPaths.isEmpty());
        assertFalse(incrementPaths.isEmpty());
        assertFalse(allowed.isEmpty());
        assertFalse(forbidden.isEmpty());

        assertTrue(
                programPaths.contains(
                        "backend/payment/src/main/java/"
                                + "com/sixpay/payment/domain/**"
                )
        );
        assertTrue(
                programPaths.contains(
                        "backend/payment/src/test/java/"
                                + "com/sixpay/payment/architecture/**"
                )
        );

        Set<String> overlap = new HashSet<>(allowed);
        overlap.retainAll(forbidden);

        assertTrue(
                overlap.isEmpty(),
                () -> "Allowed/forbidden overlap: "
                        + overlap
        );
    }

    @Test
    void implementedCapabilitiesAreNotMarkedDeferred()
            throws IOException {

        Map<String, Object> constraints =
                constraints(loadManifest());

        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "snapshotsDeferred"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "aggregateRootDeferred"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "policiesDeferred"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "domainEventsDeferred"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        constraints,
                        "aggregateMutationAllowed"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        constraints,
                        "eventRegistrationAllowed"
                )
        );
    }

    @Test
    void stableDomainConstraintsAreConsistent()
            throws IOException {

        Map<String, Object> constraints =
                constraints(loadManifest());

        assertEquals(
                21,
                requiredInteger(constraints, "javaVersion")
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        constraints,
                        "frameworkFreeDomain"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "anotherBusinessDomainDependencyAllowed"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "databaseOrNetworkAccessAllowed"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "systemClockAccessAllowed"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(constraints, "ioAllowed")
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "repositoryAccessAllowed"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "externalClientAccessAllowed"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        constraints,
                        "aggregateRootImplemented"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        constraints,
                        "immutablePaymentStateImplemented"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        constraints,
                        "aggregateOwnsMutation"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        constraints,
                        "aggregateOwnsEventRegistration"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        constraints,
                        "oneVersionIncrementPerSuccessfulMutation"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        constraints,
                        "eventSequenceOneBasedPerMutation"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "reconstitutionCreatesEvents"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "noOpCreatesEvents"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "invalidTransitionCreatesEvents"
                )
        );
    }

    @Test
    void eventAuditAndOutboxBoundariesAreSafe()
            throws IOException {

        Map<String, Object> constraints =
                constraints(loadManifest());

        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        constraints,
                        "auditRequiredForEveryDomainEvent"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        constraints,
                        "transactionalOutboxRequiredForPublishableEvents"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "directKafkaFromAggregateAllowed"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "directDomainEventPublicationAllowed"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        constraints,
                        "explicitIntegrationEventMappingRequired"
                )
        );
        assertEquals(
                "eventId",
                requiredString(
                        constraints,
                        "eventDeduplicationKey"
                )
        );
        assertEquals(
                "aggregateVersion,eventSequence",
                requiredString(
                        constraints,
                        "eventOrdering"
                )
        );
        assertEquals(
                "AT_LEAST_ONCE",
                requiredString(
                        constraints,
                        "outboxDeliveryGuarantee"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "kafkaFailureRollsBackPayment"
                )
        );
        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        constraints,
                        "kafkaFailureReplaysFinancialOperation"
                )
        );
    }

    @Test
    void frozenModelCountsRemainStable()
            throws IOException {

        Map<String, Object> counts =
                requiredMap(
                        loadManifest(),
                        "modelCounts"
                );

        assertEquals(
                1,
                requiredInteger(counts, "aggregateRoots")
        );
        assertEquals(
                17,
                requiredInteger(counts, "states")
        );
        assertEquals(
                4,
                requiredInteger(counts, "terminalStates")
        );
        assertEquals(
                16,
                requiredInteger(counts, "commands")
        );
        assertEquals(
                17,
                requiredInteger(
                        counts,
                        "aggregateOperations"
                )
        );
        assertEquals(
                38,
                requiredInteger(counts, "transitions")
        );
        assertEquals(
                76,
                requiredInteger(counts, "invariants")
        );
        assertEquals(
                33,
                requiredInteger(counts, "events")
        );
        assertEquals(
                14,
                requiredInteger(counts, "policies")
        );
        assertEquals(
                4,
                requiredInteger(counts, "domainServices")
        );
        assertEquals(
                12,
                requiredInteger(counts, "policyProfiles")
        );
    }

    @Test
    void readinessIsIndependentFromCurrentIncrement()
            throws IOException {

        Map<String, Object> readiness =
                requiredMap(
                        loadManifest(),
                        "readiness"
                );

        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        readiness,
                        "domainModelComplete"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        readiness,
                        "domainModelFrozen"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        readiness,
                        "paymentDomainOnlyImplementationComplete"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        readiness,
                        "paymentDomainKernelValidated"
                )
        );
        assertEquals(
                Boolean.TRUE,
                requiredBoolean(
                        readiness,
                        "nextIncrementRequiresExplicitActivation"
                )
        );
        assertEquals(
                "FORBIDDEN_PENDING_EXTERNAL_APPROVALS",
                requiredString(
                        readiness,
                        "globalCodeGeneration"
                )
        );
    }

    @Test
    void primaryDocumentsExist()
            throws IOException {

        List<Map<String, Object>> documents =
                requiredObjectList(
                        loadManifest(),
                        "primaryDocuments"
                );

        assertFalse(documents.isEmpty());

        List<String> missing =
                documents.stream()
                        .map(document ->
                                requiredString(
                                        document,
                                        "path"
                                )
                        )
                        .filter(path ->
                                !Files.isRegularFile(
                                        REPOSITORY_ROOT.resolve(path)
                                )
                        )
                        .toList();

        assertTrue(
                missing.isEmpty(),
                () -> "Missing documents: " + missing
        );
    }

    @Test
    void generationFreezeAndTraceabilityRemainExplicit()
            throws IOException {

        Map<String, Object> manifest = loadManifest();
        Map<String, Object> rules =
                requiredMap(manifest, "generationRules");
        Map<String, Object> traceability =
                requiredMap(manifest, "traceability");

        assertEquals(
                "IA1_MODEL_FROZEN",
                requiredString(rules, "modelFreeze")
        );

        List<String> forbidden =
                requiredStringList(
                        rules,
                        "forbiddenUntilExplicitApproval"
                );

        assertTrue(
                forbidden.contains(
                        "DATABASE_SCHEMA_OR_MIGRATION_GENERATION"
                )
        );
        assertTrue(
                forbidden.contains("OPENAPI_MODIFICATION")
        );
        assertTrue(
                forbidden.contains(
                        "ADAPTER_OR_CONTROLLER_GENERATION"
                )
        );

        List<String> prefixes =
                requiredStringList(
                        traceability,
                        "requiredPrefixes"
                );

        assertTrue(prefixes.contains("PAY-INV-"));
        assertTrue(prefixes.contains("PAY-CMD-"));
        assertTrue(prefixes.contains("PAY-TR-"));
        assertTrue(prefixes.contains("PAY-EVT-"));
        assertTrue(prefixes.contains("PAY-POL-"));
        assertTrue(prefixes.contains("PAY-DS-"));
        assertTrue(prefixes.contains("PAY-DEC-"));

        assertEquals(
                Boolean.FALSE,
                requiredBoolean(
                        traceability,
                        "untracedRuleAllowed"
                )
        );
    }

    private static Map<String, Object> loadManifest()
            throws IOException {

        assertTrue(
                Files.isRegularFile(MANIFEST_PATH),
                () -> "Missing manifest: " + MANIFEST_PATH
        );

        try (Reader reader =
                     Files.newBufferedReader(MANIFEST_PATH)) {

            Object loaded = new Yaml().load(reader);

            assertNotNull(loaded);

            return castMap(loaded, "manifest root");
        }
    }

    private static Map<String, Object> authorization(
            Map<String, Object> manifest
    ) {
        return requiredMap(
                manifest,
                "implementationAuthorization"
        );
    }

    private static Map<String, Object> constraints(
            Map<String, Object> manifest
    ) {
        return requiredMap(
                authorization(manifest),
                "constraints"
        );
    }

    private static Map<String, Object> requiredMap(
            Map<String, Object> parent,
            String key
    ) {
        Object value = parent.get(key);

        assertNotNull(
                value,
                () -> "Missing YAML map: " + key
        );

        return castMap(value, key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(
            Object value,
            String description
    ) {
        assertTrue(
                value instanceof Map<?, ?>,
                () -> description + " must be a map"
        );

        return (Map<String, Object>) value;
    }

    private static List<Map<String, Object>>
            requiredObjectList(
                    Map<String, Object> parent,
                    String key
            ) {

        Object value = parent.get(key);

        assertTrue(
                value instanceof List<?>,
                () -> key + " must be a list"
        );

        return ((List<?>) value).stream()
                .map(item ->
                        castMap(item, key + " item")
                )
                .toList();
    }

    private static List<String> requiredStringList(
            Map<String, Object> parent,
            String key
    ) {
        Object value = parent.get(key);

        assertTrue(
                value instanceof List<?>,
                () -> key + " must be a list"
        );

        return ((List<?>) value).stream()
                .map(item -> {
                    assertTrue(
                            item instanceof String,
                            () -> key
                                    + " must contain strings"
                    );
                    return (String) item;
                })
                .toList();
    }

    private static String requiredString(
            Map<String, Object> parent,
            String key
    ) {
        Object value = parent.get(key);

        assertTrue(
                value instanceof String,
                () -> key + " must be a string"
        );

        String text = (String) value;

        assertFalse(
                text.isBlank(),
                () -> key + " must not be blank"
        );

        return text;
    }

    private static Boolean requiredBoolean(
            Map<String, Object> parent,
            String key
    ) {
        Object value = parent.get(key);

        assertTrue(
                value instanceof Boolean,
                () -> key + " must be a boolean"
        );

        return (Boolean) value;
    }

    private static int requiredInteger(
            Map<String, Object> parent,
            String key
    ) {
        Object value = parent.get(key);

        assertTrue(
                value instanceof Number,
                () -> key + " must be numeric"
        );

        return ((Number) value).intValue();
    }
}
