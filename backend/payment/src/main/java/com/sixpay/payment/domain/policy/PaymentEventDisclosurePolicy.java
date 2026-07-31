package com.sixpay.payment.domain.policy;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PaymentEventDisclosurePolicy {

    public PolicyDecision<EventDisclosureDecision> decide(
            ExplicitEventPayload payload,
            EventDisclosureProfile profile
    ) {
        Objects.requireNonNull(payload, "Explicit event payload");
        Objects.requireNonNull(profile, "Event disclosure profile");

        Set<String> allowed = profile.allowedFieldsByEventType()
                .get(payload.eventType());

        if (allowed == null) {
            return result(
                    profile,
                    EventDisclosureDecision.REJECT_UNDECLARED_FIELD,
                    "EVENT_TYPE_NOT_DECLARED"
            );
        }

        for (Map.Entry<String, Object> entry
                : payload.fields().entrySet()) {
            String field = entry.getKey();

            if (!allowed.contains(field)) {
                return result(
                        profile,
                        EventDisclosureDecision.REJECT_UNDECLARED_FIELD,
                        "UNDECLARED_FIELD_" + field
                );
            }

            if (profile.sensitiveFieldNames().contains(field)) {
                return result(
                        profile,
                        EventDisclosureDecision.REJECT_SENSITIVE_DATA,
                        "SENSITIVE_FIELD_" + field
                );
            }

            EventDataClassification classification =
                    profile.classificationByField()
                            .getOrDefault(
                                    field,
                                    EventDataClassification.SENSITIVE
                            );

            if (!profile.permittedClassifications().contains(
                    classification
            )) {
                return result(
                        profile,
                        EventDisclosureDecision.REJECT_CLASSIFICATION,
                        "FIELD_CLASSIFICATION_NOT_PERMITTED_" + field
                );
            }
        }

        return result(
                profile,
                EventDisclosureDecision.ALLOW,
                "EVENT_PAYLOAD_ALLOWED"
        );
    }

    private static PolicyDecision<EventDisclosureDecision> result(
            EventDisclosureProfile profile,
            EventDisclosureDecision decision,
            String reason
    ) {
        return PolicyDecision.withProfile(
                decision,
                reason,
                profile.metadata()
        );
    }
}
