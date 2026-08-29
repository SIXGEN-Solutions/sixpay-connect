package com.sixpay.accounting.domain.policy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

public final class DailyAccountingCutoffPolicy
        implements AccountingCutoffPolicy {

    private final ZoneId zoneId;
    private final LocalTime cutoffTime;

    public DailyAccountingCutoffPolicy(
            ZoneId zoneId,
            LocalTime cutoffTime
    ) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
        this.cutoffTime = Objects.requireNonNull(cutoffTime, "cutoffTime");
    }

    @Override
    public AccountingSelectionWindow resolve(
            Instant runAt,
            AccountingCutoffMode mode,
            Optional<LocalDate> manualBusinessDate
    ) {
        Objects.requireNonNull(runAt, "runAt");
        Objects.requireNonNull(mode, "mode");

        Optional<LocalDate> manual = manualBusinessDate == null
                ? Optional.empty()
                : manualBusinessDate;

        LocalDate businessDate;

        if (mode == AccountingCutoffMode.MANUAL) {
            businessDate = manual.orElseThrow(
                    () -> new IllegalArgumentException(
                            "manualBusinessDate is required input MANUAL mode"
                    )
            );
        } else {
            if (manual.isPresent()) {
                throw new IllegalArgumentException(
                        "manualBusinessDate must be empty input AUTO mode"
                );
            }

            ZonedDateTime localRun = runAt.atZone(zoneId);
            businessDate = !localRun.toLocalTime().isBefore(cutoffTime)
                    ? localRun.toLocalDate()
                    : localRun.toLocalDate().minusDays(1);
        }

        Instant toExclusive = businessDate
                .atTime(cutoffTime)
                .atZone(zoneId)
                .toInstant();

        Instant fromInclusive = businessDate
                .minusDays(1)
                .atTime(cutoffTime)
                .atZone(zoneId)
                .toInstant();

        return new AccountingSelectionWindow(
                businessDate,
                fromInclusive,
                toExclusive
        );
    }
}
