from pathlib import Path
import re
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

DOC = ROOT / "documentation/architecture/persistence/FS-2.3.5_CROSS_DOMAIN_PERSISTENCE_REVIEW.md"
TEST = ROOT / "backend/bootstrap/src/test/java/com/sixpay/bootstrap/architecture/CrossDomainForeignKeyArchitectureTest.java"

BASELINES = {
    "Partner": ROOT / "backend/partner/src/main/resources/db/migration/V100__partner_baseline.sql",
    "Customer": ROOT / "backend/customer/src/main/resources/db/migration/V200__customer_baseline.sql",
    "Payment": ROOT / "backend/payment/src/main/resources/db/migration/V300__payment_baseline.sql",
    "Accounting": ROOT / "backend/accounting/src/main/resources/db/migration/V400__accounting_baseline.sql",
    "Reporting": ROOT / "backend/reporting/src/main/resources/db/migration/V500__reporting_baseline.sql",
    "Notification": ROOT / "backend/notification/src/main/resources/db/migration/V600__notification_baseline.sql",
    "Security": ROOT / "backend/security/src/main/resources/db/migration/V700__security_baseline.sql",
    "Administration": ROOT / "backend/administration/src/main/resources/db/migration/V800__administration_baseline.sql",
}

REPORT = '# FS-2.3.5 — Cross-Domain Persistence Review\n\n**Branch:** `feat/repository-baseline-consolidation`  \n**Gate:** `FS-2.3 — Database baseline consolidation`  \n**Status:** Reviewed  \n**Golden module:** Partner\n\n## Purpose\n\nFS-2.3.5 verifies that a domain-owned database baseline does not create a\nphysical SQL foreign key to a table owned by another bounded context.\n\nCanonical rule:\n\n```text\ntable A owner X\n    REFERENCES table B owner Y\n\nif X != Y\n    => forbidden by default\n```\n\nCross-domain relationships remain logical references unless an explicit\narchitecture decision authorizes a physical dependency.\n\n## Reviewed baselines\n\n| Baseline | Owner | Physical FK target ownership | Result |\n|---|---|---|---|\n| `V100__partner_baseline.sql` | Partner | Partner only | PASS |\n| `V200__customer_baseline.sql` | Customer | Customer only | PASS |\n| `V300__payment_baseline.sql` | Payment | Payment only | PASS |\n| `V400__accounting_baseline.sql` | Accounting | Accounting only | PASS |\n| `V500__reporting_baseline.sql` | Reporting | No external FK | PASS |\n| `V600__notification_baseline.sql` | Notification | Notification only | PASS |\n| `V700__security_baseline.sql` | Security | Security only | PASS |\n| `V800__administration_baseline.sql` | Administration | Administration only | PASS |\n\n## Review conclusion\n\nCurrent canonical baseline:\n\n```text\ncross-domain physical FK = 0\n```\n\nAll discovered physical FK targets are owned by the same module baseline as the\nreferencing persistence model.\n\nNo exception or ADR is required for the current baseline.\n\n### Important logical cross-domain references\n\nThese identifiers remain logical only and intentionally have no SQL FK to another domain:\n\n- Customer: `partner_id`, `payment_id`\n- Payment: `observed_customer_id`\n- Accounting: `payment_id`, `partner_id`\n- Reporting: `payment_id`, `observed_customer_id`\n- Notification: `event_id`, `aggregate_id`\n- Administration: `accounting_batch_id`, `payment_id`, `payment_reference`\n\n### `payment_observed_customer_link`\n\nThis table is Payment-owned.\n\n```text\npayment_observed_customer_link.payment_id\n    -> payments.payment_id\n```\n\nBoth source and target are Payment-owned.\n\n`observed_customer_id` remains a logical Customer reference and has no FK to a\nCustomer table.\n\n## Non-regression rule\n\nEvery table named by a `REFERENCES` clause in a canonical Vx00 baseline must\nalso be created by that same Vx00 baseline.\n\nThis stricter rule is valid for the current one-baseline-per-domain model and\nautomatically rejects a direct cross-domain FK.\n\n## Exit criteria\n\nFS-2.3.5 is complete when:\n\n- all V100–V800 baselines have been inspected;\n- every physical FK resolves to the same domain baseline;\n- `payment_observed_customer_link` is formally Payment-owned;\n- logical cross-domain identifiers remain unconstrained by external SQL FKs;\n- current cross-domain physical FK count is zero;\n- a non-regression architecture test enforces the rule.\n\n## Decision\n\n```text\nsame-domain FK       = ALLOWED\ncross-domain FK      = FORBIDDEN BY DEFAULT\nlogical cross-domain = ALLOWED\n\ncurrent cross-domain FK count = 0\n```\n'
JAVA = 'package com.sixpay.bootstrap.architecture;\n\nimport org.junit.jupiter.api.Test;\n\nimport java.nio.file.Files;\nimport java.nio.file.Path;\nimport java.util.LinkedHashMap;\nimport java.util.Locale;\nimport java.util.Map;\nimport java.util.Set;\nimport java.util.TreeSet;\nimport java.util.regex.Pattern;\n\nimport static org.junit.jupiter.api.Assertions.assertFalse;\nimport static org.junit.jupiter.api.Assertions.assertTrue;\n\nclass CrossDomainForeignKeyArchitectureTest {\n\n    private static final Pattern CREATE_TABLE = Pattern.compile(\n            "(?i)\\\\bCREATE\\\\s+TABLE"\n                    + "(?:\\\\s+IF\\\\s+NOT\\\\s+EXISTS)?"\n                    + "\\\\s+([\\\\w.\\"-]+)"\n    );\n\n    private static final Pattern REFERENCES = Pattern.compile(\n            "(?i)\\\\bREFERENCES\\\\s+([\\\\w.\\"-]+)"\n    );\n\n    private static final Map<String, Path> BASELINES =\n            new LinkedHashMap<>();\n\n    static {\n        BASELINES.put("Partner", Path.of(\n                "../partner/src/main/resources/db/migration/"\n                        + "V100__partner_baseline.sql"));\n        BASELINES.put("Customer", Path.of(\n                "../customer/src/main/resources/db/migration/"\n                        + "V200__customer_baseline.sql"));\n        BASELINES.put("Payment", Path.of(\n                "../payment/src/main/resources/db/migration/"\n                        + "V300__payment_baseline.sql"));\n        BASELINES.put("Accounting", Path.of(\n                "../accounting/src/main/resources/db/migration/"\n                        + "V400__accounting_baseline.sql"));\n        BASELINES.put("Reporting", Path.of(\n                "../reporting/src/main/resources/db/migration/"\n                        + "V500__reporting_baseline.sql"));\n        BASELINES.put("Notification", Path.of(\n                "../notification/src/main/resources/db/migration/"\n                        + "V600__notification_baseline.sql"));\n        BASELINES.put("Security", Path.of(\n                "../security/src/main/resources/db/migration/"\n                        + "V700__security_baseline.sql"));\n        BASELINES.put("Administration", Path.of(\n                "../administration/src/main/resources/db/migration/"\n                        + "V800__administration_baseline.sql"));\n    }\n\n    @Test\n    void canonicalBaselinesContainNoCrossDomainForeignKey()\n            throws Exception {\n\n        for (var entry : BASELINES.entrySet()) {\n            String owner = entry.getKey();\n            Path baseline = entry.getValue();\n\n            assertTrue(\n                    Files.isRegularFile(baseline),\n                    () -> owner + " canonical baseline is missing: "\n                            + baseline\n            );\n\n            String sql = Files.readString(baseline);\n\n            Set<String> ownedTables = tablesCreatedBy(sql);\n            Set<String> referencedTables = referencedTables(sql);\n\n            Set<String> externalTargets =\n                    new TreeSet<>(referencedTables);\n\n            externalTargets.removeAll(ownedTables);\n\n            assertTrue(\n                    externalTargets.isEmpty(),\n                    () -> owner\n                            + " baseline contains FK references to "\n                            + "tables not created by that baseline: "\n                            + externalTargets\n                            + ". Cross-domain SQL foreign keys are "\n                            + "forbidden."\n            );\n        }\n    }\n\n    @Test\n    void paymentObservedCustomerLinkRemainsPaymentOwned()\n            throws Exception {\n\n        String paymentSql = Files.readString(\n                BASELINES.get("Payment")\n        );\n\n        String normalized =\n                paymentSql.toLowerCase(Locale.ROOT);\n\n        assertTrue(\n                normalized.contains(\n                        "create table payment_observed_customer_link"\n                )\n        );\n\n        assertTrue(\n                normalized.contains(\n                        "references payments(payment_id)"\n                )\n                        || normalized.contains(\n                        "references payments (payment_id)"\n                )\n        );\n\n        assertFalse(\n                normalized.contains(\n                        "references customer_observed_customer"\n                ),\n                "observed_customer_id must remain a logical "\n                        + "cross-domain reference without Customer FK"\n        );\n    }\n\n    private Set<String> tablesCreatedBy(String sql) {\n        Set<String> tables = new TreeSet<>();\n        var matcher = CREATE_TABLE.matcher(sql);\n\n        while (matcher.find()) {\n            tables.add(normalizeTableName(matcher.group(1)));\n        }\n\n        return tables;\n    }\n\n    private Set<String> referencedTables(String sql) {\n        Set<String> tables = new TreeSet<>();\n        var matcher = REFERENCES.matcher(sql);\n\n        while (matcher.find()) {\n            tables.add(normalizeTableName(matcher.group(1)));\n        }\n\n        return tables;\n    }\n\n    private String normalizeTableName(String raw) {\n        String normalized =\n                raw.replace("\\"", "")\n                        .toLowerCase(Locale.ROOT);\n\n        int schemaSeparator =\n                normalized.lastIndexOf(\'.\');\n\n        if (schemaSeparator >= 0) {\n            return normalized.substring(schemaSeparator + 1);\n        }\n\n        return normalized;\n    }\n}\n'

def fail(message):
    print(f"ERROR: {message}")
    sys.exit(1)

def require(path):
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")

def normalize(raw):
    value = raw.replace('"', '').lower()
    return value.rsplit('.', 1)[-1]

def main():
    engineering = require(ENGINEERING)
    if EXPECTED_BRANCH not in engineering:
        fail(f"ENGINEERING_CONTEXT.md does not declare {EXPECTED_BRANCH}.")

    create_pattern = re.compile(
        r"(?i)\bCREATE\s+TABLE(?:\s+IF\s+NOT\s+EXISTS)?\s+([\w.\"-]+)"
    )
    ref_pattern = re.compile(
        r"(?i)\bREFERENCES\s+([\w.\"-]+)"
    )

    violations = []

    for owner, baseline in BASELINES.items():
        sql = require(baseline)
        created = {
            normalize(m.group(1))
            for m in create_pattern.finditer(sql)
        }
        referenced = {
            normalize(m.group(1))
            for m in ref_pattern.finditer(sql)
        }

        external = sorted(referenced - created)

        if external:
            violations.append(f"{owner}: {external}")

    if violations:
        fail(
            "Cross-domain or unresolved FK targets detected:\n - "
            + "\n - ".join(violations)
        )

    DOC.parent.mkdir(parents=True, exist_ok=True)
    TEST.parent.mkdir(parents=True, exist_ok=True)

    if DOC.exists():
        fail(f"Review document already exists: {DOC.relative_to(ROOT)}")

    if TEST.exists():
        fail(f"Architecture test already exists: {TEST.relative_to(ROOT)}")

    DOC.write_text(REPORT, encoding="utf-8")
    TEST.write_text(JAVA, encoding="utf-8")

    print("FS-2.3.5 cross-domain persistence review installed.")
    print(f"Created: {DOC.relative_to(ROOT)}")
    print(f"Created: {TEST.relative_to(ROOT)}")
    print()
    print("Audit result:")
    print(" - canonical baselines reviewed: 8")
    print(" - cross-domain physical FK: 0")
    print(" - payment_observed_customer_link owner: Payment")
    print()
    print("Run:")
    print("  cd backend")
    print("  mvn -pl bootstrap -am test")

if __name__ == "__main__":
    main()
