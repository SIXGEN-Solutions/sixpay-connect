package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentBriefValidationTest {

    private static final Path REPOSITORY_ROOT =
            Path.of("..", "..").normalize();

    private static final Path BRIEF =
            REPOSITORY_ROOT.resolve(
                    "documentation/ai/payment/"
                            + "PAYMENT_DOMAIN_GENERATION_BRIEF.md"
            );

    private static final Path GATE =
            REPOSITORY_ROOT.resolve(
                    "documentation/ai/payment/"
                            + "PAYMENT_IA1_GATE_VALIDATION.yaml"
            );

    private static final List<String> FORBIDDEN_MARKERS =
            List.of(
                    "T" + "ODO",
                    "T" + "BD",
                    "{" + "{",
                    "}" + "}",
                    "<" + "PLACEHOLDER" + ">",
                    "[" + "TO COMPLETE" + "]",
                    "CHANGE" + "ME",
                    "FIX" + "ME"
            );

    @Test
    void briefContainsExactlyTwentyOrderedSections()
            throws IOException {

        String content = Files.readString(BRIEF);

        Matcher matcher = Pattern.compile(
                "^## ([0-9]+)\\.",
                Pattern.MULTILINE
        ).matcher(content);

        List<Integer> actual = matcher.results()
                .map(result ->
                        Integer.parseInt(result.group(1))
                )
                .toList();

        List<Integer> expected = IntStream
                .rangeClosed(1, 20)
                .boxed()
                .toList();

        assertEquals(expected, actual);
    }

    @Test
    void briefContainsNoUnresolvedMarker()
            throws IOException {

        String content = Files.readString(BRIEF);

        for (String marker : FORBIDDEN_MARKERS) {
            assertFalse(
                    content.contains(marker),
                    () -> "Unresolved marker: " + marker
            );
        }
    }

    @Test
    void gateRemainsInReviewUntilApprovalsAreRecorded()
            throws IOException {

        Map<String, Object> gate = loadYaml(GATE);
        Map<String, Object> approvals =
                requiredMap(gate, "approvals");
        Map<String, Object> generation =
                requiredMap(gate, "generation");

        assertEquals(
                "NOT_RECORDED",
                approvals.get("product")
        );
        assertEquals(
                "NOT_RECORDED",
                approvals.get("architecture")
        );
        assertEquals(
                "NOT_RECORDED",
                approvals.get("engineering")
        );
        assertEquals(
                "IN_REVIEW",
                generation.get("gateStatus")
        );
        assertEquals(
                Boolean.FALSE,
                generation.get("gateApproved")
        );
        assertEquals(
                Boolean.FALSE,
                generation.get(
                        "globalCodeGenerationAllowed"
                )
        );
    }

    @Test
    void modelExitCriteriaPassWithoutFabricatingApprovals()
            throws IOException {

        Map<String, Object> gate = loadYaml(GATE);
        Map<String, Object> criteria =
                requiredMap(gate, "exitCriteria");

        for (String key : List.of(
                "allModelTypesDefined",
                "allTransitionsExplicit",
                "allInvariantsHaveControlMechanism",
                "sensitiveDataClassified",
                "domainResponsibilitiesExplicit",
                "eventsDefined",
                "idempotencyAndUnknownOutcomeRulesComplete",
                "persistencePlanDescribed",
                "testPlanDescribed",
                "briefHasNoPlaceholder"
        )) {
            assertEquals(Boolean.TRUE, criteria.get(key));
        }

        assertEquals(
                Boolean.FALSE,
                criteria.get("requiredApprovalsRecorded")
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(
            Path path
    ) throws IOException {

        try (Reader reader =
                     Files.newBufferedReader(path)) {
            return (Map<String, Object>)
                    new Yaml().load(reader);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requiredMap(
            Map<String, Object> parent,
            String key
    ) {
        Object value = parent.get(key);
        assertTrue(value instanceof Map<?, ?>);
        return (Map<String, Object>) value;
    }
}
