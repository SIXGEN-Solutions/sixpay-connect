package com.sixpay.payment.domain.validation;

import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentDomainKernelCatalogueTest {

    private static final Path REPOSITORY_ROOT =
            Path.of("..", "..").normalize();

    private static final Set<String> EXPECTED_OPERATIONS = Set.of(
                "receive",
                "startAuthorizationChecking",
                "recordAuthorizationDecision",
                "recordBankingVerification",
                "recordFundsControl",
                "recordTreasuryAccountResolution",
                "authorizePosting",
                "recordPostingOutcome",
                "resolvePostingOutcome",
                "recordMatchedEndOfDayConfirmation",
                "authorizeReversal",
                "recordReversalOutcome",
                "resolveReversalOutcome",
                "reject",
                "recordRecoverableFailure",
                "failWithoutFinancialEffect",
                "reconstitute"
    );

    private static final List<String> EXPECTED_TRANSITIONS = List.of(
                "PAY-TR-001",
                "PAY-TR-002",
                "PAY-TR-003",
                "PAY-TR-004",
                "PAY-TR-005",
                "PAY-TR-006",
                "PAY-TR-007",
                "PAY-TR-008",
                "PAY-TR-009",
                "PAY-TR-010",
                "PAY-TR-011",
                "PAY-TR-012",
                "PAY-TR-013",
                "PAY-TR-014",
                "PAY-TR-015",
                "PAY-TR-016",
                "PAY-TR-017",
                "PAY-TR-018",
                "PAY-TR-019",
                "PAY-TR-020",
                "PAY-TR-021",
                "PAY-TR-022",
                "PAY-TR-023",
                "PAY-TR-024",
                "PAY-TR-025",
                "PAY-TR-026",
                "PAY-TR-027",
                "PAY-TR-028",
                "PAY-TR-029",
                "PAY-TR-030",
                "PAY-TR-031",
                "PAY-TR-032",
                "PAY-TR-033",
                "PAY-TR-034",
                "PAY-TR-035",
                "PAY-TR-036",
                "PAY-TR-037",
                "PAY-TR-038"
    );

    private static final List<String> EXPECTED_INVARIANTS = List.of(
                "PAY-INV-001",
                "PAY-INV-002",
                "PAY-INV-003",
                "PAY-INV-004",
                "PAY-INV-005",
                "PAY-INV-006",
                "PAY-INV-007",
                "PAY-INV-008",
                "PAY-INV-009",
                "PAY-INV-010",
                "PAY-INV-011",
                "PAY-INV-012",
                "PAY-INV-013",
                "PAY-INV-014",
                "PAY-INV-015",
                "PAY-INV-016",
                "PAY-INV-017",
                "PAY-INV-018",
                "PAY-INV-019",
                "PAY-INV-020",
                "PAY-INV-021",
                "PAY-INV-022",
                "PAY-INV-023",
                "PAY-INV-024",
                "PAY-INV-025",
                "PAY-INV-026",
                "PAY-INV-027",
                "PAY-INV-028",
                "PAY-INV-029",
                "PAY-INV-030",
                "PAY-INV-031",
                "PAY-INV-032",
                "PAY-INV-033",
                "PAY-INV-034",
                "PAY-INV-035",
                "PAY-INV-036",
                "PAY-INV-037",
                "PAY-INV-038",
                "PAY-INV-039",
                "PAY-INV-040",
                "PAY-INV-041",
                "PAY-INV-042",
                "PAY-INV-043",
                "PAY-INV-044",
                "PAY-INV-045",
                "PAY-INV-046",
                "PAY-INV-047",
                "PAY-INV-048",
                "PAY-INV-049",
                "PAY-INV-050",
                "PAY-INV-051",
                "PAY-INV-052",
                "PAY-INV-053",
                "PAY-INV-054",
                "PAY-INV-055",
                "PAY-INV-056",
                "PAY-INV-057",
                "PAY-INV-058",
                "PAY-INV-059",
                "PAY-INV-060",
                "PAY-INV-061",
                "PAY-INV-062",
                "PAY-INV-063",
                "PAY-INV-064",
                "PAY-INV-065",
                "PAY-INV-066",
                "PAY-INV-067",
                "PAY-INV-068",
                "PAY-INV-069",
                "PAY-INV-070",
                "PAY-INV-071",
                "PAY-INV-072",
                "PAY-INV-073",
                "PAY-INV-074",
                "PAY-INV-075",
                "PAY-INV-076"
    );

    private static final List<String> EXPECTED_EVENTS = List.of(
                "PAY-EVT-001",
                "PAY-EVT-002",
                "PAY-EVT-003",
                "PAY-EVT-004",
                "PAY-EVT-005",
                "PAY-EVT-006",
                "PAY-EVT-007",
                "PAY-EVT-008",
                "PAY-EVT-009",
                "PAY-EVT-010",
                "PAY-EVT-011",
                "PAY-EVT-012",
                "PAY-EVT-013",
                "PAY-EVT-014",
                "PAY-EVT-015",
                "PAY-EVT-016",
                "PAY-EVT-017",
                "PAY-EVT-018",
                "PAY-EVT-019",
                "PAY-EVT-020",
                "PAY-EVT-021",
                "PAY-EVT-022",
                "PAY-EVT-023",
                "PAY-EVT-024",
                "PAY-EVT-025",
                "PAY-EVT-026",
                "PAY-EVT-027",
                "PAY-EVT-028",
                "PAY-EVT-029",
                "PAY-EVT-030",
                "PAY-EVT-031",
                "PAY-EVT-032",
                "PAY-EVT-033"
    );

    @Test
    void kernelContainsEighteenStatesAndFourTerminalStates() {
        assertEquals(18, PaymentStatus.values().length);
        assertEquals(
                Set.of(
                        PaymentStatus.REJECTED,
                        PaymentStatus.FAILED,
                        PaymentStatus.TREASURY_INTEGRATED,
                        PaymentStatus.REVERSED
                ),
                Arrays.stream(PaymentStatus.values())
                        .filter(PaymentStatus::isTerminal)
                        .collect(Collectors.toSet())
        );
    }

    @Test
    void aggregateExposesAllSeventeenNamedOperations() {
        Set<String> operations = Arrays.stream(
                        Payment.class.getDeclaredMethods()
                )
                .filter(method ->
                        java.lang.reflect.Modifier.isPublic(
                                method.getModifiers()
                        )
                )
                .map(Method::getName)
                .filter(EXPECTED_OPERATIONS::contains)
                .collect(Collectors.toSet());

        assertEquals(EXPECTED_OPERATIONS, operations);
    }

    @Test
    void normativeCataloguesExposeAllRequiredPayIdentifiers()
            throws IOException {
        String stateMachine = Files.readString(
                REPOSITORY_ROOT.resolve(
                        "documentation/ai/payment/PAYMENT_STATE_MACHINE.yaml"
                )
        );
        String invariantCatalogue = Files.readString(
                REPOSITORY_ROOT.resolve(
                        "documentation/ai/payment/PAYMENT_INVARIANT_CATALOGUE.yaml"
                )
        );
        String eventCatalogue = Files.readString(
                REPOSITORY_ROOT.resolve(
                        "documentation/ai/payment/PAYMENT_EVENT_CATALOG.yaml"
                )
        );

        EXPECTED_TRANSITIONS.forEach(id ->
                assertTrue(stateMachine.contains("id: " + id))
        );
        EXPECTED_INVARIANTS.forEach(id ->
                assertTrue(invariantCatalogue.contains("id: " + id))
        );
        EXPECTED_EVENTS.forEach(id ->
                assertTrue(eventCatalogue.contains("id: " + id))
        );

        assertEquals(38, EXPECTED_TRANSITIONS.size());
        assertEquals(76, EXPECTED_INVARIANTS.size());
        assertEquals(33, EXPECTED_EVENTS.size());
    }

    @Test
    void terminalStatesHaveNoOutgoingTransitionInNormativeMachine()
            throws IOException {
        String yaml = Files.readString(
                REPOSITORY_ROOT.resolve(
                        "documentation/ai/payment/PAYMENT_STATE_MACHINE.yaml"
                )
        );

        for (PaymentStatus terminal : Set.of(
                PaymentStatus.REJECTED,
                PaymentStatus.FAILED,
                PaymentStatus.TREASURY_INTEGRATED,
                PaymentStatus.REVERSED
        )) {
            assertFalse(
                    yaml.matches(
                            "(?s).*from:\\s*\\n\\s*- "
                                    + terminal.name()
                                    + ".*"
                    ),
                    () -> terminal + " must have no outgoing transition"
            );
        }
    }
}
