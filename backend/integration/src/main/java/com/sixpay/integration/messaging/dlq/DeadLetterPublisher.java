package com.sixpay.integration.messaging.dlq;
@FunctionalInterface
public interface DeadLetterPublisher { void publish(DeadLetterRecord record); }
