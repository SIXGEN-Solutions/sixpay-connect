package com.sixpay.payment.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtectedAccountValueObjectsTest {

    private static final FinancialInstitutionCode BANK =
            FinancialInstitutionCode.of("BANK_CM");

    @Test
    void debtorReferenceNeverPrintsTheProtectedToken() {
        DebtorAccountReference reference =
                debtorReference(
                        "vault:debtor:0001",
                        "****************1234"
                );

        assertEquals(
                "****************1234",
                reference.toString()
        );
        assertFalse(
                reference.toString().contains(
                        reference.integrationAccountToken()
                )
        );
        assertEquals(
                BANK,
                reference.financialInstitutionCode()
        );
    }

    @Test
    void debtorIdentityExcludesDisplayButIncludesTokenAndFingerprint() {
        DebtorAccountReference first =
                debtorReference(
                        "vault:debtor:0001",
                        "****************1234"
                );
        DebtorAccountReference sameIdentityDifferentDisplay =
                debtorReference(
                        "vault:debtor:0001",
                        "************1234"
                );
        DebtorAccountReference anotherToken =
                debtorReference(
                        "vault:debtor:0002",
                        "****************1234"
                );

        assertEquals(first, sameIdentityDifferentDisplay);
        assertNotEquals(first, anotherToken);
    }

    @Test
    void debtorReferenceRejectsUnsafeComponents() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DebtorAccountReference(
                        BANK,
                        "vault:debtor:0001",
                        "1234567890123456",
                        "v1:" + "a".repeat(64)
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new DebtorAccountReference(
                        BANK,
                        "vault:debtor:0001",
                        "****************1234",
                        "sha256:" + "a".repeat(64)
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new DebtorAccountReference(
                        BANK,
                        "bad\nvalue",
                        "****************1234",
                        "v1:" + "a".repeat(64)
                )
        );
    }

    @Test
    void treasuryReferenceEqualityUsesConfigurationIdentity() {
        TreasuryAccountReference first =
                new TreasuryAccountReference(
                        BANK,
                        "CUT-CONFIG-01",
                        "vault:cut:0001",
                        "****************9999",
                        "v7"
                );
        TreasuryAccountReference sameConfiguration =
                new TreasuryAccountReference(
                        BANK,
                        "CUT-CONFIG-01",
                        "vault:cut:rotated",
                        "************9999",
                        "v7"
                );
        TreasuryAccountReference anotherVersion =
                new TreasuryAccountReference(
                        BANK,
                        "CUT-CONFIG-01",
                        "vault:cut:0001",
                        "****************9999",
                        "v8"
                );

        assertEquals(first, sameConfiguration);
        assertNotEquals(first, anotherVersion);
        assertEquals(
                "****************9999",
                first.toString()
        );
        assertFalse(first.toString().contains(first.accountToken()));
    }

    @Test
    void bankPostingReferenceRequiresPrincipalAndKeepsOptionalLegs() {
        BankPostingReference principalOnly =
                BankPostingReference.principalOnly(
                        "POSTING-0001"
                );
        BankPostingReference complete =
                new BankPostingReference(
                        "POSTING-0001",
                        "DEBIT-0001",
                        "CUT-0001"
                );

        assertEquals(
                "POSTING-0001",
                principalOnly.principalPostingReference()
        );
        assertTrueEmpty(principalOnly);
        assertEquals(
                "DEBIT-0001",
                complete.debitLegReference().orElseThrow()
        );
        assertEquals(
                "CUT-0001",
                complete.cutCreditLegReference().orElseThrow()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> BankPostingReference.principalOnly(
                        "POSTING 0001"
                )
        );
    }

    private static void assertTrueEmpty(
            BankPostingReference reference
    ) {
        assertEquals(
                true,
                reference.debitLegReference().isEmpty()
        );
        assertEquals(
                true,
                reference.cutCreditLegReference().isEmpty()
        );
    }

    private static DebtorAccountReference debtorReference(
            String token,
            String display
    ) {
        return new DebtorAccountReference(
                BANK,
                token,
                display,
                "v1:" + "a".repeat(64)
        );
    }
}
