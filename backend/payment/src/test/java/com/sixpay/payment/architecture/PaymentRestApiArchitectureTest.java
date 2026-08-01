package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class PaymentRestApiArchitectureTest {
    private static final Path WEB_ROOT = Path.of("src/main/java/com/sixpay/payment/infrastructure/web");

    @Test
    void exposesOnlyContractedReadEndpoints() throws IOException {
        String c = Files.readString(WEB_ROOT.resolve("PaymentQueryController.java"));
        assertTrue(c.contains("@RequestMapping(\"/internal/api/v1/payments\")"));
        assertTrue(c.contains("@GetMapping"));
        for (String f : List.of("@PostMapping", "@PutMapping", "@PatchMapping", "@DeleteMapping")) assertFalse(c.contains(f));
    }

    @Test
    void restAdapterNeverLoadsAggregate() throws IOException {
        try (Stream<Path> paths = Files.walk(WEB_ROOT)) {
            List<String> violations = paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .flatMap(p -> {
                        try {
                            String s = Files.readString(p);
                            return List.of("domain.model.Payment;", "PaymentRepository", "PaymentJpaEntity", "PaymentStateDocument").stream()
                                    .filter(s::contains).map(t -> p + " contains " + t);
                        } catch (IOException e) { throw new IllegalStateException(e); }
                    }).toList();
            assertEquals(List.of(), violations);
        }
    }

    @Test
    void controllerRequiresReadScopeAndProjectionPort() throws IOException {
        String c = Files.readString(WEB_ROOT.resolve("PaymentQueryController.java"));
        assertTrue(c.contains("SCOPE_payment.read"));
        assertTrue(c.contains("@ConditionalOnBean(PaymentProjectionQueryUseCase.class)"));
    }
}
