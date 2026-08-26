package com.sixpay.integration.exception;

/**
 * Standard categories of errors produced during an external
 * system integration.
 */
public enum IntegrationErrorType {

    TIMEOUT,
    CONNECTION_FAILURE,
    AUTHENTICATION_FAILURE,
    AUTHORIZATION_FAILURE,
    INVALID_REQUEST,
    INVALID_RESPONSE,
    REMOTE_REJECTION,
    RATE_LIMIT_EXCEEDED,
    UNAVAILABLE,
    UNEXPECTED_ERROR
}