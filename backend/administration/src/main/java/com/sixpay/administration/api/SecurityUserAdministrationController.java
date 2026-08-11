package com.sixpay.administration.api;

import com.sixpay.administration.api.dto.*;
import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.model.SecurityUserSummary;
import com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase;
import com.sixpay.security.authentication.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/administration/users")
@PreAuthorize("hasRole('ADMIN')")
public class SecurityUserAdministrationController {

    private final SecurityUserAdministrationUseCase useCase;
    private final CurrentUserProvider currentUserProvider;

    public SecurityUserAdministrationController(
            SecurityUserAdministrationUseCase useCase,
            CurrentUserProvider currentUserProvider
    ) {
        this.useCase = Objects.requireNonNull(useCase);
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider);
    }

    @GetMapping
    public List<SecurityUserSummary> listUsers() {
        return useCase.listUsers();
    }

    @GetMapping("/{userId}")
    public SecurityUserDetail getUser(@PathVariable UUID userId) {
        return useCase.getUser(userId);
    }

    @PutMapping("/{userId}/authentication-methods/local")
    public SecurityUserDetail setLocalAuthentication(
            @PathVariable UUID userId,
            @Valid @RequestBody SetAuthenticationMethodRequest request
    ) {
        return useCase.setLocalAuthenticationEnabled(
                userId,
                request.enabled(),
                actorSubject()
        );
    }

    @PostMapping("/{userId}/local-password-reset")
    public SecurityUserDetail resetPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ResetLocalPasswordRequest request
    ) {
        return useCase.resetLocalPassword(
                userId,
                request.newPassword(),
                actorSubject()
        );
    }

    @PostMapping("/{userId}/identities/oidc")
    public SecurityUserDetail linkOidc(
            @PathVariable UUID userId,
            @Valid @RequestBody LinkOidcIdentityRequest request
    ) {
        return useCase.linkOidcIdentity(
                userId,
                request.provider(),
                request.providerSubject(),
                actorSubject()
        );
    }

    @DeleteMapping("/{userId}/identities/{identityId}")
    public SecurityUserDetail unlinkOidc(
            @PathVariable UUID userId,
            @PathVariable UUID identityId
    ) {
        return useCase.unlinkOidcIdentity(
                userId,
                identityId,
                actorSubject()
        );
    }

    @PostMapping("/{userId}/disable")
    public SecurityUserDetail disableUser(@PathVariable UUID userId) {
        return useCase.disableUser(userId, actorSubject());
    }

    private String actorSubject() {
        return currentUserProvider.requireCurrentUser().subject();
    }
}
