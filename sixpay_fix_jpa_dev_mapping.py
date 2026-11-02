#!/usr/bin/env python3
"""Correctif local: aligner CustomerBankAccountJpaEntity sur Flyway V200 (dev).

Aucun accès Git/réseau, aucun gate.
"""
import argparse
import difflib
from datetime import datetime
from pathlib import Path
import sys

REL = Path('backend/customer/src/main/java/com/sixpay/customer/management/infrastructure/persistence/CustomerBankAccountJpaEntity.java')
OLD = '@Column(name = "currency", nullable = false, length = 3)'
NEW = '@Column(name = "dev", nullable = false, length = 3)'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--dry-run', action='store_true')
    group.add_argument('--apply', action='store_true')
    parser.add_argument('--root', default='.')
    args = parser.parse_args()
    target = Path(args.root).resolve() / REL
    try:
        original = target.read_bytes()
        source = original.decode('utf-8')
        normalized = source.replace('\r\n', '\n')
        if OLD not in normalized and NEW in normalized:
            print('Mapping deja aligne sur dev :', REL)
            return
        if normalized.count(OLD) != 1:
            raise ValueError('Precondition non satisfaite : mapping currency introuvable ou ambigu')
        updated = normalized.replace(OLD, NEW, 1)
        print('Fichier :', REL)
        if args.dry_run:
            print('\n'.join(difflib.unified_diff(normalized.splitlines(), updated.splitlines(), fromfile='avant', tofile='apres', lineterm='')))
            print('DRY RUN : aucune ecriture')
            return
        backup = target.with_name(target.name + '.bak-dev-' + datetime.now().strftime('%Y%m%d-%H%M%S'))
        if backup.exists():
            raise ValueError('La sauvegarde existe deja: ' + str(backup))
        backup.write_bytes(original)
        if target.read_bytes() != original:
            raise ValueError('Fichier modifie pendant la preparation')
        newline = '\r\n' if b'\r\n' in original else '\n'
        temp = target.with_name(target.name + '.dev-mapping.tmp')
        try:
            temp.write_bytes(updated.replace('\n', newline).encode('utf-8'))
            temp.replace(target)
        finally:
            if temp.exists():
                temp.unlink()
        print('Mapping mis a jour. Sauvegarde :', backup)
    except (OSError, ValueError, UnicodeError) as exc:
        print('ERREUR :', exc, file=sys.stderr)
        sys.exit(2)


if __name__ == '__main__':
    main()
