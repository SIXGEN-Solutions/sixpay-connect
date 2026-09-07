package com.sixpay.payment.infrastructure.banking.amplitude.posting.client;

import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.CoreBankingCodeResponse;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.CoreBankingStringDataResponse;

public interface AmplitudePaymentEventContextClient {

    CoreBankingStringDataResponse allocateNextEventNumber(
            String operationCode,
            String correlationId,
            String financialInstitutionCode
    );

    CoreBankingStringDataResponse getAccountingDate(
            String correlationId,
            String financialInstitutionCode
    );

    CoreBankingCodeResponse getNightMode(
            String correlationId,
            String financialInstitutionCode
    );
}
