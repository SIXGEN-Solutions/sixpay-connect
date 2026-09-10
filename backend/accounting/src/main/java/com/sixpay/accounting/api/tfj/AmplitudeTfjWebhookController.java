package com.sixpay.accounting.api.tfj;

import com.sixpay.accounting.application.service.TfjIngestionResult;
import com.sixpay.accounting.application.service.TfjIngestionService;
import com.sixpay.accounting.domain.model.TfjObservationChannel;
import com.sixpay.accounting.infrastructure.tfj.dto.EndOfDayConfirmationDto;
import com.sixpay.accounting.infrastructure.tfj.mapper.AmplitudeTfjMapper;
import com.sixpay.accounting.infrastructure.tfj.security.AmplitudeTfjSignatureVerifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@RestController
public class AmplitudeTfjWebhookController {

    private final AmplitudeTfjSignatureVerifier signatureVerifier;
    private final AmplitudeTfjMapper mapper;
    private final TfjIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    public AmplitudeTfjWebhookController(
            AmplitudeTfjSignatureVerifier signatureVerifier,
            AmplitudeTfjMapper mapper,
            TfjIngestionService ingestionService,
            ObjectMapper objectMapper
    ) {
        this.signatureVerifier = signatureVerifier;
        this.mapper = mapper;
        this.ingestionService = ingestionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(
            path = "/webhooks/v1/amplitude/end-of-day-confirmations",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ConfirmationAcknowledgement> receive(
            @RequestHeader("X-Correlation-ID")
            UUID correlationId,
            @RequestHeader("Idempotency-Key")
            String idempotencyKey,
            @RequestHeader("X-Amplitude-Signature")
            String signature,
            @RequestHeader("X-Amplitude-Signature-Key-ID")
            String signatureKeyId,
            @RequestHeader("X-Amplitude-Signature-Timestamp")
            String signatureTimestamp,
            @RequestBody byte[] rawBody
    ) {
        signatureVerifier.verify(
                rawBody,
                signature,
                signatureKeyId,
                signatureTimestamp
        );

        EndOfDayConfirmationDto dto =
                objectMapper.readValue(
                        rawBody,
                        EndOfDayConfirmationDto.class
                );

        TfjIngestionResult result =
                ingestionService.ingest(
                        mapper.toDomain(
                                dto,
                                idempotencyKey,
                                correlationId.toString(),
                                TfjObservationChannel.ASYNC_CALLBACK
                        )
                );

        return ResponseEntity.accepted().body(
                new ConfirmationAcknowledgement(
                        result.confirmationId(),
                        correlationId,
                        result.receiptStatus().name(),
                        result.receivedAt()
                )
        );
    }
}
