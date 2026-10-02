package com.sixpay.administration.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Explicit administrative LDAP identity link.
 *
 * trustDomain identifies the configured LDAP trust boundary.
 * stableSubject is the immutable external directory subject.
 * DN, username, email and LDAP groups are deliberately not accepted as durable link keys.
 */
public record LinkLdapIdentityRequest(
        @NotBlank @Size(max = 500) String trustDomain,
        @NotBlank @Size(max = 255) String stableSubject
) {
}