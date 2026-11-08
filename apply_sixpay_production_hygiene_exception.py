#!/usr/bin/env python3
"""Correctif local borné SIXPAY: règle de doublons production et documentation.

Référence: SIXGEN-Solutions/sixpay-connect @ 498cbb3ae899880d2ee2489fdf0b4f8db4309091
N'exécute aucun gate, aucune commande Git et aucune opération réseau.
"""
from __future__ import annotations

import argparse
import difflib
import os
from pathlib import Path
import shutil
import sys
from datetime import datetime, timezone

VERIFY = Path('scripts/verify_spring_configuration_hygiene.py')
DOC = Path('documentation/architecture/CONFIGURATION_ARCHITECTURE.md')

OLD_VERIFY = '''    for (key, value), paths in declarations.items():
        if len(paths) < 2 or key in ALLOWED_IDENTICAL_PROFILE_KEYS:
            continue
        errors.append(
            f"duplicate runtime value {key}={value}: "
            + ", ".join(relative(path) for path in paths)
        )
'''

NEW_VERIFY = '''    for (key, value), paths in declarations.items():
        if len(paths) < 2 or key in ALLOWED_IDENTICAL_PROFILE_KEYS:
            continue

        # application-production.yml is deliberately self-contained: it
        # repeats runtime values from application.yml and other profiles.
        # Only those cross-file repetitions are exempt. Two or more non-
        # production declarations still constitute a duplicate.
        non_production_paths = [
            path for path in paths
            if path != BOOTSTRAP_RESOURCES / "application-production.yml"
        ]
        if len(non_production_paths) < 2:
            continue

        errors.append(
            f"duplicate runtime value {key}={value}: "
            + ", ".join(relative(path) for path in non_production_paths)
        )
'''

OLD_DOC = '''## Non-regression

Configuration changes must preserve explicit ownership, stable profile
semantics, no cross-domain configuration consumption, no API-to-mock fallback
and no duplicate competing defaults.

```bash
python scripts/verify_spring_configuration_hygiene.py
```
'''

NEW_DOC = '''## Production runtime configuration consolidation

`backend/bootstrap/src/main/resources/application-production.yml` is the
self-contained runtime configuration for the `production` Spring profile. It
intentionally repeats values also declared in the global `application.yml` and
in other, separately selected profiles or reusable configuration fragments.
This duplication is the consequence of the approved production deployment
composition; it is not by itself a competing runtime default. The production
profile does not rely on `spring.config.import` for those fragments.

This exception concerns **only** identical `(property key, value)` declarations
where one file is `application-production.yml`. It does not permit duplicate
keys inside a YAML/properties file or identical runtime configuration files.
It also does not permit duplicate values shared by two or more *other* runtime
files. Explicit classpath imports must still resolve. All module ownership,
security, feature-flag and profile-safety requirements remain unchanged.

Spring Boot can still load `application.yml` together with a selected profile.
The production consolidation does not imply that global configuration loading
is disabled; deployed values and overrides remain subject to Spring Boot
precedence and must be reviewed when the production profile is configured.

## Non-regression

Configuration changes must preserve explicit ownership, stable profile
semantics, no cross-domain configuration consumption, no API-to-mock fallback
and no duplicate competing defaults outside the narrowly documented production
consolidation exception.

```bash
python scripts/verify_spring_configuration_hygiene.py
```
'''


def decode_source(path: Path) -> tuple[str, str, bool]:
    raw = path.read_bytes()
    if raw.startswith(b'\xef\xbb\xbf'):
        raise ValueError(f'{path}: UTF-8 BOM inattendu, refus de modifier')
    text = raw.decode('utf-8')
    if b'\r\n' in raw and raw.replace(b'\r\n', b'').count(b'\n'):
        raise ValueError(f'{path}: fins de ligne mixtes, refus de modifier')
    newline = '\r\n' if b'\r\n' in raw else '\n'
    return text.replace('\r\n', '\n'), newline, text.endswith('\n')


def prepare(path: Path, before: str, after: str) -> tuple[str, str] | None:
    if not path.is_file():
        raise ValueError(f'fichier absent: {path}')
    current, newline, _ = decode_source(path)
    if after in current and before not in current:
        print(f'DÉJÀ APPLIQUÉ : {path}')
        return None
    occurrences = current.count(before)
    if occurrences != 1:
        raise ValueError(
            f'{path}: ancre attendue non unique (occurrences={occurrences}); '
            'révision locale différente, aucune modification'
        )
    revised = current.replace(before, after, 1)
    return current, revised.replace('\n', newline)


def show_diff(path: Path, before: str, after: str) -> None:
    print(''.join(difflib.unified_diff(
        before.splitlines(keepends=True),
        after.replace('\r\n', '\n').splitlines(keepends=True),
        fromfile=str(path), tofile=str(path) + ' (proposé)',
    )))


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--repo', type=Path, default=Path.cwd(), help='racine du repository local')
    mode = p.add_mutually_exclusive_group(required=True)
    mode.add_argument('--dry-run', action='store_true')
    mode.add_argument('--apply', action='store_true')
    a = p.parse_args()
    repo = a.repo.resolve()
    candidates = [(repo / VERIFY, OLD_VERIFY, NEW_VERIFY), (repo / DOC, OLD_DOC, NEW_DOC)]
    updates = []
    try:
        # Préparer les deux modifications avant toute écriture.
        for path, before, after in candidates:
            item = prepare(path, before, after)
            if item is not None:
                original, revised = item
                updates.append((path, original, revised))
                show_diff(path.relative_to(repo), original, revised)
    except (OSError, UnicodeError, ValueError) as exc:
        print(f'ÉCHEC : {exc}', file=sys.stderr)
        return 1

    if not updates:
        print('AUCUNE MODIFICATION : correctif déjà appliqué.')
        return 0
    if a.dry_run:
        print(f'DRY-RUN : {len(updates)} fichier(s) concerné(s), aucune écriture.')
        return 0

    # Sauvegardes des deux fichiers avant toute modification.
    stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    backup_dir = repo.parent / f'sixpay-production-hygiene-backup-{stamp}'
    try:
        if backup_dir.exists():
            raise FileExistsError(f'dossier de sauvegarde déjà existant: {backup_dir}')
        backup_dir.mkdir(parents=False)
        for path, _, _ in updates:
            dest = backup_dir / path.relative_to(repo)
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, dest)
        for path, _, revised in updates:
            tmp = path.with_name(path.name + '.sixpay-hygiene-tmp')
            if tmp.exists():
                raise FileExistsError(f'fichier temporaire existant: {tmp}')
            try:
                with tmp.open('xb') as handle:
                    handle.write(revised.encode('utf-8'))
                shutil.copymode(path, tmp)
                os.replace(tmp, path)
            finally:
                if tmp.exists():
                    tmp.unlink()
    except Exception as exc:
        # Restaurer tous les fichiers dont la sauvegarde existe.
        for path, _, _ in updates:
            backup = backup_dir / path.relative_to(repo)
            if backup.exists():
                shutil.copy2(backup, path)
        print(f'ÉCHEC / restauration tentée : {exc}', file=sys.stderr)
        return 1
    print(f'APPLIQUÉ : {len(updates)} fichier(s). Sauvegarde : {backup_dir}')
    print('Aucune commande Git ni gate exécuté.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
