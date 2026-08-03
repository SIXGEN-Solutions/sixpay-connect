package com.sixpay.customer.verification.domain.policy;

import com.sixpay.customer.verification.domain.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VerificationOutcomePolicyTest {
    private static List<VerificationCheck> allPassed() { return Arrays.stream(VerificationCheckType.values()).map(VerificationCheck::passed).toList(); }
    private static void replace(List<VerificationCheck> list, VerificationCheck replacement) { list.set(replacement.type().ordinal(), replacement); }
    @Test void allPassIsVerified() { assertEquals(VerificationOutcome.VERIFIED, VerificationOutcomePolicy.determine(allPassed())); }
    @Test void failIsRejected() { var c=new ArrayList<>(allPassed()); replace(c, VerificationCheck.failed(VerificationCheckType.ACCOUNT_NOT_BLOCKED, VerificationFailureCode.ACCOUNT_BLOCKED)); assertEquals(VerificationOutcome.REJECTED, VerificationOutcomePolicy.determine(c)); }
    @Test void unknownIsIndeterminate() { var c=new ArrayList<>(allPassed()); replace(c, VerificationCheck.unknown(VerificationCheckType.ACCOUNT_EXISTS, VerificationFailureCode.BANKING_RESPONSE_TIMEOUT)); assertEquals(VerificationOutcome.INDETERMINATE, VerificationOutcomePolicy.determine(c)); }
    @Test void failPrecedesUnknown() { var c=new ArrayList<>(allPassed()); replace(c, VerificationCheck.unknown(VerificationCheckType.ACCOUNT_EXISTS)); replace(c, VerificationCheck.failed(VerificationCheckType.ACCOUNT_NOT_BLOCKED, VerificationFailureCode.ACCOUNT_BLOCKED)); assertEquals(VerificationOutcome.REJECTED, VerificationOutcomePolicy.determine(c)); }
}
