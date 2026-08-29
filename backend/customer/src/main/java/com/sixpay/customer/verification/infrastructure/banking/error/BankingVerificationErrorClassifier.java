package com.sixpay.customer.verification.infrastructure.banking.error;

import com.sixpay.customer.verification.application.exception.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;

public final class BankingVerificationErrorClassifier {

    public BankingVerificationException classify(
            RuntimeException failure
    ) {
        if (failure instanceof BankingVerificationException classified) {
            return classified;
        }

        if (failure instanceof AmplitudeInvalidResponseException) {
            return new BankingVerificationInvalidResponseException(
                    "Core Banking response is invalid",
                    failure
            );
        }

        if (failure instanceof AmplitudeRateLimitException) {
            return new BankingVerificationUnavailableException(
                    "Core Banking rate limit exceeded",
                    failure
            );
        }

        if (containsTimeout(failure)) {
            return new BankingVerificationTimeoutException(
                    "Core Banking request timed output",
                    failure
            );
        }

        if (containsConnectionFailure(failure)) {
            return new BankingVerificationUnavailableException(
                    "Core Banking connection failed",
                    failure
            );
        }

        if (failure instanceof AmplitudeClientException httpFailure) {
            return classifyHttp(httpFailure);
        }

        if (failure instanceof ResourceAccessException
                || failure instanceof RestClientException) {
            return new BankingVerificationUnavailableException(
                    "Core Banking could not be reached",
                    failure
            );
        }

        if (failure instanceof IllegalArgumentException
                || failure instanceof IllegalStateException) {
            return new BankingVerificationInvalidResponseException(
                    "Core Banking response or configuration is invalid",
                    failure
            );
        }

        return new BankingVerificationProtocolException(
                "Unexpected Core Banking protocol failure",
                failure
        );
    }

    private BankingVerificationException classifyHttp(
            AmplitudeClientException failure
    ) {
        return switch (failure.httpStatus()) {
            case 401, 403 ->
                    new BankingVerificationAuthenticationException(
                            "Core Banking authentication or authorization failed",
                            failure
                    );
            case 429, 502, 503, 504 ->
                    new BankingVerificationUnavailableException(
                            "Core Banking is temporarily unavailable",
                            failure
                    );
            case 400, 404, 409, 422 ->
                    new BankingVerificationProtocolException(
                            "Core Banking rejected the verification request",
                            failure
                    );
            default -> failure.httpStatus() >= 500
                    ? new BankingVerificationUnavailableException(
                            "Core Banking returned a server error",
                            failure
                    )
                    : new BankingVerificationProtocolException(
                            "Core Banking returned a non-retryable HTTP error",
                            failure
                    );
        };
    }

    private static boolean containsTimeout(Throwable failure) {
        return causeChainContains(
                failure,
                SocketTimeoutException.class,
                HttpTimeoutException.class,
                HttpConnectTimeoutException.class
        );
    }

    private static boolean containsConnectionFailure(Throwable failure) {
        return causeChainContains(failure, ConnectException.class);
    }

    @SafeVarargs
    private static boolean causeChainContains(
            Throwable failure,
            Class<? extends Throwable>... expectedTypes
    ) {
        Throwable current = failure;
        while (current != null) {
            for (Class<? extends Throwable> expectedType : expectedTypes) {
                if (expectedType.isInstance(current)) return true;
            }
            Throwable next = current.getCause();
            if (next == current) break;
            current = next;
        }
        return false;
    }
}
