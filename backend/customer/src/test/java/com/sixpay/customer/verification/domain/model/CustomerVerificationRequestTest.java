package com.sixpay.customer.verification.domain.model;

import com.sixpay.common.context.CorrelationId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerVerificationRequestTest {

    private static final Instant REQUESTED_AT =
            Instant.parse("2026-08-02T20:00:00Z");

    @Test
    void createsAValidTransportNeutralRequest() {
        CustomerVerificationRequest request = validRequest();

        String rendered = request.toString();

        assertFalse(rendered.contains("M0123456"));
        assertFalse(rendered.contains("Ada Lovelace"));
        assertFalse(rendered.contains("v1:" + "a".repeat(64)));
    }

    @Test
    void rejectsEveryMissingRequiredComponent() {
        CustomerVerificationRequest valid = validRequest();

        assertThrows(
                NullPointerException.class,
                () -> new CustomerVerificationRequest(
                        null,
                        valid.subject(),
                        valid.financialInstitutionCode(),
                        valid.accountBindingFingerprint(),
                        valid.context(),
                        valid.requestedAt()
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new CustomerVerificationRequest(
                        valid.verificationId(),
                        null,
                        valid.financialInstitutionCode(),
                        valid.accountBindingFingerprint(),
                        valid.context(),
                        valid.requestedAt()
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new CustomerVerificationRequest(
                        valid.verificationId(),
                        valid.subject(),
                        null,
                        valid.accountBindingFingerprint(),
                        valid.context(),
                        valid.requestedAt()
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new CustomerVerificationRequest(
                        valid.verificationId(),
                        valid.subject(),
                        valid.financialInstitutionCode(),
                        null,
                        valid.context(),
                        valid.requestedAt()
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new CustomerVerificationRequest(
                        valid.verificationId(),
                        valid.subject(),
                        valid.financialInstitutionCode(),
                        valid.accountBindingFingerprint(),
                        null,
                        valid.requestedAt()
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new CustomerVerificationRequest(
                        valid.verificationId(),
                        valid.subject(),
                        valid.financialInstitutionCode(),
                        valid.accountBindingFingerprint(),
                        valid.context(),
                        null
                )
        );
    }

    private static CustomerVerificationRequest validRequest() {
        return new CustomerVerificationRequest(
                new CustomerVerificationId(
                        UUID.fromString(
                                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                        )
                ),
                CustomerVerificationSubject.of(
                        CustomerIdentity.of(
                                CustomerNiu.of("M0123456"),
                                "Ada Lovelace"
                        )
                ),
                FinancialInstitutionCode.of("AMPLITUDE"),
                AccountBindingFingerprint.of(
                        "v1:" + "a".repeat(64)
                ),
                CustomerVerificationContext.of(
                        CorrelationId.of("corr-123"),
                        UUID.fromString(
                                "c74e165f-df46-463e-a520-188e6df3e5ae"
                        )
                ),
                REQUESTED_AT
        );
    }
}
