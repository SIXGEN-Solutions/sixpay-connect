#!/usr/bin/env python3
"""S3: actualisation ciblée de deux fixtures obsolètes, sans changer le métier.

Usage: py sixpay_s3_test_regression_fix.py --dry-run --root .
       py sixpay_s3_test_regression_fix.py --apply --root .
Ne lance aucune commande Git ni aucun test du repository.
"""
from __future__ import annotations

import argparse
import datetime
import difflib
import os
from pathlib import Path
import shutil
import sys
import tempfile

ROOT_REL = Path('backend/customer/src/test/java/com/sixpay/customer/verification')
CHANGES = {
    ROOT_REL / 'application/port/output/BankingVerificationResponseTest.java': (
        'type != VerificationCheckType.REQUIRED_KYC_VERIFIED',
        'type != VerificationCheckType.ACCOUNT_EXISTS',
    ),
    ROOT_REL / 'infrastructure/banking/support/BankingVerificationHttpTestSupport.java': (
        ',{\\"type\\":\\"REQUIRED_KYC_VERIFIED\\",\\"result\\":\\"PASS\\"}',
        ',{\\"type\\":\\"ACCOUNT_EXISTS\\",\\"result\\":\\"PASS\\"}',
    ),
}

class PreflightError(Exception):
    pass


def prepare(path: Path, original: bytes, old: str, new: str) -> tuple[bytes, str]:
    if b'\x00' in original:
        raise PreflightError(f'{path}: fichier binaire inattendu')
    try:
        text = original.decode('utf-8')
    except UnicodeDecodeError as e:
        raise PreflightError(f'{path}: UTF-8 attendu') from e
    if old in text:
        if text.count(old) != 1:
            raise PreflightError(f'{path}: occurrence attendue unique de {old!r}')
        changed = text.replace(old, new, 1)
        if path.name == 'BankingVerificationResponseTest.java':
            if 'assertThrows(\n                CustomerVerificationDomainException.class' not in text:
                raise PreflightError(f'{path}: structure du test inattendue')
        elif path.name == 'BankingVerificationHttpTestSupport.java':
            if 'public static String partialJson()' not in text or 'responseJson("INDETERMINATE", checks, false)' not in text:
                raise PreflightError(f'{path}: structure de partialJson() inattendue')
        return changed.encode('utf-8'), 'MODIFIER'
    if new in text:
        if text.count(new) != 1:
            raise PreflightError(f'{path}: version modifiée ambiguë')
        return original, 'DEJA_APPLIQUE'
    raise PreflightError(f'{path}: contenu non reconnu : ni ancienne ni nouvelle assertion')


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--dry-run', action='store_true')
    group.add_argument('--apply', action='store_true')
    parser.add_argument('--root', default='.', help='Racine locale de sixpay-connect')
    args = parser.parse_args()
    root = Path(args.root).resolve()
    if not (root / 'backend' / 'pom.xml').is_file():
        raise PreflightError(f'Racine SIXPAY non reconnue : {root}')
    plan = []
    for rel, (old, new) in CHANGES.items():
        path = root / rel
        if not path.is_file():
            raise PreflightError(f'Fichier obligatoire absent : {path}')
        data = path.read_bytes()
        target, state = prepare(path, data, old, new)
        plan.append((path, data, target, state))
        print(f'{state}: {rel}')
        if args.dry_run and state == 'MODIFIER':
            before = data.decode('utf-8').splitlines(keepends=True)
            after = target.decode('utf-8').splitlines(keepends=True)
            print(''.join(difflib.unified_diff(before, after, fromfile=str(rel), tofile=str(rel) + ' (prévu)')))
    if args.dry_run:
        print('SIMULATION UNIQUEMENT — aucun fichier modifié.')
        return 0
    edits = [item for item in plan if item[3] == 'MODIFIER']
    if not edits:
        print('Idempotence : aucune modification nécessaire.')
        return 0
    stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
    backup_dir = root.parent / f'sixpay_s3_test_backup_{stamp}'
    if backup_dir.exists():
        raise PreflightError(f'Sauvegarde existante : {backup_dir}')
    backup_dir.mkdir(parents=True, exist_ok=False)
    completed = []
    try:
        for path, original, target, _ in edits:
            # Vérifie encore le contenu juste avant toute écriture.
            if path.read_bytes() != original:
                raise PreflightError(f'Le fichier a changé depuis le précontrôle : {path}')
            rel = path.relative_to(root)
            dst = backup_dir / rel
            dst.parent.mkdir(parents=True, exist_ok=True)
            dst.write_bytes(original)
        for path, original, target, _ in edits:
            if path.read_bytes() != original:
                raise PreflightError(f'Le fichier a changé durant la préparation : {path}')
            fd, temp_name = tempfile.mkstemp(prefix='.sixpay_s3_', suffix='.tmp', dir=path.parent)
            try:
                with os.fdopen(fd, 'wb') as file:
                    file.write(target)
                    file.flush()
                    os.fsync(file.fileno())
                os.replace(temp_name, path)
            finally:
                if os.path.exists(temp_name):
                    os.unlink(temp_name)
            completed.append((path, original, target))
            print(f'MODIFIE: {path.relative_to(root)}')
    except Exception:
        for path, original, target in reversed(completed):
            if path.read_bytes() == target:
                path.write_bytes(original)
            else:
                print(f'ATTENTION: restauration manuelle nécessaire, contenu changé : {path}', file=sys.stderr)
        raise
    print(f'Sauvegardes externes : {backup_dir}')
    print('Aucune commande Git ou validation Java exécutée.')
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (PreflightError, OSError) as exc:
        print(f'ERREUR S3 : {exc}', file=sys.stderr)
        sys.exit(1)
