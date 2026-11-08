#!/usr/bin/env python3
"""SIXPAY R6: local-only additive T0 validation tests and evidence report.

Base: feat/repository-baseline-consolidation-cucumber @
      d9c2ec23c4dd0f16d9a833b7985a3cd82a18394b

No Git operations, Maven, npm, Docker or remote calls. Standard library only.
"""
from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import shutil
import sys
import tempfile

ROOT_FILES = {
    'backend/payment/src/test/java/com/sixpay/payment/infrastructure/recovery/PaymentT0ManualReconciliationServiceTest.java': '7f09bbe1a4b9901ce70822f64270b49db9a7d228',
    'backend/payment/src/test/java/com/sixpay/payment/infrastructure/recovery/PaymentT0RecoveryClaimStoreIT.java': '95071390c08cf705d93ecc33410174a8315f1846',
    'backend/payment/src/test/java/com/sixpay/payment/infrastructure/banking/amplitude/posting/client/RestAmplitudePaymentEventRecoveryClientTest.java': 'efd047f2cc2e7db1607b591333419cff311796f1',
    'backend/payment/src/test/java/com/sixpay/payment/infrastructure/callback/relay/PaymentCallbackPlanFactoryTest.java': '1a95558be532099cb85236548d3e051704a0050e',
    'backend/payment/src/test/java/com/sixpay/payment/domain/model/PaymentAtomicEventLifecycleTest.java': 'edcbfee731e3e84234ac16fefb64859e594c8809',
}
REPORT_PATH = 'documentation/ai/payment/PAYMENT_T0_R6_VALIDATION_REPORT.md'
START = '\n    // BEGIN SIXPAY_R6_T0_VALIDATION_TESTS\n'
END = '    // END SIXPAY_R6_T0_VALIDATION_TESTS\n'

MANUAL = '''    @Test
    void r6ConclusiveCompletedBankLookupResolvesOwnedClaim() {
        var fixture = r6Fixture();
        when(fixture.recovery().recoverByPaymentId(eq(new PaymentId(PAYMENT)), any()))
                .thenReturn(r6Result(com.sixpay.payment.domain.model.PaymentStatus.POSTED_PENDING_TFJ));
        when(fixture.claims().completed(fixture.claim(), NOW)).thenReturn(true);

        var results = fixture.service().reconcile(List.of(PAYMENT), CORRELATION);

        assertEquals(1, results.size());
        assertEquals("RESOLVED", results.getFirst().outcome());
        assertEquals("POSTED_PENDING_TFJ", results.getFirst().paymentStatus());
        verify(fixture.claims()).completed(fixture.claim(), NOW);
        verify(fixture.claims(), never()).retry(any(), any(), any());
        verify(fixture.audit()).record(PAYMENT, "r6-admin", CORRELATION, "RESOLVED", NOW);
    }

    @Test
    void r6ConclusiveRejectedBankLookupResolvesOwnedClaimWithoutRetry() {
        var fixture = r6Fixture();
        when(fixture.recovery().recoverByPaymentId(eq(new PaymentId(PAYMENT)), any()))
                .thenReturn(r6Result(com.sixpay.payment.domain.model.PaymentStatus.REJECTED));
        when(fixture.claims().completed(fixture.claim(), NOW)).thenReturn(true);

        var results = fixture.service().reconcile(List.of(PAYMENT), CORRELATION);

        assertEquals("RESOLVED", results.getFirst().outcome());
        assertEquals("REJECTED", results.getFirst().paymentStatus());
        verify(fixture.claims()).completed(fixture.claim(), NOW);
        verify(fixture.claims(), never()).retry(any(), any(), any());
        verify(fixture.audit()).record(PAYMENT, "r6-admin", CORRELATION, "RESOLVED", NOW);
    }

    @Test
    void r6StillUnknownBankLookupKeepsTheAttemptRetryable() {
        var fixture = r6Fixture();
        when(fixture.recovery().recoverByPaymentId(eq(new PaymentId(PAYMENT)), any()))
                .thenReturn(r6Result(com.sixpay.payment.domain.model.PaymentStatus.POSTING_OUTCOME_UNKNOWN));
        when(fixture.claims().retry(eq(fixture.claim()), eq(NOW), any())).thenReturn(true);

        var results = fixture.service().reconcile(List.of(PAYMENT), CORRELATION);

        assertEquals("RETRY_SCHEDULED", results.getFirst().outcome());
        assertEquals("POSTING_OUTCOME_UNKNOWN", results.getFirst().paymentStatus());
        verify(fixture.claims(), never()).completed(any(), any());
        verify(fixture.claims()).retry(fixture.claim(), NOW,
                java.time.Duration.ofMinutes(2));
    }

    @Test
    void r6UnavailableBankLookupSchedulesRetryWithoutFalseRejection() {
        var fixture = r6Fixture();
        when(fixture.recovery().recoverByPaymentId(eq(new PaymentId(PAYMENT)), any()))
                .thenThrow(new IllegalStateException("Core Banking unavailable"));
        when(fixture.claims().retry(eq(fixture.claim()), eq(NOW), any())).thenReturn(true);

        var results = fixture.service().reconcile(List.of(PAYMENT), CORRELATION);

        assertEquals("LOOKUP_FAILED_RETRY_SCHEDULED", results.getFirst().outcome());
        assertNull(results.getFirst().paymentStatus());
        verify(fixture.claims(), never()).completed(any(), any());
        verify(fixture.audit()).record(PAYMENT, "r6-admin", CORRELATION,
                "LOOKUP_FAILED_RETRY_SCHEDULED", NOW);
    }

    @Test
    void r6OldFencingTokenCannotReportSuccessfulResolution() {
        var fixture = r6Fixture();
        when(fixture.recovery().recoverByPaymentId(eq(new PaymentId(PAYMENT)), any()))
                .thenReturn(r6Result(com.sixpay.payment.domain.model.PaymentStatus.REJECTED));
        when(fixture.claims().completed(fixture.claim(), NOW)).thenReturn(false);

        var results = fixture.service().reconcile(List.of(PAYMENT), CORRELATION);

        assertEquals("STALE_CLAIM", results.getFirst().outcome());
        verify(fixture.audit()).record(PAYMENT, "r6-admin", CORRELATION, "STALE_CLAIM", NOW);
    }

    private static com.sixpay.payment.application.service.PaymentWorkflowResult r6Result(
            com.sixpay.payment.domain.model.PaymentStatus status) {
        return new com.sixpay.payment.application.service.PaymentWorkflowResult(
                new PaymentId(PAYMENT),
                com.sixpay.payment.domain.model.PublicPaymentReference.of(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"), status, 2, true);
    }

    private record R6Fixture(
            PaymentT0ManualReconciliationService service,
            PaymentT0RecoveryClaimStore claims,
            RecoverUnknownPaymentT0UseCase recovery,
            PaymentT0ManualAuditStore audit,
            PaymentT0RecoveryClaimStore.Claim claim) { }

    private static R6Fixture r6Fixture() {
        var claims = mock(PaymentT0RecoveryClaimStore.class);
        var recovery = mock(RecoverUnknownPaymentT0UseCase.class);
        var access = mock(PaymentObjectAccessPort.class);
        var policy = mock(PaymentAccessPolicy.class);
        var users = mock(CurrentUserProvider.class);
        var actor = mock(AuthenticatedUser.class);
        var audit = mock(PaymentT0ManualAuditStore.class);
        var time = mock(TimeProvider.class);
        var claim = new PaymentT0RecoveryClaimStore.Claim(PAYMENT,
                UUID.fromString("33333333-3333-4333-8333-333333333333"), 1);
        when(users.requireCurrentUser()).thenReturn(actor);
        when(actor.subject()).thenReturn("r6-admin");
        when(time.now()).thenReturn(NOW);
        when(access.findAccessDescriptor(new PaymentId(PAYMENT)))
                .thenReturn(Optional.of(new PaymentObjectAccessDescriptor(
                        new PaymentId(PAYMENT), PaymentSource.TRESOR_PAY, null)));
        when(claims.claimSelected(eq(PAYMENT), eq(NOW), any(), startsWith("manual:")))
                .thenReturn(Optional.of(claim));
        var service = new PaymentT0ManualReconciliationService(claims,
                new PaymentT0RecoveryBatchProperties(), recovery, access, policy, users,
                audit, time);
        return new R6Fixture(service, claims, recovery, audit, claim);
    }
'''

CLAIMS = '''    @Test
    void r6ManualSelectionRejectsOldPostingDespiteRecentPaymentUpdate() {
        UUID paymentId = UUID.fromString("33333333-3333-4333-8333-333333333333");
        insertPayment(paymentId, NOW.minus(Duration.ofHours(49)), "POSTING_OUTCOME_UNKNOWN");
        // insertPayment stores payments.updated_at = NOW; the original posting audit wins.
        assertTrue(store.claimSelected(paymentId, NOW, props("manual"),
                "manual:r6-admin").isEmpty());
        Integer attempts = jdbc.queryForObject(
                "SELECT count(*) FROM payment_t0_recovery_attempts WHERE payment_id = ?",
                Integer.class, paymentId);
        assertEquals(0, attempts);
    }

    @Test
    void r6ManualSelectionRejectsPaymentBeforeMinimumPostingAge() {
        UUID paymentId = UUID.fromString("44444444-4444-4444-8444-444444444444");
        insertPayment(paymentId, NOW.minus(Duration.ofMinutes(1)), "POSTING_PENDING");
        assertTrue(store.claimSelected(paymentId, NOW, props("manual"),
                "manual:r6-admin").isEmpty());
    }
'''

HTTP = '''    @Test
    void r6Http404OnPaymentReferenceIsInconclusiveAndReturnsEmpty() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var client = client(builder);
        server.expect(requestTo("http://localhost/api/v1/payment-events/"
                        + "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        assertTrue(client.findByPaymentReference(
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                "11111111-1111-4111-8111-111111111111", "LRB").isEmpty());
        server.verify();
    }

    @Test
    void r6Http503OnBankLookupIsUnknownNotAConclusiveRejection() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var client = client(builder);
        server.expect(requestTo("http://localhost/api/v1/payment-events/"
                        + "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));

        org.junit.jupiter.api.Assertions.assertThrows(
                com.sixpay.payment.infrastructure.banking.amplitude.posting.error
                        .AmplitudePaymentEventOutcomeUnknownException.class,
                () -> client.findByPaymentReference(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                        "11111111-1111-4111-8111-111111111111", "LRB"));
        server.verify();
    }
'''

CALLBACK = '''    @Test
    void r6RejectedT0EventCurrentlyHasNoCallbackPlanMapping() throws Exception {
        // Evidence of the current implementation, NOT acceptance of missing rejection delivery.
        // Contract-level decision / integrated delivery proof is tracked in R6 report.
        var method = PaymentCallbackPlanFactory.class
                .getDeclaredMethod("callbackType", PaymentAuditEntry.class);
        method.setAccessible(true);
        PaymentAuditEntry rejected = audit(
                PaymentEventOutcomeRecorded.class.getSimpleName(),
                PaymentStatus.REJECTED);
        assertThat(method.invoke(null, rejected)).isNull();
    }
'''

DOMAIN = '''    @Test
    void r6AuthoritativeRejectedLookupResolvesUnknownToRejected() {
        Payment payment = PaymentAggregateTestFixtures.approvedPayment();
        PostingInstructionIdentity instruction = PaymentAggregateTestFixtures.postingInstruction();
        payment.authorizePaymentEventPosting(
                instruction, PaymentAggregateTestFixtures.T0.plusSeconds(6));
        payment.recordPaymentEventOutcome(
                outcome(instruction, PaymentEventOutcome.UNKNOWN,
                        PaymentEventObservationSource.DIRECT_RESPONSE, false),
                PaymentAggregateTestFixtures.unknownFailure(
                        "POSTING_OUTCOME_UNKNOWN", FailureStage.POSTING,
                        PaymentAggregateTestFixtures.T0.plusSeconds(7)),
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                PaymentAggregateTestFixtures.profiles());

        payment.resolvePaymentEventOutcome(
                outcome(instruction, PaymentEventOutcome.REJECTED,
                        PaymentEventObservationSource.IDEMPOTENCY_LOOKUP, false),
                PaymentAggregateTestFixtures.businessFailure(
                        "BANK_REJECTED", FailureStage.POSTING_RESOLUTION,
                        PaymentAggregateTestFixtures.T0.plusSeconds(8)),
                PaymentAggregateTestFixtures.T0.plusSeconds(8),
                PaymentAggregateTestFixtures.profiles());

        assertEquals(PaymentStatus.REJECTED, payment.status());
        assertEquals(PaymentEventOutcome.REJECTED,
                payment.toState().paymentEventOutcomeEvidence().orElseThrow().outcome());
    }
'''

INSERTIONS = {
    list(ROOT_FILES)[0]: MANUAL,
    list(ROOT_FILES)[1]: CLAIMS,
    list(ROOT_FILES)[2]: HTTP,
    list(ROOT_FILES)[3]: CALLBACK,
    list(ROOT_FILES)[4]: DOMAIN,
}

REPORT = '''# SIXPAY CONNECT — R6 — Matrice de validation intégrée Payment T0

- Repository : `SIXGEN-Solutions/sixpay-connect`
- Révision documentaire examinée : `feat/repository-baseline-consolidation-cucumber` @ `d9c2ec23c4dd0f16d9a833b7985a3cd82a18394b`
- Statut des preuves dynamiques : **NON EXÉCUTÉES par le préparateur**.
- Périmètre : récupération T0 bancaire, claims R2/R3, notification après résolution et régression.
- Hors périmètre : Accounting T1, modification de contrat, migration, API, permissions ou débit bancaire.

## Autorité contractuelle conservée

- `payment-t0-manual-reconciliation-api-v1` : `ACTIVE_MVP / APPROVED / ACTIVE / codeGenerationAllowed=true`.
- `payment-consultation-api-v1` : `ACTIVE_MVP / PENDING_APPROVAL / REFERENCE_ONLY / codeGenerationAllowed=false`.
- `tresorpay-payment-status-webhook-v1` : `ACTIVE_MVP / APPROVED / REFERENCE_ONLY / codeGenerationAllowed=false`.
- Ces statuts sont constatés dans le registre à la révision cible, sans changement R6.

## Matrice et traces attendues

| Cas | Source / test | Assertion ou preuve recherchée | Résultat local |
|---|---|---|---|
| UNKNOWN → COMPLETED | `PaymentT0ManualReconciliationServiceTest.r6ConclusiveCompletedBankLookupResolvesOwnedClaim` et tests T0 métier | `POSTED_PENDING_TFJ`, claim clôturé, audit RESOLVED, aucun deuxième POST bancaire | NON EXÉCUTÉ |
| UNKNOWN → REJECTED | `PaymentAtomicEventLifecycleTest.r6AuthoritativeRejectedLookupResolvesUnknownToRejected` + `...r6ConclusiveRejectedBankLookupResolvesOwnedClaimWithoutRetry` | `REJECTED`, claim clôturé, pas de second POST, transition persistée à confirmer en intégration | NON EXÉCUTÉ |
| GET bancaire 404 | `RestAmplitudePaymentEventRecoveryClientTest.r6Http404OnPaymentReferenceIsInconclusiveAndReturnsEmpty` ; fallback testé dans adapter | 404 = absence de résultat, non une preuve de rejet ; deuxième GET d'idempotence si pertinent | NON EXÉCUTÉ |
| GET bancaire 503 | `...r6Http503OnBankLookupIsUnknownNotAConclusiveRejection` + `...r6UnavailableBankLookupSchedulesRetryWithoutFalseRejection` | erreur technique incertaine, claim reprogrammé, pas de rejet artificiel | NON EXÉCUTÉ |
| UNKNOWN toujours inconnu | `...r6StillUnknownBankLookupKeepsTheAttemptRetryable` | RETRY_SCHEDULED, délai 2 minutes, claim non clôturé | NON EXÉCUTÉ |
| Concurrence opérateur/scheduler | `PaymentT0RecoveryClaimStoreIT.manualClaimExcludesSchedulerAndHonoursFencing` ; `twoOperatorsCannotClaimTheSamePayment` | un seul token possédé, fencing, aucune double acquisition | NON EXÉCUTÉ |
| Claim expiré / token obsolète | `PaymentT0RecoveryClaimStoreIT.staleLeaseReclaimedAndOldTokenCannotComplete` + `...r6OldFencingTokenCannotReportSuccessfulResolution` | ancien propriétaire ne clôture pas et ne prétend pas être RESOLVED | NON EXÉCUTÉ |
| Hors fenêtre | `PaymentT0RecoveryClaimStoreIT.r6ManualSelectionRejectsOldPostingDespiteRecentPaymentUpdate` | aucun claim après 49 h malgré `updated_at` récent | NON EXÉCUTÉ |
| Trop récent | `PaymentT0RecoveryClaimStoreIT.r6ManualSelectionRejectsPaymentBeforeMinimumPostingAge` | aucun claim pendant l'âge minimum de 2 minutes | NON EXÉCUTÉ |
| Callback COMPLETED | `PaymentCallbackPlanFactoryTest.cutCreditedIsBoundToDurableCompletedT0OutcomeEvent` ; `PaymentCallbackOutboxRelayTest` | événement métier durable → CUT_CREDITED, livraison et retry contrôlés | NON EXÉCUTÉ |
| Callback REJECTED | `PaymentCallbackPlanFactoryTest.r6RejectedT0EventCurrentlyHasNoCallbackPlanMapping` | **ÉCART STATIQUE : absence de mapping pour REJECTED dans la fabrique callback** | NON EXÉCUTÉ |

## Attention : preuve intégrée et blocage

Ces tests de composants n'établissent PAS à eux seuls un parcours complet :
banque simulée → mutation Payment persistée → outbox durable → callback HTTP réel → consultation partenaire.
Ce parcours doit être validé localement avec une intégration appropriée, y compris PostgreSQL/Testcontainers et récepteur callback, après vérification de ses fixtures.

L'implémentation du callback `REJECTED` n'est pas démontrée dans
`PaymentCallbackPlanFactory`. Le contrat webhook est approuvé et protégé :
**aucune modification de payload, de mapping de notification ou de contrat n'est appliquée par R6**.
Décision humaine requise pour valider le traitement attendu de la notification
après résolution T0 rejetée et son périmètre exact, avant correction de production.

## Commandes à exécuter localement (depuis `backend`)

```powershell
mvn -pl payment -am "-Dtest=PaymentT0ManualReconciliationServiceTest,PaymentT0RecoveryWorkerTest,PaymentT0RecoveryServiceTest,PaymentAtomicEventLifecycleTest,AmplitudePaymentEventRecoveryAdapterTest,RestAmplitudePaymentEventRecoveryClientTest,PaymentCallbackPlanFactoryTest,PaymentCallbackOutboxRelayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl payment -am -Pfull-tests "-Dit.test=PaymentT0RecoveryClaimStoreIT" "-Dfailsafe.failIfNoSpecifiedTests=false" verify
mvn -pl payment -am verify
mvn verify
```

Contrôler explicitement les rapports Surefire et Failsafe : **code de sortie nul seul insuffisant si un IT est ignoré par manque de Docker**. Exécuter ensuite les gates de gouvernance/doc du repository selon `AGENTS.md` et `TESTS_AND_GATES.md`.

## Preuves à renseigner après exécution locale

| Preuve | Valeur / lien local |
|---|---|
| Révision réellement testée (`git rev-parse HEAD`) | À renseigner |
| Date, JDK et version Maven | À renseigner |
| Docker / Testcontainers disponibles | À renseigner |
| Commande ciblée, exit code, nombre de tests | À renseigner |
| Tests IT PostgreSQL réellement exécutés, nombre | À renseigner |
| Commande globale, exit code, nombre de tests | À renseigner |
| État Payment et audit après COMPLETED / REJECTED | À renseigner |
| Vérification outbox et réception callback après résolution | À renseigner |
| Absence de nouveau POST financier en récupération | À renseigner |
| Anomalies, écarts et décisions restantes | À renseigner |

**La fermeture fonctionnelle de Payment T0 et le démarrage d'Accounting T1 ne sont pas autorisés par ce rapport de préparation.**
'''


def git_blob_sha(data: bytes) -> str:
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()


def unix(data: bytes) -> bytes:
    return data.replace(b'\r\n', b'\n')


def newline_style(data: bytes) -> bytes:
    return b'\r\n' if b'\r\n' in data else b'\n'


def strip_insertions(data: bytes) -> bytes:
    first = data.find(START.encode())
    last = data.find(END.encode())
    if first < 0 and last < 0:
        return data
    if first < 0 or last < 0 or data.find(START.encode(), first + 1) >= 0 or data.find(END.encode(), last + 1) >= 0:
        raise ValueError('R6 markers incomplete or duplicated')
    last += len(END)
    return data[:first] + data[last:]


def update_test(data: bytes, expected_blob: str, content: str, name: str) -> tuple[bytes, str]:
    normalized = unix(data)
    original = strip_insertions(normalized)
    if git_blob_sha(original) != expected_blob:
        raise ValueError(f'{name}: file differs from target GitHub SHA, or contains unexpected edits; refusing overwrite')
    already = normalized != original
    if already:
        expected = inject(original, content)
        if normalized != expected:
            raise ValueError(f'{name}: existing R6 block differs; refusing overwrite')
        return data, 'UNCHANGED (already applied)'
    patched = inject(original, content)
    patched = patched.replace(b'\n', newline_style(data))
    return patched, 'MODIFY'


def inject(data: bytes, content: str) -> bytes:
    # Only before the final class brace, preserving other method and import structure.
    end = b'\n}\n' if data.endswith(b'\n}\n') else b'\n}'
    if not data.endswith(end):
        raise ValueError('Unexpected Java class ending')
    block = (START + content + END).encode()
    return data[:-len(end)] + block + end


def prepare(root: Path) -> tuple[dict[Path, bytes], list[str]]:
    if not (root / 'AGENTS.md').is_file() or not (root / 'backend/payment/pom.xml').is_file():
        raise ValueError('Wrong --project-root: AGENTS.md and backend/payment/pom.xml required')
    changes: dict[Path, bytes] = {}
    messages: list[str] = []
    for rel, sha in ROOT_FILES.items():
        file = root / rel
        if not file.is_file():
            raise ValueError(f'Missing required file: {rel}')
        before = file.read_bytes()
        after, status = update_test(before, sha, INSERTIONS[rel], rel)
        messages.append(f'{status}: {rel}')
        if after != before:
            changes[file] = after
    report = root / REPORT_PATH
    if report.exists():
        if report.read_bytes() != REPORT.encode('utf-8'):
            raise ValueError(f'{REPORT_PATH} exists and is not the expected R6 report; refusing overwrite')
        messages.append(f'UNCHANGED (already applied): {REPORT_PATH}')
    else:
        if not report.parent.is_dir():
            raise ValueError(f'Missing documentation folder: {report.parent}')
        changes[report] = REPORT.encode('utf-8')
        messages.append(f'CREATE: {REPORT_PATH}')
    return changes, messages


def apply(changes: dict[Path, bytes], root: Path) -> None:
    if not changes:
        print('No changes to apply.')
        return
    backup_dir = Path(tempfile.mkdtemp(prefix='sixpay-r6-backup-'))
    print('Backups:', backup_dir)
    old: dict[Path, bytes | None] = {}
    for path in changes:
        if path.exists():
            old[path] = path.read_bytes()
            shutil.copy2(path, backup_dir / (path.name + '-' + hashlib.sha256(str(path).encode()).hexdigest()[:12]))
        else:
            old[path] = None
    written: list[Path] = []
    try:
        for path, content in changes.items():
            handle, tempname = tempfile.mkstemp(prefix='.sixpay-r6-', dir=path.parent)
            try:
                with os.fdopen(handle, 'wb') as stream:
                    stream.write(content)
                    stream.flush()
                    os.fsync(stream.fileno())
                os.replace(tempname, path)
                written.append(path)
            finally:
                if os.path.exists(tempname):
                    os.unlink(tempname)
        print('Applied', len(written), 'file(s).')
    except Exception as exc:
        failures = []
        for path in reversed(written):
            try:
                previous = old[path]
                if previous is None:
                    path.unlink()
                else:
                    handle, tempname = tempfile.mkstemp(prefix='.sixpay-r6-restore-', dir=path.parent)
                    try:
                        with os.fdopen(handle, 'wb') as stream:
                            stream.write(previous)
                        os.replace(tempname, path)
                    finally:
                        if os.path.exists(tempname):
                            os.unlink(tempname)
            except Exception as restore_error:
                failures.append(f'{path}: {restore_error}')
        raise RuntimeError(f'Apply failed: {exc}. Rollback issues: {failures or "none"}. Backups: {backup_dir}') from exc


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project-root', required=True, type=Path)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--dry-run', action='store_true')
    mode.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    root = args.project_root.expanduser().resolve()
    try:
        changes, messages = prepare(root)
        for message in messages:
            print(message)
        if args.dry_run:
            print(f'DRY RUN: {len(changes)} change(s) pending, no files written.')
        else:
            apply(changes, root)
        return 0
    except (ValueError, OSError, RuntimeError) as exc:
        print(f'ERROR: {exc}', file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
