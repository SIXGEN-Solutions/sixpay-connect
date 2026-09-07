package com.sixpay.administration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "general_parameter")
public class GeneralParameterJpaEntity {

    @Id
    @Column(name = "type_code", nullable = false, length = 64)
    private String typeCode;

    @Column(name = "valeur_code", nullable = false, length = 256)
    private String valeurCode;

    @Column(name = "description", length = 512)
    private String description;

    protected GeneralParameterJpaEntity() {
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getValeurCode() {
        return valeurCode;
    }

    public String getDescription() {
        return description;
    }
}
