package com.sixpay.tests.assembled;

import com.sixpay.SixpayApplication;
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
 * <p>INT-11 Payment -> Accounting remains PLANNED / TO_DEFINE in the
 * integration architecture. Existing Accounting-owned candidate projection
 * infrastructure does not imply approval of the cross-module contract.</p>
 */
@SpringBootTest(
        classes = SixpayApplication.class,
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
            "com.sixpay.payment.application.port.input.PaymentProjectionQueryUseCase";
    private static final String ACCOUNTING_BATCH_BUILDER =
            "com.sixpay.accounting.application.service.AccountingBatchBuilder";
    private static final String ACCOUNTING_CANDIDATE_SOURCE =
            "com.sixpay.accounting.application.port.output.PaymentAccountingCandidateSource";
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
    void accountingCandidateSourceFoundationIsAvailable() {
        assertTypeAvailable(ACCOUNTING_CANDIDATE_SOURCE);
        assertBeanPresent(ACCOUNTING_CANDIDATE_SOURCE);
    }

    @Test
    void reportingReadModelRemainsAvailableIndependentlyOfInt11() {
        assertBeanPresent(REPORTING_READ_PORT);
        assertBeanPresent(REPORTING_QUERY_SERVICE);
    }

    private void assertBeanPresent(String typeName) {
        Class<?> type = requiredType(typeName);
        Map<String, ?> beans = context.getBeansOfType(type);
        assertThat(beans)
                .as(typeName + " must participate input the assembled context")
                .isNotEmpty();
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
