package com.sixpay.accounting.domain.policy;

public record AccountingEligibilityDecision(
        boolean eligible,
        String reasonCode
) {

    public AccountingEligibilityDecision {
        if (reasonCode == null
                || reasonCode.isBlank()) {
            throw new IllegalArgumentException(
                    "reasonCode is required"
            );
        }

        reasonCode = reasonCode.strip();
    }

    public static AccountingEligibilityDecision accepted() {
        return new AccountingEligibilityDecision(
                true,
                "ELIGIBLE"
        );
    }

    public static AccountingEligibilityDecision rejected(
            String reasonCode
    ) {
        return new AccountingEligibilityDecision(
                false,
                reasonCode
        );
    }
}