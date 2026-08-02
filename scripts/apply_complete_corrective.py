#!/usr/bin/env python3
from pathlib import Path
import re
import shutil
import sys

delivery = Path(__file__).resolve().parents[1]
repo = Path(sys.argv[1]).resolve()

for source in (delivery / "files-to-copy").rglob("*"):
    if source.is_file():
        relative = source.relative_to(delivery / "files-to-copy")
        target = repo / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)

def replace_file(relative, substitutions):
    path = repo / relative
    if not path.is_file():
        raise SystemExit(f"Missing expected file: {relative}")
    content = path.read_text(encoding="utf-8")
    for pattern, replacement in substitutions:
        updated, count = re.subn(
            pattern,
            replacement,
            content,
            flags=re.MULTILINE | re.DOTALL
        )
        if count == 0 and replacement not in content:
            raise SystemExit(
                f"Patch point not found in {relative}: {pattern}"
            )
        content = updated
    path.write_text(content, encoding="utf-8", newline="\n")

replace_file(
    "backend/payment/src/main/java/com/sixpay/payment/"
    "application/command/InitiateDebitCommand.java",
    [
        (
            r'idempotencyKey = requireText\(\s*'
            r'idempotencyKey,\s*150,',
            'idempotencyKey = requireText(\\n'
            '                idempotencyKey,\\n'
            '                128,'
        )
    ]
)

replace_file(
    "backend/payment/src/test/java/com/sixpay/payment/"
    "PaymentOutboxAtomicityIT.java",
    [
        (
            r'import com\.sixpay\.payment\.configuration\.'
            r'PaymentModuleConfiguration;\s*',
            'import com.sixpay.payment.support.'
            'PaymentOutboxAtomicityTestConfiguration;\\n'
        ),
        (
            r'import com\.sixpay\.security\.authentication\.'
            r'AuthenticatedUser;\s*',
            ''
        ),
        (
            r'import com\.sixpay\.security\.authentication\.'
            r'CurrentUserProvider;\s*',
            ''
        ),
        (
            r'import org\.springframework\.context\.annotation\.Bean;\s*',
            ''
        ),
        (
            r'import org\.springframework\.boot\.autoconfigure\.'
            r'EnableAutoConfiguration;\s*',
            ''
        ),
        (
            r'import org\.springframework\.boot\.autoconfigure\.'
            r'ImportAutoConfiguration;\s*',
            ''
        ),
        (
            r'import java\.util\.Optional;\s*',
            ''
        ),
        (
            r'@SpringBootConfiguration\s*'
            r'@EnableAutoConfiguration\s*'
            r'@ImportAutoConfiguration\(PaymentModuleConfiguration\.class\)\s*'
            r'static class TestApplication \{.*?\n    \}',
            '@SpringBootConfiguration\\n'
            '    @org.springframework.context.annotation.Import(\\n'
            '            PaymentOutboxAtomicityTestConfiguration.class\\n'
            '    )\\n'
            '    static class TestApplication {\\n'
            '    }'
        )
    ]
)

replace_file(
    "backend/payment/src/test/java/com/sixpay/payment/"
    "infrastructure/audit/PaymentAuditAtomicityIT.java",
    [
        (
            r'import com\.sixpay\.payment\.configuration\.'
            r'PaymentModuleConfiguration;\s*',
            'import com.sixpay.payment.support.'
            'PaymentAuditAtomicityTestConfiguration;\\n'
        ),
        (
            r'import com\.sixpay\.security\.authentication\.'
            r'AuthenticatedUser;\s*',
            ''
        ),
        (
            r'import com\.sixpay\.security\.authentication\.'
            r'CurrentUserProvider;\s*',
            ''
        ),
        (
            r'import org\.springframework\.context\.annotation\.Bean;\s*',
            ''
        ),
        (
            r'import org\.springframework\.boot\.autoconfigure\.'
            r'EnableAutoConfiguration;\s*',
            ''
        ),
        (
            r'import org\.springframework\.boot\.autoconfigure\.'
            r'ImportAutoConfiguration;\s*',
            ''
        ),
        (
            r'import java\.util\.Optional;\s*',
            ''
        ),
        (
            r'@SpringBootConfiguration\s*'
            r'@EnableAutoConfiguration\s*'
            r'@ImportAutoConfiguration\(\s*'
            r'PaymentModuleConfiguration\.class\s*\)\s*'
            r'static class TestApplication \{.*?\n    \}',
            '@SpringBootConfiguration\\n'
            '    @org.springframework.context.annotation.Import(\\n'
            '            PaymentAuditAtomicityTestConfiguration.class\\n'
            '    )\\n'
            '    static class TestApplication {\\n'
            '    }'
        )
    ]
)

target = repo / "backend/payment/target"
if target.exists():
    shutil.rmtree(target)

print("Payment initiation corrective patch applied.")
