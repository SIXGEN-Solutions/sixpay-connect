package com.sixpay.tests.assembled;

import com.sixpay.tests.support.CrossModulePostgreSqlTestSupport;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 8.3.6 — Pilot Critical Flow Matrix.
 *
 * <p>This is a cross-module assembly gate, not a replacement for module-level
 * golden tests. It verifies that the production composition root exposes only
 * the pilot flows that are currently approved and implemented.</p>
 *
 * <p>A flow that is still contractually TO_DEFINE must remain explicitly
 * unwired. The harness must never create a test-only adapter to hide such a
 * gap.</p>
 */
@SpringBootTest(
        classes = AssembledApplicationContextIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("assembled-test")
@EnabledIfSystemProperty(
        named = "sixpay.assembled.tests",
        matches = "true"
)
class PilotCriticalFlowMatrixIT
        extends CrossModulePostgreSqlTestSupport {

    private static final String CUSTOMER_VERIFICATION_PORT =
            "com.sixpay.payment.application.port.output.CustomerVerificationPort";

    private static final String OBSERVED_CUSTOMER_PROJECTION_PORT =
            "com.sixpay.payment.application.port.output.ObservedCustomerProjectionPort";

    private static final String OBSERVED_CUSTOMER_PROJECTION_SERVICE =
            "com.sixpay.payment.application.service.PaymentObservedCustomerProjectionService";

    private static final String ACCOUNTING_CANDIDATE_SOURCE =
            "com.sixpay.accounting.application.port.output.PaymentAccountingCandidateSource";

    private static final String REPORTING_AUDIT_READ_PORT =
            "com.sixpay.reporting.application.port.output.PaymentAuditReadPort";

    private static final String REPORTING_AUDIT_QUERY_SERVICE =
            "com.sixpay.reporting.application.service.PaymentAuditQueryService";

    private static final String NOTIFICATION_TRIGGER_USE_CASE =
            "com.sixpay.notification.application.port.input.OperationalNotificationTriggerUseCase";

    private static final String NOTIFICATION_ORCHESTRATION_USE_CASE =
            "com.sixpay.notification.application.port.input.OperationalNotificationOrchestrationUseCase";

    private static final String AUTHENTICATION_CAPABILITIES =
            "com.sixpay.security.configuration.AuthenticationCapabilitiesProperties";

    private static final List<FlowExpectation> PILOT_MATRIX = List.of(
            FlowExpectation.wired(
                    "Payment -> Customer Verification",
                    CUSTOMER_VERIFICATION_PORT,
                    "INT-03 is an implemented input-process module boundary"
            ),
            FlowExpectation.wired(
                    "Payment -> Observed Customer projection",
                    OBSERVED_CUSTOMER_PROJECTION_PORT,
                    "INT-10 projection boundary must participate input the assembled application"
            ),
            FlowExpectation.wired(
                    "Payment -> Observed Customer orchestration",
                    OBSERVED_CUSTOMER_PROJECTION_SERVICE,
                    "the assembled composition root must expose the projection service"
            ),
            FlowExpectation.explicitlyUnwired(
                    "Payment -> Accounting",
                    ACCOUNTING_CANDIDATE_SOURCE,
                    "INT-11 remains contract-dependent; the harness must not invent an adapter"
            ),
            FlowExpectation.wired(
                    "Reporting payment audit read model",
                    REPORTING_AUDIT_READ_PORT,
                    "reporting query capability must remain available"
            ),
            FlowExpectation.wired(
                    "Reporting payment audit query service",
                    REPORTING_AUDIT_QUERY_SERVICE,
                    "reporting application service must remain reachable"
            ),
            FlowExpectation.explicitlyUnwired(
                    "Payment -> Operational Notification trigger",
                    NOTIFICATION_TRIGGER_USE_CASE,
                    "INT-12 receiving-side orchestration remains conditional"
            ),
            FlowExpectation.explicitlyUnwired(
                    "Payment -> Operational Notification orchestration",
                    NOTIFICATION_ORCHESTRATION_USE_CASE,
                    "the harness must not manufacture a missing composition"
            )
    );

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @TestFactory
    Stream<DynamicTest> pilotCriticalFlowMatrixMatchesApprovedAssembly() {
        return PILOT_MATRIX.stream()
                .map(expectation ->
                        DynamicTest.dynamicTest(
                                expectation.flowName(),
                                () -> assertExpectation(expectation)
                        )
                );
    }

    @Test
    void securityBoundaryParticipatesWithoutChangingAuthenticationProfile() {
        assertThat(securityFilterChain)
                .as("assembled pilot matrix requires the real SIXPAY security chain")
                .isNotNull();

        assertBeanPresent(
                AUTHENTICATION_CAPABILITIES,
                "authentication capability configuration must be part of the assembled context"
        );
    }

    @Test
    void matrixContainsBothPositiveAndNegativeArchitectureGuards() {
        assertThat(PILOT_MATRIX)
                .anyMatch(flow -> flow.state() == ExpectedState.WIRED)
                .anyMatch(flow -> flow.state() == ExpectedState.EXPLICITLY_UNWIRED);
    }

    private void assertExpectation(FlowExpectation expectation) {
        Class<?> type = requiredType(expectation.typeName());
        Map<String, ?> beans = context.getBeansOfType(type);

        if (expectation.state() == ExpectedState.WIRED) {
            assertThat(beans)
                    .as(expectation.flowName() + " must be wired: " + expectation.reason())
                    .isNotEmpty();
            return;
        }

        assertThat(beans)
                .as(expectation.flowName() + " must remain unwired: " + expectation.reason())
                .isEmpty();
    }

    private void assertBeanPresent(String typeName, String reason) {
        Class<?> type = requiredType(typeName);
        assertThat(context.getBeansOfType(type))
                .as(reason)
                .isNotEmpty();
    }

    private static Class<?> requiredType(String typeName) {
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(
                    "Required pilot-matrix type is absent: " + typeName,
                    exception
            );
        }
    }

    private enum ExpectedState {
        WIRED,
        EXPLICITLY_UNWIRED
    }

    private record FlowExpectation(
            String flowName,
            String typeName,
            ExpectedState state,
            String reason
    ) {

        private static FlowExpectation wired(
                String flowName,
                String typeName,
                String reason
        ) {
            return new FlowExpectation(
                    flowName,
                    typeName,
                    ExpectedState.WIRED,
                    reason
            );
        }

        private static FlowExpectation explicitlyUnwired(
                String flowName,
                String typeName,
                String reason
        ) {
            return new FlowExpectation(
                    flowName,
                    typeName,
                    ExpectedState.EXPLICITLY_UNWIRED,
                    reason
            );
        }
    }
}
