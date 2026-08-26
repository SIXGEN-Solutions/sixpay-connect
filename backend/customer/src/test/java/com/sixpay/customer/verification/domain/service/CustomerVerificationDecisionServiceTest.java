package com.sixpay.customer.verification.domain.service;

import com.sixpay.customer.verification.domain.model.*;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class CustomerVerificationDecisionServiceTest {
    @Test void delegatesToCanonicalPolicy() {
        var checks=Arrays.stream(VerificationCheckType.values()).map(VerificationCheck::passed).toList();
        assertEquals(VerificationOutcome.VERIFIED, new CustomerVerificationDecisionService().decide(checks));
    }
}
