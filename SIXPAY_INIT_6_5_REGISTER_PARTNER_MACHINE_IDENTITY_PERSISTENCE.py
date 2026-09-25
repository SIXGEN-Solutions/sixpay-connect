#!/usr/bin/env python3
from pathlib import Path
import subprocess

EXPECTED_HEAD = "9f62dc5c8f43d4a49516272e516b49faceb9ce57"
ROOT = Path(__file__).resolve().parent
REL = "backend/security/src/main/java/com/sixpay/security/configuration/SecurityAdministrationConfiguration.java"

def run(*args):
    return subprocess.check_output(args, cwd=ROOT, text=True).strip()

if run("git", "rev-parse", "HEAD") != EXPECTED_HEAD:
    raise RuntimeError("Unexpected HEAD")

p = ROOT / REL
s = p.read_text(encoding="utf-8")

entity_import = "import com.sixpay.security.infrastructure.authentication.machine.PartnerMachineIdentityJpaEntity;\n"
repo_import = "import com.sixpay.security.infrastructure.authentication.machine.PartnerMachineIdentitySpringDataRepository;\n"

if entity_import not in s:
    marker = "import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;"
    if marker not in s:
        raise RuntimeError("Import insertion anchor not found")
    s = s.replace(marker, entity_import + repo_import + marker, 1)
elif repo_import not in s:
    s = s.replace(entity_import, entity_import + repo_import, 1)

old_entity = """        AuthenticationAuditJpaEntity.class, SecurityAuditJpaEntity.class
})"""
new_entity = """        AuthenticationAuditJpaEntity.class, SecurityAuditJpaEntity.class,
        PartnerMachineIdentityJpaEntity.class
})"""
if new_entity not in s:
    if old_entity not in s:
        raise RuntimeError("EntityScan anchor not found")
    s = s.replace(old_entity, new_entity, 1)

old_repo = """        AuthenticationAuditSpringDataRepository.class, SecurityAuditSpringDataRepository.class
})"""
new_repo = """        AuthenticationAuditSpringDataRepository.class, SecurityAuditSpringDataRepository.class,
        PartnerMachineIdentitySpringDataRepository.class
})"""
if new_repo not in s:
    if old_repo not in s:
        raise RuntimeError("EnableJpaRepositories anchor not found")
    s = s.replace(old_repo, new_repo, 1)

p.write_text(s, encoding="utf-8", newline="\n")
print("UPDATED", REL)
print("Registered PartnerMachineIdentity JPA entity and repository in the existing Security persistence configuration.")
print("No gate, commit, push, PR, branch creation, or worktree check performed.")
