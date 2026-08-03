package com.sixpay.customer.verification.domain.compatibility;

import com.sixpay.customer.verification.domain.model.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class PaymentVerificationTaxonomyCompatibilityTest {
    @Test void checkTypesRemainAlignedWithPaymentSource() throws Exception {
        Path source=Path.of("../payment/src/main/java/com/sixpay/payment/domain/model/evidence/BankingVerificationCheckType.java");
        assertTrue(Files.isRegularFile(source));
        String content=Files.readString(source);
        for (var type: VerificationCheckType.values()) assertTrue(content.contains(type.name()), () -> "Payment taxonomy missing " + type.name());
        assertEquals(11, VerificationCheckType.values().length);
    }
    @Test void resultAndOutcomeNamesRemainCanonical() {
        assertEquals(Set.of("PASS","FAIL","UNKNOWN"), Arrays.stream(VerificationCheckResult.values()).map(Enum::name).collect(Collectors.toSet()));
        assertEquals(Set.of("VERIFIED","REJECTED","INDETERMINATE"), Arrays.stream(VerificationOutcome.values()).map(Enum::name).collect(Collectors.toSet()));
    }
}
