package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentT0StateCoherenceTest {
    private static final Path MAIN = Path.of("src/main/java/com/sixpay/payment");

    @Test
    void aggregateHasOneAtomicT0OutcomeModel() throws IOException {
        String payment = Files.readString(MAIN.resolve("domain/model/Payment.java"));
        String state = Files.readString(MAIN.resolve("domain/model/PaymentState.java"));
        assertThat(payment).contains("recordPaymentEventOutcome(").contains("resolvePaymentEventOutcome(")
                .doesNotContain("recordPostingOutcome(").doesNotContain("resolvePostingOutcome(")
                .doesNotContain("PostingOutcomeSnapshot");
        assertThat(state).contains("PaymentEventOutcomeSnapshot")
                .doesNotContain("PostingOutcomeSnapshot").doesNotContain("postingOutcomeEvidence");
    }

    @Test
    void atomicT0MapsOnlyCompletedRejectedUnknown() throws IOException {
        String payment = Files.readString(MAIN.resolve("domain/model/Payment.java"));
        int a=payment.indexOf("private void applyPaymentEventOutcome(");
        int b=payment.indexOf("private void applyReversalOutcome(",a);
        assertThat(a).isGreaterThanOrEqualTo(0); assertThat(b).isGreaterThan(a);
        String method=payment.substring(a,b);
        assertThat(method).contains("case COMPLETED").contains("PaymentStatus.POSTED_PENDING_TFJ")
                .contains("case REJECTED").contains("PaymentStatus.REJECTED")
                .contains("case UNKNOWN").contains("PaymentStatus.POSTING_OUTCOME_UNKNOWN")
                .doesNotContain("PaymentStatus.DEBIT_CONFIRMED").doesNotContain("PaymentStatus.REVERSAL_REQUIRED");
    }

    @Test
    void activeMainHasNoLegacyPostingExecutionTypes() throws IOException {
        try (var paths=Files.walk(MAIN)) {
            var bad=paths.filter(Files::isRegularFile).filter(p->p.toString().endsWith(".java")).filter(p->{
                try { String s=Files.readString(p); return s.contains("PostingGateway") || s.contains("LookupGateway") || s.contains("PostingOutcomeSnapshot") || s.contains("PostingOutcomeDecisionService"); }
                catch(IOException e){ throw new IllegalStateException(e); }
            }).toList();
            assertThat(bad).isEmpty();
        }
    }

    @Test
    void debitConfirmedIsVocabularyOnly() throws IOException {
        assertThat(Files.readString(MAIN.resolve("domain/model/PaymentStatus.java"))).contains("DEBIT_CONFIRMED");
        assertThat(Files.readString(MAIN.resolve("domain/model/PaymentState.java")))
                .contains("DEBIT_CONFIRMED is not reachable in the atomic T0 MVP");
    }
}
