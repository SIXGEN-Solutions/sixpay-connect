package com.sixpay.administration.api.dto;

public record DirectoryUserResponse(
        String username,
        String displayName,
        String email,
        String accountStatus,
        String stableSubject
) {
}
