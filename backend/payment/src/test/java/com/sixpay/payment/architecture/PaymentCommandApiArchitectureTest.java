package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCommandApiArchitectureTest {

    private static final Path API_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/api"
    );

    @Test
    void commandControllerUsesGoldenModulePackageAndContractPath()
            throws Exception {
        String source = Files.readString(
                API_ROOT.resolve(
                        "PaymentCommandController.java"
                )
        );

        assertTrue(source.contains(
                "package com.sixpay.payment.api;"
        ));
        assertTrue(source.contains(
                "@RequestMapping(\"/v1/payments\")"
        ));
        assertTrue(source.contains(
                "@PostMapping(\"/initiate\")"
        ));
        assertTrue(source.contains(
                "SCOPE_payment.initiate"
        ));
        assertTrue(source.contains(
                "PaymentInitiationUseCase"
        ));
    }

    @Test
    void commandApiDoesNotLoadAggregateOrPersistence()
            throws Exception {
        String controller = Files.readString(
                API_ROOT.resolve(
                        "PaymentCommandController.java"
                )
        );
        String mapper = Files.readString(
                API_ROOT.resolve(
                        "PaymentCommandApiMapper.java"
                )
        );

        for (String forbidden : new String[]{
                "domain.model.Payment;",
                "PaymentRepository",
                "PaymentJpaEntity",
                "PaymentStateDocument",
                "infrastructure."
        }) {
            assertFalse(controller.contains(forbidden));
            assertFalse(mapper.contains(forbidden));
        }
    }
}
