#!/usr/bin/env python3
"""CB-1: fix Mockito nested stubbing in CustomerEnrollmentServiceTest only.

No Git, Maven, network or repository commands are executed.
"""
import argparse
import os
from pathlib import Path
import tempfile

REL = Path('backend/customer/src/test/java/com/sixpay/customer/management/application/service/CustomerEnrollmentServiceTest.java')
OLD = 'when(verification.verify(any())).thenReturn(verified("ACC-001"));'
NEW = 'VerifyCustomerResult verifiedResult = verified("ACC-001");\n        when(verification.verify(any())).thenReturn(verifiedResult);'
OLD_MISMATCH = 'when(verification.verify(any())).thenReturn(verified("ANOTHER-ACCOUNT"));'
NEW_MISMATCH = 'VerifyCustomerResult mismatchedResult = verified("ANOTHER-ACCOUNT");\n        when(verification.verify(any())).thenReturn(mismatchedResult);'

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument('--dry-run', action='store_true')
    modes.add_argument('--apply', action='store_true')
    parser.add_argument('--root', type=Path, default=Path('.'))
    args = parser.parse_args()
    file = args.root.resolve() / REL
    if not file.is_file():
        parser.error(f'File not found: {file}')
    raw = file.read_bytes()
    try:
        source = raw.decode('utf-8')
    except UnicodeDecodeError:
        parser.error('Test file must be UTF-8')
    newline = '\r\n' if b'\r\n' in raw else '\n'
    normalized = source.replace('\r\n', '\n')
    if all(t in normalized for t in (NEW, NEW_MISMATCH)) and OLD not in normalized and OLD_MISMATCH not in normalized:
        print('ALREADY_APPLIED:', REL)
        return
    required = ('void verifiesOnceThenCreatesCustomerFromBankingEvidence()',
                'void mismatchedBankAccountDoesNotPersist()',
                'private VerifyCustomerResult verified(String accountReference)')
    if not all(x in normalized for x in required):
        parser.error('Unexpected test structure; no modification')
    if normalized.count(OLD) != 1 or normalized.count(OLD_MISMATCH) != 1:
        parser.error('Expected both exact nested stubbings once each; no modification')
    updated = normalized.replace(OLD, NEW).replace(OLD_MISMATCH, NEW_MISMATCH)
    updated_raw = updated.replace('\n', newline).encode('utf-8')
    print(f'{"WOULD_MODIFY" if args.dry_run else "MODIFY"}: {REL}')
    if args.dry_run:
        return
    backup = file.with_name(file.name + '.cb1-mockito.bak')
    if backup.exists():
        parser.error(f'Backup exists, refusing overwrite: {backup}')
    if file.read_bytes() != raw:
        parser.error('File changed since reading; refusing overwrite')
    with backup.open('xb') as b:
        b.write(raw)
    try:
        fd, temp_path = tempfile.mkstemp(dir=file.parent, prefix='.cb1-mockito-', suffix='.tmp')
        try:
            with os.fdopen(fd, 'wb') as f:
                f.write(updated_raw)
            if file.read_bytes() != raw:
                raise RuntimeError('Test file changed during patch; refusing overwrite')
            os.replace(temp_path, file)
        finally:
            if os.path.exists(temp_path):
                os.unlink(temp_path)
    except Exception as e:
        parser.error(f'Update failed; original remains available in {backup}: {e}')
    print('BACKUP:', backup)

if __name__ == '__main__':
    main()
