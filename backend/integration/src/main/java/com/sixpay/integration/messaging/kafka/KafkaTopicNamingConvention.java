package com.sixpay.integration.messaging.kafka;

import java.util.Locale;
import java.util.regex.Pattern;

public final class KafkaTopicNamingConvention {
    private static final Pattern SEGMENT = Pattern.compile("[a-z0-9][a-z0-9-]*");
    public String topic(String environment, String domain, String eventFamily, int majorVersion) {
        if (majorVersion < 1) throw new IllegalArgumentException("majorVersion must be positive");
        return "sixpay." + segment(environment) + "." + segment(domain) + "."
                + segment(eventFamily) + ".v" + majorVersion;
    }
    public String retryTopic(String topic, int level) {
        if (level < 1) throw new IllegalArgumentException("level must be positive");
        return topic + ".retry." + level;
    }
    public String deadLetterTopic(String topic) { return topic + ".dlq"; }
    private static String segment(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("topic segment is required");
        String normalized = value.strip().toLowerCase(Locale.ROOT).replace('_', '-');
        if (!SEGMENT.matcher(normalized).matches()) throw new IllegalArgumentException("invalid topic segment");
        return normalized;
    }
}
