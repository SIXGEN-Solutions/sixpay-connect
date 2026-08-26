package com.sixpay.integration.kafka.replay;

public interface KafkaReplayAuditStore {

    void recordRequested(ReplayRequest request);

    void recordCompleted(ReplayRequest request);

    void recordFailed(
            ReplayRequest request,
            String safeErrorCode
    );
}
