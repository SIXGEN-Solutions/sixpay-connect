package com.sixpay.customer.verification.infrastructure.banking.error;

import com.sixpay.customer.verification.application.exception.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class BankingVerificationErrorClassifierTest {

    private final BankingVerificationErrorClassifier classifier =
            new BankingVerificationErrorClassifier();

    @Test
    void classifiesAuthenticationStatusesAsNonRetryable() {
        for (int status : new int[]{401, 403}) {
            var classified = classifier.classify(
                    new AmplitudeClientException(
                            status,
                            AmplitudeErrorResponse.unknown(
                                    status,
                                    "corr"
                            ),
                            null
                    )
            );

            assertThat(classified)
                    .isInstanceOf(
                            BankingVerificationAuthenticationException.class
                    );
            assertThat(classified.retryable()).isFalse();
        }
    }

    @Test
    void classifiesRateLimitAsRetryableUnavailable() {
        var classified = classifier.classify(
                new AmplitudeRateLimitException(
                        Duration.ofSeconds(2),
                        null
                )
        );

        assertThat(classified)
                .isInstanceOf(
                        BankingVerificationUnavailableException.class
                );
        assertThat(classified.retryable()).isTrue();
    }

    @Test
    void classifiesMalformedResponseAsNonRetryable() {
        var classified = classifier.classify(
                new AmplitudeInvalidResponseException(
                        "malformed"
                )
        );

        assertThat(classified)
                .isInstanceOf(
                        BankingVerificationInvalidResponseException.class
                );
        assertThat(classified.retryable()).isFalse();
    }

    @Test
    void classifies404And409AsProtocolErrors() {
        for (int status : new int[]{404, 409}) {
            var classified = classifier.classify(
                    new AmplitudeClientException(
                            status,
                            AmplitudeErrorResponse.unknown(
                                    status,
                                    "corr"
                            ),
                            null
                    )
            );

            assertThat(classified)
                    .isInstanceOf(
                            BankingVerificationProtocolException.class
                    );
            assertThat(classified.retryable()).isFalse();
        }
    }
}
