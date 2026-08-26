package com.sixpay.integration.outbox;

@FunctionalInterface
public interface OutboxTopicResolver {

    String topicFor(String eventType);
}
