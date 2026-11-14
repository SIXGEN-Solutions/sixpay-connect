#!/usr/bin/env python3
"""SIXPAY CONNECT: deterministic Customer Banking port bean alignment.

Reference: SIXGEN-Solutions/sixpay-connect
Branch: feat/repository-baseline-consolidation-cucumber
Reference SHA: ae6d04109f6a0ab57016b1a5bfd0f7c7a8618c89

Scope: two Spring configuration classes and one new wiring test.
No worktree cleanliness checks, Maven gates, commits, pushes, or PRs.
"""
from __future__ import annotations

import argparse
import difflib
import subprocess
import sys
from pathlib import Path

REFERENCE_SHA = "ae6d04109f6a0ab57016b1a5bfd0f7c7a8618c89"
BRANCH = "feat/repository-baseline-consolidation-cucumber"
FALLBACK_PATH = Path(
    "backend/customer/src/main/java/com/sixpay/customer/verification/configuration/"
    "CustomerVerificationApplicationConfiguration.java"
)
REAL_PATH = Path(
    "backend/customer/src/main/java/com/sixpay/customer/verification/infrastructure/banking/configuration/"
    "AmplitudeCustomerVerificationConfiguration.java"
)
TEST_PATH = Path(
    "backend/customer/src/test/java/com/sixpay/customer/verification/infrastructure/banking/configuration/"
    "CustomerBankingPortWiringConfigurationTest.java"
)
CONDITION = '''    @ConditionalOnProperty(
            prefix = "sixpay.customer.verification.banking",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true
    )
'''
TEST_CONTENT = '''package com.sixpay.customer.verification.infrastructure.banking.configuration;

import com.sixpay.customer.configuration.CustomerCapabilityUnavailableException;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.management.infrastructure.banking.amplitude.AmplitudeCustomerLookupAdapter;
import com.sixpay.customer.verification.application.port.output.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.configuration.CustomerVerificationApplicationConfiguration;
import com.sixpay.customer.verification.infrastructure.banking.retry.RetryingBankingCustomerVerificationAdapter;
import com.sixpay.integration.http.StandardRestClientFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerBankingPortWiringConfigurationTest {

    private static final String PREFIX = "sixpay.customer.verification.banking.";

    private ApplicationContextRunner runner(boolean reverseOrder) {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withBean(StandardRestClientFactory.class,
                        () -> new StandardRestClientFactory(RestClient.builder()))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(ObjectMapper.class, ObjectMapper::new);
        return reverseOrder
                ? runner.withUserConfiguration(
                        AmplitudeCustomerVerificationConfiguration.class,
                        CustomerVerificationApplicationConfiguration.class)
                : runner.withUserConfiguration(
                        CustomerVerificationApplicationConfiguration.class,
                        AmplitudeCustomerVerificationConfiguration.class);
    }

    private static String[] validEnabledConfiguration() {
        return new String[] {
                PREFIX + "enabled=true",
                PREFIX + "base-url=http://127.0.0.1:9092",
                PREFIX + "endpoint-path=/api/v1/customer-verifications",
                PREFIX + "connect-timeout=2s",
                PREFIX + "read-timeout=5s",
                PREFIX + "max-attempts=3",
                PREFIX + "retry-backoff=250ms",
                PREFIX + "evidence-ttl=5m",
                PREFIX + "contract.version=1.0.0",
                PREFIX + "contract.success-codes[0]=00",
                PREFIX + "contract.business-failure-codes[0]=01"
        };
    }

    @Test
    void missingPropertyUsesOnlyFailClosedPorts() {
        runner(false).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeansOfType(BankingCustomerLookupPort.class)).hasSize(1);
            assertThat(context.getBeansOfType(BankingCustomerVerificationPort.class)).hasSize(1);
            assertThat(context).hasBean("unavailableBankingCustomerLookupPort");
            assertThat(context).hasBean("unavailableBankingCustomerVerificationPort");
            assertThat(context).doesNotHaveBean("bankingCustomerLookupPort");
            assertThat(context).doesNotHaveBean("bankingCustomerVerificationPort");
            assertLookupFailsClosed(context.getBean(BankingCustomerLookupPort.class));
        });
    }

    @Test
    void explicitlyDisabledPropertyUsesOnlyFailClosedPorts() {
        runner(false).withPropertyValues(PREFIX + "enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(BankingCustomerLookupPort.class)).hasSize(1);
                    assertThat(context.getBeansOfType(BankingCustomerVerificationPort.class)).hasSize(1);
                    assertThat(context).hasBean("unavailableBankingCustomerLookupPort");
                    assertThat(context).hasBean("unavailableBankingCustomerVerificationPort");
                    assertLookupFailsClosed(context.getBean(BankingCustomerLookupPort.class));
                });
    }

    @Test
    void enabledPropertyUsesOnlyRealPortsRegardlessOfConfigurationOrder() {
        for (boolean reverseOrder : new boolean[] {false, true}) {
            runner(reverseOrder).withPropertyValues(validEnabledConfiguration())
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBeansOfType(BankingCustomerLookupPort.class))
                                .hasSize(1);
                        assertThat(context.getBeansOfType(BankingCustomerVerificationPort.class))
                                .hasSize(1);
                        assertThat(context.getBean(BankingCustomerLookupPort.class))
                                .isInstanceOf(AmplitudeCustomerLookupAdapter.class);
                        assertThat(context.getBean(BankingCustomerVerificationPort.class))
                                .isInstanceOf(RetryingBankingCustomerVerificationAdapter.class);
                        assertThat(context).doesNotHaveBean("unavailableBankingCustomerLookupPort");
                        assertThat(context).doesNotHaveBean("unavailableBankingCustomerVerificationPort");
                    });
        }
    }

    @Test
    void enabledButIncompleteBankingConfigurationFailsStartup() {
        runner(false).withPropertyValues(PREFIX + "enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    private static void assertLookupFailsClosed(BankingCustomerLookupPort port) {
        assertThatThrownBy(() -> port.lookup(
                new BankingCustomerLookupPort.BankingCustomerLookupQuery(
                        "SIXPAY_BANK", "NIU-001", null, "ACC-001", "corr-001")))
                .isInstanceOf(CustomerCapabilityUnavailableException.class)
                .hasMessageContaining("banking lookup");
    }
}
'''


class PatchError(RuntimeError):
    pass


def run_git(root: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", *args], cwd=root, capture_output=True, text=True, check=False
    )
    if result.returncode:
        raise PatchError(f"git {' '.join(args)} failed: {result.stderr.strip()}")
    return result.stdout.strip()


def check_revision(root: Path, skip: bool) -> None:
    if skip:
        print("[REVISION] Git revision verification explicitly skipped.")
        return
    branch = run_git(root, "branch", "--show-current")
    head = run_git(root, "rev-parse", "HEAD")
    if branch != BRANCH:
        raise PatchError(f"Wrong branch: {branch!r}; expected {BRANCH!r}")
    # SHA is a baseline, not a mandatory exact HEAD. No worktree status is queried.
    result = subprocess.run(
        ["git", "merge-base", "--is-ancestor", REFERENCE_SHA, "HEAD"],
        cwd=root, capture_output=True, text=True, check=False,
    )
    if result.returncode != 0:
        raise PatchError(
            f"Reference SHA {REFERENCE_SHA} must be an ancestor of HEAD {head}. "
            "Checkout the selected revision before applying."
        )
    print(f"[REVISION] Branch: {branch}")
    print(f"[REVISION] HEAD: {head}")
    print(f"[REVISION] Required ancestor: {REFERENCE_SHA} [OK]")


def exactly_one(contents: str, old: str, new: str, name: str) -> str:
    count = contents.count(old)
    if count != 1:
        raise PatchError(f"{name}: expected exactly one anchor, found {count}")
    return contents.replace(old, new, 1)


def patch_fallback(before: str) -> str:
    after = before
    after = exactly_one(
        after,
        "import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;\n",
        "import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;\n"
        "import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;\n",
        "fallback property import",
    )
    for port in ["BankingCustomerVerificationPort", "BankingCustomerLookupPort"]:
        old = f"    @Bean\n    @ConditionalOnMissingBean({port}.class)\n"
        new = f"    @Bean\n{CONDITION}    @ConditionalOnMissingBean({port}.class)\n"
        after = exactly_one(after, old, new, f"fallback {port} condition")
    # The lookup adapter exists on the selected revision; remove obsolete documentation.
    after = exactly_one(
        after,
        "     * <p>No bank-approved runtime implementation of the Customer lookup\n"
        "     * contract exists on the authoritative branch yet.</p>\n",
        "     * <p>Used only when Customer banking is disabled or not configured.\n"
        "     * An enabled integration uses the real lookup adapter.</p>\n",
        "stale fallback documentation",
    )
    return after


def patch_real(before: str) -> str:
    after = before
    for port in ["BankingCustomerVerificationPort", "BankingCustomerLookupPort"]:
        # Match only these bean declarations, not unrelated @Primary annotations.
        old = f"    @Bean\n    @Primary\n    {port} "
        new = f"    @Bean\n    {port} "
        after = exactly_one(after, old, new, f"real {port} primary removal")
    return after


def unified(path: Path, before: str, after: str) -> str:
    return "".join(difflib.unified_diff(
        before.splitlines(keepends=True), after.splitlines(keepends=True),
        fromfile=f"a/{path.as_posix()}", tofile=f"b/{path.as_posix()}",
    ))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path("."), help="Repository root")
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--dry-run", action="store_true", help="Preview patch, never write")
    modes.add_argument("--apply", action="store_true", help="Apply this exact patch locally")
    parser.add_argument(
        "--skip-revision-check", action="store_true",
        help="Skip Git branch/SHA ancestry verification; never checks worktree status either way",
    )
    args = parser.parse_args()
    root = args.root.resolve()
    try:
        if not root.is_dir():
            raise PatchError(f"Repository root does not exist: {root}")
        check_revision(root, args.skip_revision_check)

        old_new: list[tuple[Path, str, str]] = []
        for path, transform in [(FALLBACK_PATH, patch_fallback), (REAL_PATH, patch_real)]:
            full = root / path
            if not full.is_file():
                raise PatchError(f"Missing file: {path}")
            old = full.read_bytes().decode("utf-8")
            if "\r\n" in old and "\n" in old.replace("\r\n", ""):
                raise PatchError(f"Mixed line endings are unsupported: {path}")
            newline = "\r\n" if "\r\n" in old else "\n"
            new = transform(old.replace("\r\n", "\n"))
            if newline == "\r\n":
                new = new.replace("\n", "\r\n")
            if old == new:
                raise PatchError(f"No expected change for {path}")
            old_new.append((path, old, new))

        test_full = root / TEST_PATH
        if test_full.exists():
            raise PatchError(f"Refusing to replace existing test file: {TEST_PATH}")
        old_new.append((TEST_PATH, "", TEST_CONTENT))

        print(f"[MODE] {'DRY-RUN' if args.dry_run else 'APPLY'}")
        print("[SCOPE] Customer Banking bean wiring only; no HTTP error handling changes")
        for path, before, after in old_new:
            print(f"\n--- {path} ---")
            print(unified(path, before, after), end="")

        if args.dry_run:
            print("\n[DRY-RUN] No files written.")
            return 0

        # Preflight completed for every target: write only after all anchors validate.
        for path, _, after in old_new:
            full = root / path
            full.parent.mkdir(parents=True, exist_ok=True)
            full.write_bytes(after.encode("utf-8"))
            print(f"[APPLIED] {path}")
        print("[DONE] Files updated locally. No gates, commits, pushes, or PRs executed.")
        return 0
    except (PatchError, OSError, UnicodeError) as exc:
        print(f"[ERROR] {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
