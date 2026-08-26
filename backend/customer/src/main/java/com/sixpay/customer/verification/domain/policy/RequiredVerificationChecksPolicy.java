package com.sixpay.customer.verification.domain.policy;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationCheckType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RequiredVerificationChecksPolicy {
    private RequiredVerificationChecksPolicy() {}
    public static List<VerificationCheck> requireComplete(Collection<VerificationCheck> checks) {
        Objects.requireNonNull(checks, "checks are required");
        Map<VerificationCheckType, VerificationCheck> byType = new EnumMap<>(VerificationCheckType.class);
        for (VerificationCheck check : checks) {
            VerificationCheck validated = Objects.requireNonNull(check, "verification check is required");
            if (byType.putIfAbsent(validated.type(), validated) != null)
                throw new CustomerVerificationDomainException("Duplicate verification check type: " + validated.type());
        }
        List<VerificationCheckType> missing = new ArrayList<>();
        for (VerificationCheckType required : VerificationCheckType.values())
            if (!byType.containsKey(required)) missing.add(required);
        if (!missing.isEmpty())
            throw new CustomerVerificationDomainException("Missing mandatory verification checks: " + missing);
        List<VerificationCheck> canonical = new ArrayList<>(byType.values());
        canonical.sort((l, r) -> Integer.compare(l.type().ordinal(), r.type().ordinal()));
        return List.copyOf(canonical);
    }
}
