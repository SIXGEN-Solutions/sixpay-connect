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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentTraceabilityValidationTest {

    private static final Path REPOSITORY_ROOT =
            Path.of("..", "..").normalize();

    private static final Path TRACEABILITY_PATH =
            REPOSITORY_ROOT.resolve(
                    "documentation/ai/payment/"
                            + "PAYMENT_TEST_TRACEABILITY.yaml"
            );

    private static final Path INVARIANT_PATH =
            REPOSITORY_ROOT.resolve(
                    "documentation/ai/payment/"
                            + "PAYMENT_INVARIANT_CATALOGUE.yaml"
            );

    private static final Path STATE_MACHINE_PATH =
            REPOSITORY_ROOT.resolve(
                    "documentation/ai/payment/"
                            + "PAYMENT_STATE_MACHINE.yaml"
            );

    private static final Path EVENT_PATH =
            REPOSITORY_ROOT.resolve(
                    "documentation/ai/payment/"
                            + "PAYMENT_EVENT_CATALOG.yaml"
            );

    private static final Path COMMAND_PATH =
            REPOSITORY_ROOT.resolve(
                    "documentation/ai/payment/"
                            + "PAYMENT_COMMAND_CATALOGUE.yaml"
            );

    @Test
    void allSeventySixInvariantsHaveNamedScenarios()
            throws IOException {

        Map<String, Object> traceability =
                loadYaml(TRACEABILITY_PATH);
        Map<String, Object> catalogue =
                loadYaml(INVARIANT_PATH);

        List<Map<String, Object>> coverage =
                objectList(
                        traceability,
                        "invariantCoverage"
                );
        List<Map<String, Object>> invariants =
                objectList(
                        catalogue,
                        "invariants"
                );

        Set<String> expected =
                ids(invariants, "id");
        Set<String> actual =
                ids(coverage, "invariantId");

        assertEquals(76, expected.size());
        assertEquals(expected, actual);

        for (Map<String, Object> row : coverage) {
            assertNonBlank(row, "scenarioId");
            assertNonBlank(row, "scenarioName");
            assertNonBlank(row, "testStatus");
            assertNonBlank(row, "testFile");
            assertNonBlank(row, "futureFile");
            assertNonBlank(row, "stableErrorCode");
        }
    }

    @Test
    void allThirtyEightTransitionsHaveCompleteTraceability()
            throws IOException {

        Map<String, Object> traceability =
                loadYaml(TRACEABILITY_PATH);
        Map<String, Object> stateMachine =
                loadYaml(STATE_MACHINE_PATH);

        List<Map<String, Object>> coverage =
                objectList(
                        traceability,
                        "transitionCoverage"
                );
        List<Map<String, Object>> transitions =
                objectList(
                        stateMachine,
                        "transitions"
                );

        Set<String> expected =
                ids(transitions, "id");
        Set<String> actual =
                ids(coverage, "transitionId");

        assertEquals(38, expected.size());
        assertEquals(expected, actual);

        for (Map<String, Object> row : coverage) {
            assertNonBlank(row, "scenarioId");
            assertNonBlank(row, "scenarioName");
            assertNonBlank(row, "operationId");
            assertNonBlank(row, "operation");
            assertNonBlank(row, "testFile");
            assertNonBlank(row, "futureFile");

            List<String> invariantRefs =
                    stringList(row, "invariantRefs");
            List<String> eventRefs =
                    stringList(row, "eventRefs");

            assertFalse(
                    invariantRefs.isEmpty(),
                    () -> row.get("transitionId")
                            + " has no invariant"
            );
            assertFalse(
                    eventRefs.isEmpty(),
                    () -> row.get("transitionId")
                            + " has no event"
            );
        }
    }

    @Test
    void traceabilityReferencesOnlyNormativeIdentifiers()
            throws IOException {

        Map<String, Object> traceability =
                loadYaml(TRACEABILITY_PATH);
        Map<String, Object> invariantCatalogue =
                loadYaml(INVARIANT_PATH);
        Map<String, Object> stateMachine =
                loadYaml(STATE_MACHINE_PATH);
        Map<String, Object> eventCatalogue =
                loadYaml(EVENT_PATH);
        Map<String, Object> commandCatalogue =
                loadYaml(COMMAND_PATH);

        Set<String> invariantIds =
                ids(
                        objectList(
                                invariantCatalogue,
                                "invariants"
                        ),
                        "id"
                );
        Set<String> transitionIds =
                ids(
                        objectList(
                                stateMachine,
                                "transitions"
                        ),
                        "id"
                );
        Set<String> eventIds =
                ids(
                        objectList(
                                eventCatalogue,
                                "events"
                        ),
                        "id"
                );
        Set<String> operationIds =
                ids(
                        objectList(
                                commandCatalogue,
                                "operations"
                        ),
                        "id"
                );

        for (Map<String, Object> row :
                objectList(
                        traceability,
                        "transitionCoverage"
                )) {

            assertTrue(
                    transitionIds.contains(
                            requiredString(
                                    row,
                                    "transitionId"
                            )
                    )
            );

            assertTrue(
                    invariantIds.containsAll(
                            stringList(
                                    row,
                                    "invariantRefs"
                            )
                    )
            );

            assertTrue(
                    eventIds.containsAll(
                            stringList(
                                    row,
                                    "eventRefs"
                            )
                    )
            );

            assertTrue(
                    operationIds.contains(
                            requiredString(
                                    row,
                                    "operationId"
                            )
                    )
            );
        }

        for (Map<String, Object> row :
                objectList(
                        traceability,
                        "invariantCoverage"
                )) {

            assertTrue(
                    invariantIds.contains(
                            requiredString(
                                    row,
                                    "invariantId"
                            )
                    )
            );

            Set<String> transitionRefs =
                    new HashSet<>(
                            stringList(
                                    row,
                                    "transitionRefs"
                            )
                    );
            transitionRefs.remove("PAY-CREATION");

            assertTrue(
                    transitionIds.containsAll(
                            transitionRefs
                    )
            );

            assertTrue(
                    eventIds.containsAll(
                            stringList(
                                    row,
                                    "eventRefs"
                            )
                    )
            );

            assertTrue(
                    operationIds.containsAll(
                            stringList(
                                    row,
                                    "operationRefs"
                            )
                    )
            );
        }
    }

    @Test
    void futureTestsAreClearlyMarkedAndHaveTargetFiles()
            throws IOException {

        Map<String, Object> traceability =
                loadYaml(TRACEABILITY_PATH);

        List<Map<String, Object>> futureTests =
                objectList(
                        traceability,
                        "futureApplicationPersistenceArchitectureScenarios"
                );

        assertFalse(futureTests.isEmpty());

        for (Map<String, Object> scenario : futureTests) {
            assertNonBlank(scenario, "id");
            assertNonBlank(scenario, "name");
            assertNonBlank(scenario, "layer");
            assertNonBlank(scenario, "futureFile");
        }

        for (Map<String, Object> row :
                objectList(
                        traceability,
                        "invariantCoverage"
                )) {

            String status =
                    requiredString(row, "testStatus");

            if ("FUTURE_VERTICAL_TEST".equals(status)) {
                assertTrue(
                        requiredString(
                                row,
                                "futureFile"
                        ).contains(
                                "/application/"
                        )
                                || requiredString(
                                row,
                                "futureFile"
                        ).contains(
                                "/infrastructure/"
                        ),
                        () -> row.get("invariantId")
                                + " future test has invalid target"
                );
            }
        }
    }

    private static Map<String, Object> loadYaml(
            Path path
    ) throws IOException {

        assertTrue(
                Files.isRegularFile(path),
                () -> "Missing YAML file: " + path
        );

        try (Reader reader =
                     Files.newBufferedReader(path)) {

            Object loaded = new Yaml().load(reader);

            assertNotNull(loaded);

            return castMap(loaded, path.toString());
        }
    }

    private static Set<String> ids(
            List<Map<String, Object>> rows,
            String key
    ) {
        return rows.stream()
                .map(row ->
                        requiredString(row, key)
                )
                .collect(Collectors.toSet());
    }

    private static void assertNonBlank(
            Map<String, Object> row,
            String key
    ) {
        assertFalse(
                requiredString(row, key).isBlank(),
                () -> key + " must not be blank"
        );
    }

    private static String requiredString(
            Map<String, Object> row,
            String key
    ) {
        Object value = row.get(key);

        assertTrue(
                value instanceof String,
                () -> key + " must be a string"
        );

        return (String) value;
    }

    private static List<String> stringList(
            Map<String, Object> row,
            String key
    ) {
        Object value = row.get(key);

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

    private static List<Map<String, Object>>
            objectList(
                    Map<String, Object> root,
                    String key
            ) {

        Object value = root.get(key);

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
}
