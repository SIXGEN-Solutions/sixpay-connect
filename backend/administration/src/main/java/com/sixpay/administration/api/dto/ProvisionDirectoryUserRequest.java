package com.sixpay.administration.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = false)
public record ProvisionDirectoryUserRequest(
        @NotNull Set<@NotBlank @Size(max = 100) String> roles,
        @NotNull Set<@NotBlank @Size(max = 150) String> permissions
) {
}
