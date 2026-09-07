package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.AccountingCandidateProjectionRepository;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;
import com.sixpay.accounting.events.PaymentT0FinalizedForAccounting;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountingCandidateProjectionServiceTest {
    @Test
    void sameEventReplayIsNoOp() {
        var repo = new InMemoryRepository();
        var service = service(repo);
        var event = eventWithIds(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());
        var first = service.project(event);
        var replay = service.project(event);
        assertSame(first,replay);
        assertEquals(1,repo.saved.size());
    }

    @Test
    void differentEventWithSameBusinessIdentityConverges() {
        var repo = new InMemoryRepository();
        var service = service(repo);
        UUID paymentId=UUID.randomUUID(), snapshotId=UUID.randomUUID();
        var first=service.project(eventWithIds(UUID.randomUUID(),paymentId,snapshotId));
        var second=service.project(eventWithIds(UUID.randomUUID(),paymentId,snapshotId));
        assertSame(first,second);
        assertEquals(1,repo.saved.size());
    }

    private static AccountingCandidateProjectionService service(InMemoryRepository repo) {
        return new AccountingCandidateProjectionService(
                repo, Clock.fixed(Instant.parse("2026-08-11T12:10:00Z"), ZoneOffset.UTC)
        );
    }

    private static PaymentT0FinalizedForAccounting eventWithIds(UUID eventId, UUID paymentId, UUID snapshotId) {
        return new PaymentT0FinalizedForAccounting(
                eventId, Instant.parse("2026-08-11T12:00:00Z"),1,paymentId,
                "REF-DGI-2026-0042","TRESORPAY","LRB","POSTED_PENDING_TFJ","COMPLETED",
                "RB-2026081100045",Instant.parse("2026-08-11T11:59:00Z"),LocalDate.of(2026,8,11),
                snapshotId,"v1",Instant.parse("2026-08-11T11:59:30Z"),
                "DEBTOR-REF","TREASURY-REF",new BigDecimal("1000.00"),Currency.getInstance("XAF"),
                Instant.parse("2026-08-11T11:58:00Z"),
                List.of(
                        new PaymentT0FinalizedForAccounting.Entry(UUID.randomUUID(),1,"DEBIT","DEBTOR-REF",new BigDecimal("1000.00"),Currency.getInstance("XAF"),Instant.parse("2026-08-11T11:59:10Z")),
                        new PaymentT0FinalizedForAccounting.Entry(UUID.randomUUID(),2,"CREDIT","TREASURY-REF",new BigDecimal("1000.00"),Currency.getInstance("XAF"),Instant.parse("2026-08-11T11:59:11Z"))
                )
        );
    }

    private static final class InMemoryRepository implements AccountingCandidateProjectionRepository {
        private final List<AccountingCandidateProjection> saved=new ArrayList<>();
        public Optional<AccountingCandidateProjection> findByEventId(UUID id){return saved.stream().filter(c->c.eventId().equals(id)).findFirst();}
        public Optional<AccountingCandidateProjection> findByBusinessIdentity(UUID p,UUID s){return saved.stream().filter(c->c.paymentId().equals(p)&&c.financialSnapshotId().equals(s)).findFirst();}
        public AccountingCandidateProjection save(AccountingCandidateProjection p){saved.add(p);return p;}
        public List<AccountingCandidateProjection> findEligibleUnbatched(AccountingSelectionWindow w){return List.of();}
        public void recordTresorPayEvidence(UUID p,TresorPayPaymentStatusEvidence e){}
        public void assignToBatch(UUID p,UUID b){}
    }
}
