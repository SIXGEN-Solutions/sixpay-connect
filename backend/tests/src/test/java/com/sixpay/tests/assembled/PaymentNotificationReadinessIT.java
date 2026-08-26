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

@SpringBootTest(
        classes = AssembledApplicationContextIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("assembled-test")
@EnabledIfSystemProperty(
        named = "sixpay.assembled.tests",
        matches = "true"
)
class PaymentNotificationReadinessIT
        extends CrossModulePostgreSqlTestSupport {

    private static final String PAYMENT_FINALIZATION_SERVICE =
            "com.sixpay.payment.application.service.PaymentFinalizationService";
    private static final String PAYMENT_POSTING_OUTCOME_EVENT =
            "com.sixpay.payment.domain.event.PaymentPostingOutcomeRecorded";
    private static final String PAYMENT_POSTED_NOTIFICATION_TRIGGER =
            "com.sixpay.notification.domain.model.PaymentPostedNotificationTrigger";
    private static final String NOTIFICATION_TEMPLATE_CATALOG =
            "com.sixpay.notification.domain.policy.OperationalNotificationTemplateCatalog";
    private static final String NOTIFICATION_TRIGGER_USE_CASE =
            "com.sixpay.notification.application.port.input.OperationalNotificationTriggerUseCase";
    private static final String NOTIFICATION_ORCHESTRATION_USE_CASE =
            "com.sixpay.notification.application.port.input.OperationalNotificationOrchestrationUseCase";
    private static final String BOOTSTRAP_NOTIFICATION_PACKAGE =
            "com.sixpay.bootstrap.integration.notification";

    @Autowired
    private ApplicationContext context;

    @Test
    void paymentAndNotificationParticipateInAssembledApplication() {
        assertBeanPresent(PAYMENT_FINALIZATION_SERVICE);
        assertBeanPresent(NOTIFICATION_TEMPLATE_CATALOG);
        assertTypeAvailable(PAYMENT_POSTING_OUTCOME_EVENT);
        assertTypeAvailable(PAYMENT_POSTED_NOTIFICATION_TRIGGER);
    }

    @Test
    void notificationReceivingSideRemainsConditionalInAssembledBaseline() {
        assertBeanAbsent(
                NOTIFICATION_TRIGGER_USE_CASE,
                "Operational notification planning must remain conditional"
        );
        assertBeanAbsent(
                NOTIFICATION_ORCHESTRATION_USE_CASE,
                "Operational notification orchestration must remain conditional"
        );
    }

    @Test
    void paymentToNotificationCompositionIsNotInventedByTestHarness() {
        Map<String, Object> beans = context.getBeansOfType(Object.class);

        assertThat(beans.values())
                .noneMatch(bean -> bean.getClass()
                        .getName()
                        .startsWith(BOOTSTRAP_NOTIFICATION_PACKAGE));
    }

    private void assertBeanPresent(String typeName) {
        Class<?> type = requiredType(typeName);
        assertThat(context.getBeansOfType(type)).isNotEmpty();
    }

    private void assertBeanAbsent(String typeName, String reason) {
        Class<?> type = requiredType(typeName);
        assertThat(context.getBeansOfType(type)).as(reason).isEmpty();
    }

    private void assertTypeAvailable(String typeName) {
        assertThat(requiredType(typeName)).isNotNull();
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
