package com.sixpay.tests.assembled;

import com.sixpay.tests.support.CrossModulePostgreSqlTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 8.3.3 — Payment / Accounting / Reporting cross-module readiness gate.
 *
 * <p>INT-11 Payment -> Accounting is still PLANNED / TO_DEFINE in the
 * authoritative integration architecture. This test therefore proves the
 * current assembled topology without inventing an unapproved production
 * contract.</p>
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
class PaymentAccountingReportingReadinessIT
        extends CrossModulePostgreSqlTestSupport {

    private static final String PAYMENT_QUERY_USE_CASE =
            "com.sixpay.payment.application.port.in.PaymentProjectionQueryUseCase";
    private static final String ACCOUNTING_BATCH_BUILDER =
            "com.sixpay.accounting.application.service.AccountingBatchBuilder";
    private static final String ACCOUNTING_CANDIDATE_SOURCE =
            "com.sixpay.accounting.application.port.output.PaymentAccountingCandidateSource";
    private static final String ACCOUNTING_CONSTITUTION_SERVICE =
            "com.sixpay.accounting.application.service.AccountingBatchConstitutionService";
    private static final String REPORTING_READ_PORT =
            "com.sixpay.reporting.application.port.output.PaymentAuditReadPort";
    private static final String REPORTING_QUERY_SERVICE =
            "com.sixpay.reporting.application.service.PaymentAuditQueryService";

    @Autowired
    private ApplicationContext context;

    @Test
    void paymentAccountingAndReportingParticipateInAssembledApplication() {
        assertBeanPresent(PAYMENT_QUERY_USE_CASE);
        assertBeanPresent(ACCOUNTING_BATCH_BUILDER);
        assertBeanPresent(REPORTING_READ_PORT);
        assertBeanPresent(REPORTING_QUERY_SERVICE);
    }

    @Test
    void paymentToAccountingRemainsUnwiredUntilInt11ContractExists() {
        assertTypeAvailable(ACCOUNTING_CANDIDATE_SOURCE);

        assertBeanAbsent(
                ACCOUNTING_CANDIDATE_SOURCE,
                "INT-11 is PLANNED / TO_DEFINE; no production "
                        + "PaymentAccountingCandidateSource must be wired "
                        + "before its contract is approved"
        );

        assertBeanAbsent(
                ACCOUNTING_CONSTITUTION_SERVICE,
                "AccountingBatchConstitutionService is intentionally "
                        + "conditional on PaymentAccountingCandidateSource"
        );
    }

    @Test
    void reportingReadModelRemainsAvailableIndependentlyOfInt11() {
        assertBeanPresent(REPORTING_READ_PORT);
        assertBeanPresent(REPORTING_QUERY_SERVICE);
        assertBeanAbsent(
                ACCOUNTING_CANDIDATE_SOURCE,
                "Reporting availability must not depend on an invented "
                        + "Payment -> Accounting implementation"
        );
    }

    private void assertBeanPresent(String typeName) {
        Class<?> type = requiredType(typeName);
        Map<String, ?> beans = context.getBeansOfType(type);
        assertThat(beans)
                .as(typeName + " must participate in the assembled context")
                .isNotEmpty();
    }

    private void assertBeanAbsent(String typeName, String reason) {
        Class<?> type = requiredType(typeName);
        Map<String, ?> beans = context.getBeansOfType(type);
        assertThat(beans).as(reason).isEmpty();
    }

    private void assertTypeAvailable(String typeName) {
        assertThat(requiredType(typeName))
                .as(typeName + " must exist as an explicit module boundary")
                .isNotNull();
    }

    private static Class<?> requiredType(String typeName) {
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(
                    "Required assembled type is absent: " + typeName,
                    exception
            );
        }
    }
}
