package com.sixpay.payment.architecture;

import com.sixpay.payment.domain.model.PaymentSource;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPartnerNeutralityArchitectureTest {

    private static final Path DOMAIN =
            Path.of("src/main/java/com/sixpay/payment/domain");

    @Test
    void paymentSourceAcceptsOpaqueProviderNeutralIdentifiers() {
        assertThat(PaymentSource.of("PARTNER_A").value()).isEqualTo("PARTNER_A");
        assertThat(PaymentSource.of("PARTNER_B").value()).isEqualTo("PARTNER_B");
    }

    @Test
    void paymentDomainContainsNoPartnerSemantics() throws Exception {
        try (Stream<Path> paths = Files.walk(DOMAIN)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java")).toList()) {
                String content = Files.readString(path);
                assertThat(path.getFileName().toString())
                        .as("provider-specific type name in %s", path)
                        .doesNotContain("Partner").doesNotContain("tresor" + "pay");
                assertThat(content)
                        .as("provider-specific semantics in %s", path)
                        .doesNotContain("Partner").doesNotContain("tresor" + "pay")
                        .doesNotContain("PARTNER").doesNotContain("PARTNER");
            }
        }
    }
}
