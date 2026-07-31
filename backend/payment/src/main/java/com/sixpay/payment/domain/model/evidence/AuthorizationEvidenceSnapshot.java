package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class AuthorizationEvidenceSnapshot implements ValueObject {

    private static final Pattern ALGORITHM_FORMAT =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{1,31}$");
    private static final Pattern SCOPE_FORMAT =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9:._/-]{0,127}$");

    private final AuthorizationEvidenceReference authorizationEvidenceReference;
    private final AuthorizationDecisionOutcome outcome;
    private final EvidenceFingerprint tokenFingerprint;
    private final String issuer;
    private final String keyId;
    private final String signatureAlgorithm;
    private final String scope;
    private final List<AuthorizationBindingEvidence> bindingResults;
    private final Instant issuedAt;
    private final Instant validFrom;
    private final Instant expiresAt;
    private final FailureCode rejectionCode;
    private final EvidenceMetadata metadata;

    public AuthorizationEvidenceSnapshot(
            AuthorizationEvidenceReference authorizationEvidenceReference,
            AuthorizationDecisionOutcome outcome,
            EvidenceFingerprint tokenFingerprint,
            String issuer,
            String keyId,
            String signatureAlgorithm,
            String scope,
            List<AuthorizationBindingEvidence> bindingResults,
            Instant issuedAt,
            Instant validFrom,
            Instant expiresAt,
            FailureCode rejectionCode,
            EvidenceMetadata metadata
    ) {
        this.authorizationEvidenceReference =
                EvidenceValueObjectRules.requireNonNull(
                        authorizationEvidenceReference,
                        "Authorization evidence reference"
                );
        this.outcome = EvidenceValueObjectRules.requireNonNull(
                outcome,
                "Authorization outcome"
        );
        this.tokenFingerprint = EvidenceValueObjectRules.requireNonNull(
                tokenFingerprint,
                "Token fingerprint"
        );
        this.issuer = EvidenceValueObjectRules.requireOpaque(
                issuer,
                1,
                256,
                "Authorization issuer"
        );
        this.keyId = EvidenceValueObjectRules.requirePrintableAsciiNoWhitespace(
                keyId,
                1,
                128,
                "Authorization key ID"
        );
        this.signatureAlgorithm = EvidenceValueObjectRules.requirePattern(
                signatureAlgorithm,
                ALGORITHM_FORMAT,
                2,
                32,
                "Signature algorithm"
        );
        this.scope = EvidenceValueObjectRules.requirePattern(
                scope,
                SCOPE_FORMAT,
                1,
                128,
                "Authorization scope"
        );
        this.bindingResults = canonicalBindings(bindingResults);
        this.issuedAt = EvidenceValueObjectRules.requireNonNull(
                issuedAt,
                "Authorization issued instant"
        );
        this.validFrom = EvidenceValueObjectRules.requireNonNull(
                validFrom,
                "Authorization valid-from instant"
        );
        this.expiresAt = EvidenceValueObjectRules.requireNonNull(
                expiresAt,
                "Authorization expiry instant"
        );
        this.rejectionCode = rejectionCode;
        this.metadata = EvidenceValueObjectRules.requireNonNull(
                metadata,
                "Authorization evidence metadata"
        );

        if (metadata.sourceSystem() != ExternalSystem.TRESOR_PAY) {
            throw new IllegalArgumentException(
                    "Authorization evidence source must be TRESOR_PAY"
            );
        }

        EvidenceValueObjectRules.requireNotBefore(
                validFrom,
                issuedAt,
                "Authorization valid-from instant must not precede issue"
        );
        EvidenceValueObjectRules.requireNotBefore(
                expiresAt,
                validFrom,
                "Authorization expiry must not precede validity"
        );

        if (outcome == AuthorizationDecisionOutcome.APPROVED) {
            if (rejectionCode != null) {
                throw new IllegalArgumentException(
                        "Approved authorization must not have a rejection code"
                );
            }
            if (bindingResults.stream().anyMatch(
                    binding -> binding.result()
                            == AuthorizationBindingResult.MISMATCH
            )) {
                throw new IllegalArgumentException(
                        "Approved authorization cannot contain a mismatch"
                );
            }
        } else if (rejectionCode == null) {
            throw new IllegalArgumentException(
                    "Rejected authorization requires a rejection code"
            );
        }
    }

    private static List<AuthorizationBindingEvidence> canonicalBindings(
            List<AuthorizationBindingEvidence> bindings
    ) {
        EvidenceValueObjectRules.requireNonNull(
                bindings,
                "Authorization bindings"
        );
        if (bindings.isEmpty()
                || bindings.size() > AuthorizationBindingType.values().length) {
            throw new IllegalArgumentException(
                    "Authorization bindings must contain 1 to "
                            + AuthorizationBindingType.values().length
                            + " entries"
            );
        }

        Set<AuthorizationBindingType> seen =
                EnumSet.noneOf(AuthorizationBindingType.class);
        List<AuthorizationBindingEvidence> canonical =
                new ArrayList<>(bindings.size());

        for (AuthorizationBindingEvidence binding : bindings) {
            AuthorizationBindingEvidence validated =
                    EvidenceValueObjectRules.requireNonNull(
                            binding,
                            "Authorization binding"
                    );
            if (!seen.add(validated.type())) {
                throw new IllegalArgumentException(
                        "Authorization binding types must be unique"
                );
            }
            canonical.add(validated);
        }

        canonical.sort(
                (left, right) ->
                        Integer.compare(
                                left.type().ordinal(),
                                right.type().ordinal()
                        )
        );
        return List.copyOf(canonical);
    }

    public AuthorizationEvidenceReference authorizationEvidenceReference() {
        return authorizationEvidenceReference;
    }

    public AuthorizationDecisionOutcome outcome() {
        return outcome;
    }

    public EvidenceFingerprint tokenFingerprint() {
        return tokenFingerprint;
    }

    public String issuer() {
        return issuer;
    }

    public String keyId() {
        return keyId;
    }

    public String signatureAlgorithm() {
        return signatureAlgorithm;
    }

    public String scope() {
        return scope;
    }

    public List<AuthorizationBindingEvidence> bindingResults() {
        return bindingResults;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant validFrom() {
        return validFrom;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Optional<FailureCode> rejectionCode() {
        return Optional.ofNullable(rejectionCode);
    }

    public EvidenceMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuthorizationEvidenceSnapshot that)) {
            return false;
        }
        return authorizationEvidenceReference.equals(
                that.authorizationEvidenceReference
        ) && outcome == that.outcome
                && tokenFingerprint.equals(that.tokenFingerprint)
                && issuer.equals(that.issuer)
                && keyId.equals(that.keyId)
                && signatureAlgorithm.equals(that.signatureAlgorithm)
                && scope.equals(that.scope)
                && bindingResults.equals(that.bindingResults)
                && issuedAt.equals(that.issuedAt)
                && validFrom.equals(that.validFrom)
                && expiresAt.equals(that.expiresAt)
                && Objects.equals(rejectionCode, that.rejectionCode)
                && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                authorizationEvidenceReference,
                outcome,
                tokenFingerprint,
                issuer,
                keyId,
                signatureAlgorithm,
                scope,
                bindingResults,
                issuedAt,
                validFrom,
                expiresAt,
                rejectionCode,
                metadata
        );
    }

    @Override
    public String toString() {
        return "AuthorizationEvidenceSnapshot[reference="
                + authorizationEvidenceReference
                + ", outcome=" + outcome
                + ", bindingCount=" + bindingResults.size()
                + ", metadata=" + metadata + "]";
    }
}
