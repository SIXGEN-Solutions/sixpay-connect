package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossDomainForeignKeyArchitectureTest {

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)\\bCREATE\\s+TABLE"
                    + "(?:\\s+IF\\s+NOT\\s+EXISTS)?"
                    + "\\s+([\\w.\"-]+)"
    );

    private static final Pattern REFERENCES = Pattern.compile(
            "(?i)\\bREFERENCES\\s+([\\w.\"-]+)"
    );

    private static final Map<String, Path> BASELINES =
            new LinkedHashMap<>();

    static {
        BASELINES.put("Partner", Path.of(
                "../partner/src/main/resources/db/migration/"
                        + "V100__partner_baseline.sql"));
        BASELINES.put("Customer", Path.of(
                "../customer/src/main/resources/db/migration/"
                        + "V200__customer_baseline.sql"));
        BASELINES.put("Payment", Path.of(
                "../payment/src/main/resources/db/migration/"
                        + "V300__payment_baseline.sql"));
        BASELINES.put("Accounting", Path.of(
                "../accounting/src/main/resources/db/migration/"
                        + "V400__accounting_baseline.sql"));
        BASELINES.put("Reporting", Path.of(
                "../reporting/src/main/resources/db/migration/"
                        + "V500__reporting_baseline.sql"));
        BASELINES.put("Notification", Path.of(
                "../notification/src/main/resources/db/migration/"
                        + "V600__notification_baseline.sql"));
        BASELINES.put("Security", Path.of(
                "../security/src/main/resources/db/migration/"
                        + "V700__security_baseline.sql"));
        BASELINES.put("Administration", Path.of(
                "../administration/src/main/resources/db/migration/"
                        + "V800__administration_baseline.sql"));
    }

    @Test
    void canonicalBaselinesContainNoCrossDomainForeignKey()
            throws Exception {

        for (var entry : BASELINES.entrySet()) {
            String owner = entry.getKey();
            Path baseline = entry.getValue();

            assertTrue(
                    Files.isRegularFile(baseline),
                    () -> owner + " canonical baseline is missing: "
                            + baseline
            );

            String sql = Files.readString(baseline);

            Set<String> ownedTables = tablesCreatedBy(sql);
            Set<String> referencedTables = referencedTables(sql);

            Set<String> externalTargets =
                    new TreeSet<>(referencedTables);

            externalTargets.removeAll(ownedTables);

            assertTrue(
                    externalTargets.isEmpty(),
                    () -> owner
                            + " baseline contains FK references to "
                            + "tables not created by that baseline: "
                            + externalTargets
                            + ". Cross-domain SQL foreign keys are "
                            + "forbidden."
            );
        }
    }

    @Test
    void paymentObservedCustomerLinkRemainsPaymentOwned()
            throws Exception {

        String paymentSql = Files.readString(
                BASELINES.get("Payment")
        );

        String normalized =
                paymentSql.toLowerCase(Locale.ROOT);

        assertTrue(
                normalized.contains(
                        "create table payment_observed_customer_link"
                )
        );

        assertTrue(
                normalized.contains(
                        "references payments(payment_id)"
                )
                        || normalized.contains(
                        "references payments (payment_id)"
                )
        );

        assertFalse(
                normalized.contains(
                        "references customer_observed_customer"
                ),
                "observed_customer_id must remain a logical "
                        + "cross-domain reference without Customer FK"
        );
    }

    private Set<String> tablesCreatedBy(String sql) {
        Set<String> tables = new TreeSet<>();
        var matcher = CREATE_TABLE.matcher(sql);

        while (matcher.find()) {
            tables.add(normalizeTableName(matcher.group(1)));
        }

        return tables;
    }

    private Set<String> referencedTables(String sql) {
        Set<String> tables = new TreeSet<>();
        var matcher = REFERENCES.matcher(sql);

        while (matcher.find()) {
            tables.add(normalizeTableName(matcher.group(1)));
        }

        return tables;
    }

    private String normalizeTableName(String raw) {
        String normalized =
                raw.replace("\"", "")
                        .toLowerCase(Locale.ROOT);

        int schemaSeparator =
                normalized.lastIndexOf('.');

        if (schemaSeparator >= 0) {
            return normalized.substring(schemaSeparator + 1);
        }

        return normalized;
    }
}
