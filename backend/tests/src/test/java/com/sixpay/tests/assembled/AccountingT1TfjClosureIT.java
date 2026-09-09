package com.sixpay.tests.assembled;

import com.sixpay.tests.support.CrossModulePostgreSqlTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ACCOUNTING_T1 closure topology gate.
 *
 * <p>Functional TFJ invariants are exercised in the owning Accounting and
 * Payment modules. This assembled test proves that the provider-neutral
 * internal transport, Accounting TFJ ingestion/finality publication and the
 * Payment TFJ listener participate in the same modular-monolith runtime.</p>
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
class AccountingT1TfjClosureIT
        extends CrossModulePostgreSqlTestSupport {

    @Autowired
    private ApplicationContext context;

    @Test
    void accountingTfjAndPaymentFinalityAreAssembledThroughInternalTransport() {
        assertBeanPresent(
                "com.sixpay.common.messaging.transport.IntegrationEventTransport"
        );
        assertBeanPresent(
                "com.sixpay.accounting.application.service.TfjIngestionService"
        );
        assertBeanPresent(
                "com.sixpay.accounting.application.service.TfjFinalityPublicationService"
        );
        assertBeanPresent(
                "com.sixpay.payment.infrastructure.messaging.internal."
                        + "PaymentTfjFinalityIntegrationEventListener"
        );
    }

    private void assertBeanPresent(String typeName) {
        Class<?> type = requiredType(typeName);
        assertThat(context.getBeansOfType(type))
                .as(typeName + " must participate in the assembled T1 runtime")
                .isNotEmpty();
    }

    private static Class<?> requiredType(String typeName) {
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(
                    "Required ACCOUNTING_T1 type is absent: " + typeName,
                    exception
            );
        }
    }
}
