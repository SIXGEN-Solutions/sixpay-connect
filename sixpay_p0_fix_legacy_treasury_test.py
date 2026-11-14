#!/usr/bin/env python3
"""Update the legacy Treasury equality assertion after the P0 accountToken identity fix.

Usage: py sixpay_p0_fix_legacy_treasury_test.py --root . --dry-run
       py sixpay_p0_fix_legacy_treasury_test.py --root . --apply
No Git operations, worktree checks, gates, commits, or pushes.
"""
import argparse
import difflib
from pathlib import Path

REL = Path('backend/payment/src/test/java/com/sixpay/payment/domain/model/ProtectedAccountValueObjectsTest.java')
OLD_NAME = 'void treasuryReferenceEqualityUsesConfigurationIdentity() {'
NEW_NAME = 'void treasuryReferenceEqualityIncludesAccountToken() {'
OLD_ASSERT = 'assertEquals(first, sameConfiguration);'
NEW_ASSERT = 'assertNotEquals(first, sameConfiguration);'

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path('.'))
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--dry-run', action='store_true')
    mode.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    path = args.root.resolve() / REL
    raw = path.read_bytes()
    newline = '\r\n' if b'\r\n' in raw else '\n'
    source = raw.decode('utf-8').replace('\r\n', '\n')
    if source.count(OLD_NAME) != 1:
        raise SystemExit('Expected exactly one legacy Treasury test method; no changes made.')
    method_start = source.index(OLD_NAME)
    method_end = source.find('\n    @Test', method_start)
    if method_end == -1:
        raise SystemExit('Cannot locate end of Treasury test method; no changes made.')
    method = source[method_start:method_end]
    if method.count(OLD_ASSERT) != 1:
        raise SystemExit('Expected exactly one legacy assertion in Treasury method; no changes made.')
    new_method = method.replace(OLD_NAME, NEW_NAME, 1).replace(OLD_ASSERT, NEW_ASSERT, 1)
    revised = source[:method_start] + new_method + source[method_end:]
    print(''.join(difflib.unified_diff(source.splitlines(True), revised.splitlines(True),
        fromfile='a/' + REL.as_posix(), tofile='b/' + REL.as_posix())))
    if args.dry_run:
        print('DRY RUN: no files changed.')
    else:
        path.write_bytes(revised.replace('\n', newline).encode('utf-8'))
        print('Applied locally. No gates or Git operations executed.')

if __name__ == '__main__':
    main()
