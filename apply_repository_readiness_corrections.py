#!/usr/bin/env python3
"""Apply safe repository-readiness corrections for SIXPAY CONNECT.

Run this script from the repository root. It is idempotent and does not run
Git commands, create commits, or push anything.
"""

from __future__ import annotations

import json
import shutil
import tempfile
from pathlib import Path


ROOT = Path.cwd()
BACKUP_ROOT = Path(tempfile.mkdtemp(prefix="sixpay-readiness-backup-"))

FILES_TO_REMOVE = (
    Path("backend/README.md"),
    Path("backend/sixpay-parent.iml"),
    Path("backend/tests/pom.xml.patch"),
)

OBSOLETE_FRONTEND_SCRIPTS = {
    "start:netlify",
    "test:e2e:customer-fullstack",
    "gate:3",
    "gate:4",
    "gate:5",
    "gate:6",
    "gate:7",
    "gate:8",
}

CANONICAL_FRONTEND_SCRIPTS = {
    "verify:e2e": "npm run test:e2e && npm run test:e2e:integration",
    "verify:quality": (
        "npm run format:check && npm run contract:partner && npm run "
        "lint && npm run test:coverage && npm run build:all"
    ),
    "verify:ci": (
        "npm run verify:quality && npm run dependencies:audit && "
        "npm run verify:e2e"
    ),
    "verify:sixpay": "npm run verify:quality",
}


def require_repository() -> None:
    required = (
        Path("ENGINEERING_CONTEXT.md"),
        Path("backend/pom.xml"),
        Path("frontend/package.json"),
    )
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise SystemExit(
            "Erreur: exécute ce script depuis la racine du dépôt SIXPAY CONNECT. "
            f"Fichiers absents: {', '.join(missing)}"
        )


def backup(path: Path) -> None:
    source = ROOT / path
    if not source.exists():
        return
    target = BACKUP_ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)


def remove_obsolete_files() -> list[str]:
    removed = []
    for relative in FILES_TO_REMOVE:
        path = ROOT / relative
        if path.is_file():
            backup(relative)
            path.unlink()
            removed.append(str(relative))
    return removed


def update_backend_matrix() -> bool:
    relative = Path("backend/SIXPAY_BACKEND_TECHNOLOGY_MATRIX.md")
    path = ROOT / relative
    source = path.read_text(encoding="utf-8")
    old = (
        "- aux modules métier `customer`, `partner`, `subscription`, "
        "`payment`, `accounting`, `reporting`, `notification` et "
        "`administration` ;"
    )
    new = (
        "- aux modules métier `customer`, `partner`, `payment`, `accounting`, "
        "`reporting`, `notification` et `administration` ;\n"
        "- la capacité de souscription Customer-owned, implémentée dans "
        "`customer` ;"
    )
    if old not in source:
        return False
    backup(relative)
    path.write_text(source.replace(old, new, 1), encoding="utf-8")
    return True


def update_documentation_gate_reference() -> bool:
    relative = Path("documentation/architecture/TESTS_AND_GATES.md")
    path = ROOT / relative
    source = path.read_text(encoding="utf-8")
    if "npm run verify:sixpay" in source:
        return False
    if "npm run verify:quality" not in source:
        raise SystemExit(
            f"Erreur: aucune référence connue trouvée dans {relative}; "
            "mise à jour manuelle nécessaire."
        )
    backup(relative)
    path.write_text(
        source.replace("npm run verify:quality", "npm run verify:sixpay", 1),
        encoding="utf-8",
    )
    return True


def update_frontend_package() -> tuple[list[str], int]:
    relative = Path("frontend/package.json")
    path = ROOT / relative
    package = json.loads(path.read_text(encoding="utf-8"))
    scripts = package.get("scripts")
    if not isinstance(scripts, dict):
        raise SystemExit("Erreur: frontend/package.json ne contient pas scripts.")

    removed = sorted(name for name in OBSOLETE_FRONTEND_SCRIPTS if name in scripts)
    for name in OBSOLETE_FRONTEND_SCRIPTS:
        scripts.pop(name, None)
    scripts.update(CANONICAL_FRONTEND_SCRIPTS)

    for name, command in scripts.items():
        for token in command.split("npm run ")[1:]:
            referenced = token.split()[0]
            if referenced not in scripts:
                raise SystemExit(
                    f"Erreur: le script '{name}' référence '{referenced}', "
                    "qui n'existe pas."
                )

    backup(relative)
    path.write_text(
        json.dumps(package, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    return removed, len(scripts)


def main() -> int:
    require_repository()
    removed_files = remove_obsolete_files()
    matrix_updated = update_backend_matrix()
    docs_updated = update_documentation_gate_reference()
    removed_scripts, script_count = update_frontend_package()

    print("Corrections appliquées localement; aucun commit ni push effectué.")
    print(
        "Fichiers supprimés: "
        + (", ".join(removed_files) if removed_files else "aucun")
    )
    print(
        "Scripts frontend supprimés: "
        + (", ".join(removed_scripts) if removed_scripts else "aucun")
    )
    print(f"Scripts frontend conservés: {script_count}")
    print(f"Matrice backend corrigée: {'oui' if matrix_updated else 'déjà alignée'}")
    print(
        "Gate documentaire corrigé: "
        + ("oui" if docs_updated else "déjà aligné")
    )
    print(f"Sauvegarde temporaire: {BACKUP_ROOT}")
    print(
        "Point restant volontairement non automatisé: créer et valider le contrat "
        "customer-subscription-management-api-v1.yaml."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
