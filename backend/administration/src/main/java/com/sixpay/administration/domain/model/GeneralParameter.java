package com.sixpay.administration.domain.model;

public record GeneralParameter(
        String typeCode,
        String valeurCode,
        String description
) {
    public GeneralParameter {
        if (typeCode == null || typeCode.isBlank()) {
            throw new IllegalArgumentException("typeCode is required");
        }
        if (valeurCode == null || valeurCode.isBlank()) {
            throw new IllegalArgumentException("valeurCode is required");
        }
        typeCode = typeCode.strip();
        valeurCode = valeurCode.strip();
        description = description == null ? null : description.strip();
    }
}
