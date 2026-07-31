package com.sixpay.payment.domain.policy;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EventDisclosureProfile(
        PolicyProfileMetadata metadata,
        Map<String, Set<String>> allowedFieldsByEventType,
        Map<String, EventDataClassification> classificationByField,
        Set<String> sensitiveFieldNames,
        Set<EventDataClassification> permittedClassifications
) {
    public EventDisclosureProfile {
        metadata = Objects.requireNonNull(metadata, "Profile metadata");
        Objects.requireNonNull(
                allowedFieldsByEventType,
                "Event field allowlists"
        );
        Objects.requireNonNull(
                classificationByField,
                "Field classifications"
        );
        Objects.requireNonNull(
                sensitiveFieldNames,
                "Sensitive field names"
        );
        Objects.requireNonNull(
                permittedClassifications,
                "Permitted classifications"
        );

        allowedFieldsByEventType = allowedFieldsByEventType.entrySet()
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> Set.copyOf(entry.getValue())
                ));
        classificationByField = Map.copyOf(classificationByField);
        sensitiveFieldNames = Set.copyOf(sensitiveFieldNames);
        permittedClassifications =
                Set.copyOf(permittedClassifications);
    }
}
