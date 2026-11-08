package com.sixpay.payment.api;

import com.sixpay.payment.infrastructure.recovery.PaymentT0ManualReconciliationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentT0ManualReconciliationControllerTest {
    @Test
    void controllerReusesInternalPaymentSurfaceAndRequiresReconcilePolicy() throws Exception {
        var type = PaymentT0ManualReconciliationController.class;
        assertEquals("/internal/api/v1/payments", type.getAnnotation(RequestMapping.class).value()[0]);
        var method = type.getMethod("reconcile", UUID.class,
                PaymentT0ManualReconciliationController.Request.class);
        assertEquals("/reconciliation", method.getAnnotation(PostMapping.class).value()[0]);
        assertEquals("@paymentAccessPolicy.canReconcile()",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void endpointDelegatesExactSelectionAndCorrelation() {
        var service = mock(PaymentT0ManualReconciliationService.class);
        var controller = new PaymentT0ManualReconciliationController(service);
        UUID id = UUID.fromString("11111111-1111-4111-8111-111111111111");
        UUID correlation = UUID.fromString("22222222-2222-4222-8222-222222222222");
        when(service.reconcile(List.of(id), correlation)).thenReturn(List.of(
                new PaymentT0ManualReconciliationService.Result(id, "RETRY_SCHEDULED",
                        "POSTING_OUTCOME_UNKNOWN")));
        var response = controller.reconcile(correlation,
                new PaymentT0ManualReconciliationController.Request(List.of(id)));
        assertEquals(200, response.getStatusCode().value());
        assertEquals("RETRY_SCHEDULED", response.getBody().results().getFirst().outcome());
        verify(service).reconcile(List.of(id), correlation);
    }
}
