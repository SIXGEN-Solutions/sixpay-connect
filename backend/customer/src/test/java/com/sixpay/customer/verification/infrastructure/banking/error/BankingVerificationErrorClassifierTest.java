package com.sixpay.customer.verification.infrastructure.banking.error;

import com.sixpay.customer.verification.application.exception.BankingVerificationAuthenticationException;
import com.sixpay.customer.verification.application.exception.BankingVerificationInvalidResponseException;
import com.sixpay.customer.verification.application.exception.BankingVerificationProtocolException;
import com.sixpay.customer.verification.application.exception.BankingVerificationUnavailableException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingVerificationErrorClassifierTest {

    private final BankingVerificationErrorClassifier classifier =
            new BankingVerificationErrorClassifier();

    @Test
    void classifies502503And504AsRetryableUnavailable() {
        for (int status : new int[]{502, 503, 504}) {
            var result = classifier.classify(
                    httpFailure(status)
            );

            assertInstanceOf(
                    BankingVerificationUnavailableException.class,
                    result
            );
            assertTrue(result.retryable());
        }
    }

    @Test
    void classifies401And403AsNonRetryableAuthentication() {
        for (int status : new int[]{401, 403}) {
            var result = classifier.classify(
                    httpFailure(status)
            );

            assertInstanceOf(
                    BankingVerificationAuthenticationException.class,
                    result
            );
            assertFalse(result.retryable());
        }
    }

    @Test
    void classifies400404And422AsNonRetryableProtocol() {
        for (int status : new int[]{400, 404, 422}) {
            var result = classifier.classify(
                    httpFailure(status)
            );

            assertInstanceOf(
                    BankingVerificationProtocolException.class,
                    result
            );
            assertFalse(result.retryable());
        }
    }

    @Test
    void classifiesInvalidMappingAsNonRetryableInvalidResponse() {
        var result = classifier.classify(
                new IllegalArgumentException("invalid payload")
        );

        assertInstanceOf(
                BankingVerificationInvalidResponseException.class,
                result
        );
        assertFalse(result.retryable());
    }

    private static AmplitudeClientException httpFailure(int status) {
        return new AmplitudeClientException(
                status,
                AmplitudeErrorResponse.unknown(
                        status,
                        "corr-test"
                ),
                null
        );
    }
}
