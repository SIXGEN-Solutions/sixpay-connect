package com.sixpay.accounting.application.port.output;

import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountingCandidateProjectionRepository {
    Optional<AccountingCandidateProjection> findByEventId(UUID eventId);
    Optional<AccountingCandidateProjection> findByBusinessIdentity(UUID paymentId, UUID financialSnapshotId);
    AccountingCandidateProjection save(AccountingCandidateProjection projection);
    List<AccountingCandidateProjection> findEligibleUnbatched(AccountingSelectionWindow window);
    List<AccountingCandidateProjection> findUnbatchedForVerification(AccountingSelectionWindow window);
    void recordTresorPayEvidence(UUID paymentId, TresorPayPaymentStatusEvidence evidence);
    void assignToBatch(UUID paymentId, UUID batchId);
}
