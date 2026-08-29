package com.sixpay.payment.application.port.output.idempotency;

import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.view.InitiateDebitResult;

import java.util.function.Function;

/**
 * Executes debit initiation exactly once for one idempotency key and request
 * fingerprint, or replays the previously completed result.
 */
public interface PaymentInitiationIdempotencyPort {

    /**
     * Runs {@code newRequest} only when no completed result exists for the
     * command's idempotency key.
     *
     * @param command request whose canonical content is fingerprinted
     * @param newRequest action for a genuinely new request; receives the
     *                   computed lowercase SHA-256 request fingerprint
     * @return a newly produced or durably replayed initiation result
     */
    InitiateDebitResult execute(
            InitiateDebitCommand command,
            Function<String, InitiateDebitResult> newRequest
    );
}
