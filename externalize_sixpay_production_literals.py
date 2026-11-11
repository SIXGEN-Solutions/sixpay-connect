#!/usr/bin/env python3
"""Externalise les literals operatoires de application-production.yml.

Usage (depuis la racine du repo):
    py externalize_sixpay_production_literals.py --check
    py externalize_sixpay_production_literals.py --apply

Aucune modification des secrets, des autres profils, des contrats ou de CI.
Les garde-fous securite/architecture restent volontairement constants.
"""
from __future__ import annotations
import argparse
import difflib
from pathlib import Path
import re
import sys

TARGET = Path('backend/bootstrap/src/main/resources/application-production.yml')
# chemin YAML -> (ancienne valeur exacte, nom variable). On conserve les valeurs comme defaults.
RULES = {
    # Runtime toggles and tuning values: preserve exactly the existing defaults.
    'management.messaging.outbox.enabled': ('true', 'SIXPAY_MESSAGING_OUTBOX_ENABLED'),
    'sixpay.payment.t0-recovery.enabled': ('false', 'SIXPAY_PAYMENT_T0_RECOVERY_ENABLED'),
    'sixpay.payment.t0-recovery.batch-size': ('50', 'SIXPAY_PAYMENT_T0_RECOVERY_BATCH_SIZE'),
    'sixpay.payment.t0-recovery.window': ('PT48H', 'SIXPAY_PAYMENT_T0_RECOVERY_WINDOW'),
    'sixpay.payment.t0-recovery.minimum-age': ('PT2M', 'SIXPAY_PAYMENT_T0_RECOVERY_MINIMUM_AGE'),
    'sixpay.payment.t0-recovery.claim-timeout': ('PT5M', 'SIXPAY_PAYMENT_T0_RECOVERY_CLAIM_TIMEOUT'),
    'sixpay.payment.t0-recovery.poll-delay': ('PT2M', 'SIXPAY_PAYMENT_T0_RECOVERY_POLL_DELAY'),
    'sixpay.payment.outbox.customer-projection.enabled': ('true', 'SIXPAY_PAYMENT_CUSTOMER_PROJECTION_OUTBOX_ENABLED'),
    'sixpay.payment.outbox.customer-projection.batch-size': ('50', 'SIXPAY_PAYMENT_CUSTOMER_PROJECTION_OUTBOX_BATCH_SIZE'),
    'sixpay.payment.outbox.customer-projection.polling-interval': ('1s', 'SIXPAY_PAYMENT_CUSTOMER_PROJECTION_OUTBOX_POLLING_INTERVAL'),
    'sixpay.payment.outbox.customer-projection.max-attempts': ('10', 'SIXPAY_PAYMENT_CUSTOMER_PROJECTION_OUTBOX_MAX_ATTEMPTS'),
    'sixpay.payment.outbox.customer-projection.initial-backoff': ('1s', 'SIXPAY_PAYMENT_CUSTOMER_PROJECTION_OUTBOX_INITIAL_BACKOFF'),
    'sixpay.payment.outbox.customer-projection.max-backoff': ('5m', 'SIXPAY_PAYMENT_CUSTOMER_PROJECTION_OUTBOX_MAX_BACKOFF'),
    'sixpay.payment.outbox.customer-projection.processing-timeout': ('2m', 'SIXPAY_PAYMENT_CUSTOMER_PROJECTION_OUTBOX_PROCESSING_TIMEOUT'),
}
# These are NOT general-purpose deployment overrides, hence remain fixed:
# - Flyway clean-disabled, schema/locations and out-of-order migration governance;
# - JPA ddl-auto=validate, Springdoc disabled, http-only/same-site and require-provider;
# - bank contract response codes, approved versions and treasury configuration IDs;
# - mandatory observation persistence/audit controls;
# - spring.config.activate profile selector, static application and classpath metadata.
# The second YAML document (production & amplitude-banking) already uses
# environment placeholders for its OAuth2 and TLS runtime parameters.
LIST_RULES = {}
KEY_RE = re.compile(r'^(?P<indent>\s*)(?P<key>[\w-]+):(?P<space>\s*)(?P<value>[^#\n]*?)(?P<end>\s*(?:#.*)?)$')

def rewrite(text: str) -> tuple[str, list[str]]:
    stack: list[tuple[int, str]] = []
    changed: list[str] = []
    output: list[str] = []
    for original in text.splitlines(keepends=True):
        newline = '\n' if original.endswith('\n') else ''
        line = original[:-1] if newline else original
        if line.strip() == '---':
            stack.clear()
            output.append(original)
            continue
        m = KEY_RE.match(line)
        if m and not line.lstrip().startswith('#'):
            indent = len(m.group('indent').replace('\t', '    '))
            while stack and stack[-1][0] >= indent:
                stack.pop()
            path = '.'.join([t[1] for t in stack] + [m.group('key')])
            value = m.group('value').strip()
            if path in RULES and value and not value.startswith('${'):
                expected, name = RULES[path]
                if value != expected:
                    raise ValueError(f'{path}: expected {expected!r}, found {value!r}')
                # Keep YAML type/coercion and the previous default unchanged.
                line = (m.group('indent') + m.group('key') + ': '
                        + '${' + name + ':' + expected.strip('"') + '}' + m.group('end'))
                changed.append(path + ' -> ' + name)
            if not value:
                stack.append((indent, m.group('key')))
        elif line.lstrip().startswith('- ') and stack:
            indent = len(line) - len(line.lstrip())
            path = '.'.join(t[1] for t in stack)
            if path in LIST_RULES:
                expected, name = LIST_RULES[path]
                if line.strip() == '- ' + expected:
                    line = ' ' * indent + '- ${' + name + ':' + expected.strip('"') + '}'
                    changed.append(path + '[] -> ' + name)
        output.append(line + newline)
    missing = set(RULES) - {x.split(' -> ')[0] for x in changed}
    missing |= {k + '[]' for k in LIST_RULES if k + '[]' not in {x.split(' -> ')[0] for x in changed}}
    if missing:
        raise ValueError('Paths absent or already modified (no partial write): ' + ', '.join(sorted(missing)))
    return ''.join(output), changed

def main() -> int:
    p = argparse.ArgumentParser()
    x = p.add_mutually_exclusive_group(required=True)
    x.add_argument('--check', action='store_true', help='show planned diff, do not modify')
    x.add_argument('--apply', action='store_true', help='write only expected file')
    a = p.parse_args()
    if not TARGET.is_file():
        print('Run from repository root. Missing: ' + str(TARGET), file=sys.stderr)
        return 2
    old = TARGET.read_text(encoding='utf-8')
    new, changed = rewrite(old)
    print('Changed properties:', len(changed))
    for item in changed:
        print('  ' + item)
    if a.check:
        print(''.join(difflib.unified_diff(old.splitlines(True), new.splitlines(True), fromfile='a/'+str(TARGET), tofile='b/'+str(TARGET))))
        return 0
    TARGET.write_text(new, encoding='utf-8')
    print('UPDATED ' + str(TARGET))
    return 0

if __name__ == '__main__':
    try:
        sys.exit(main())
    except ValueError as e:
        print('ABORTED: ' + str(e), file=sys.stderr)
        sys.exit(1)
