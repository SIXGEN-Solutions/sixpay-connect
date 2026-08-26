from collections import defaultdict
from hashlib import sha256
from pathlib import Path
import re
import sys


ROOT = Path.cwd()
BACKEND = ROOT / "backend"
BOOTSTRAP_RESOURCES = BACKEND / "bootstrap/src/main/resources"

CONFIG_SUFFIXES = {".properties", ".yaml", ".yml"}

REQUIRED_RUNTIME_FILES = {
    "application.yml",
    "application-amplitude-payment-sandbox.yml",
    "application-payment-banking.yml",
    "config/payment/tresorpay-common.yml",
    "config/security/local-auth-common.yml",
    "config/security/oidc-common.yml",
}

ALLOWED_IDENTICAL_PROFILE_KEYS = {
    "server.servlet.session.cookie.http-only",
    "sixpay.security.authentication.local.enabled",
    "sixpay.security.authentication.oidc.enabled",
}

YAML_KEY = re.compile(r"^(\s*)([^#\s][^:]*):(?:\s*(.*))?$")
CLASSPATH_IMPORT = re.compile(
    r"^\s*-\s*(?:optional:)?classpath:([^\s#]+)\s*$"
)


def fail(errors):
    print("\nSPRING CONFIGURATION HYGIENE GATE FAILED:\n")
    for error in sorted(set(errors)):
        print(" -", error)
    sys.exit(1)


def relative(path):
    return path.relative_to(ROOT).as_posix()


def main_application_files():
    result = []
    for path in BACKEND.glob("*/src/main/resources/application*"):
        if path.is_file() and path.suffix.lower() in CONFIG_SUFFIXES:
            result.append(path)
    return sorted(result)


def bootstrap_runtime_files():
    return sorted(
        path
        for path in BOOTSTRAP_RESOURCES.rglob("*")
        if path.is_file()
        and path.suffix.lower() in CONFIG_SUFFIXES
        and (
            path.name.startswith("application")
            or "config" in path.relative_to(BOOTSTRAP_RESOURCES).parts
        )
    )


def yaml_leaf_values(path, errors):
    stack = []
    values = []
    seen = set()

    for line_number, line in enumerate(
        path.read_text(encoding="utf-8").splitlines(),
        start=1,
    ):
        if line.lstrip().startswith("- "):
            continue

        match = YAML_KEY.match(line)
        if not match:
            continue

        indent = len(match.group(1).replace("\t", "    "))
        key = match.group(2).strip().strip("'\"")
        value = (match.group(3) or "").strip()

        while stack and stack[-1][0] >= indent:
            stack.pop()

        key_path = ".".join([entry[1] for entry in stack] + [key])

        if key_path in seen:
            errors.append(
                f"duplicate YAML key {key_path}: "
                f"{relative(path)}:{line_number}"
            )
        seen.add(key_path)

        if value:
            values.append((key_path, value))
        else:
            stack.append((indent, key))

    return values


def properties_values(path, errors):
    values = []
    seen = set()

    for line_number, line in enumerate(
        path.read_text(encoding="utf-8").splitlines(),
        start=1,
    ):
        stripped = line.strip()
        if not stripped or stripped.startswith(("#", "!")):
            continue

        separator = "=" if "=" in stripped else ":"
        if separator not in stripped:
            continue

        key, value = [part.strip() for part in stripped.split(separator, 1)]
        if key in seen:
            errors.append(
                f"duplicate properties key {key}: "
                f"{relative(path)}:{line_number}"
            )
        seen.add(key)
        values.append((key, value))

    return values


def configuration_values(path, errors):
    if path.suffix.lower() == ".properties":
        return properties_values(path, errors)
    return yaml_leaf_values(path, errors)


def validate_imports(path, errors):
    if path.suffix.lower() == ".properties":
        return

    stack = []

    for line_number, line in enumerate(
        path.read_text(encoding="utf-8").splitlines(),
        start=1,
    ):
        key_match = YAML_KEY.match(line)
        if key_match and not line.lstrip().startswith("- "):
            indent = len(key_match.group(1).replace("\t", "    "))
            key = key_match.group(2).strip().strip("'\"")

            while stack and stack[-1][0] >= indent:
                stack.pop()

            if not (key_match.group(3) or "").strip():
                stack.append((indent, key))
            continue

        match = CLASSPATH_IMPORT.match(line)
        key_path = ".".join(entry[1] for entry in stack)
        if not match or key_path != "spring.config.import":
            continue

        imported = match.group(1).strip("'\"")
        if "*" in imported:
            continue

        if not (BOOTSTRAP_RESOURCES / imported).is_file():
            errors.append(
                f"unresolved classpath import {imported}: "
                f"{relative(path)}:{line_number}"
            )


def main():
    errors = []
    application_files = main_application_files()
    runtime_files = bootstrap_runtime_files()

    for required in REQUIRED_RUNTIME_FILES:
        if not (BOOTSTRAP_RESOURCES / required).is_file():
            errors.append(f"required runtime configuration is missing: {required}")

    for path in application_files:
        module = path.relative_to(BACKEND).parts[0]
        if module != "bootstrap":
            errors.append(
                "runtime application configuration must be owned by "
                f"bootstrap, found in {relative(path)}"
            )

        if "example" in path.name.lower():
            errors.append(
                f"example configuration is packaged at runtime: {relative(path)}"
            )

    declarations = defaultdict(list)
    content_hashes = defaultdict(list)

    for path in runtime_files:
        validate_imports(path, errors)
        content_hashes[sha256(path.read_bytes()).hexdigest()].append(path)

        for key, value in configuration_values(path, errors):
            if key.startswith("spring.config."):
                continue
            declarations[(key, value)].append(path)

    for paths in content_hashes.values():
        if len(paths) > 1:
            errors.append(
                "identical runtime configuration files: "
                + ", ".join(relative(path) for path in paths)
            )

    for (key, value), paths in declarations.items():
        if len(paths) < 2 or key in ALLOWED_IDENTICAL_PROFILE_KEYS:
            continue
        errors.append(
            f"duplicate runtime value {key}={value}: "
            + ", ".join(relative(path) for path in paths)
        )

    if errors:
        fail(errors)

    print("SPRING CONFIGURATION HYGIENE GATE PASSED.")
    print(
        f"Runtime files: {len(runtime_files)}; "
        f"module application files: {len(application_files)}."
    )
    print("Validated:")
    print(" - Bootstrap is the sole runtime application-config owner")
    print(" - every explicit classpath import resolves")
    print(" - no duplicate key exists inside a configuration file")
    print(" - no unclassified identical runtime value or file remains")


if __name__ == "__main__":
    main()
