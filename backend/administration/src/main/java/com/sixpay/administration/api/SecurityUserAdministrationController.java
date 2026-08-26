package com.sixpay.administration.api;

import com.sixpay.administration.api.dto.*;
import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.model.SecurityUserSummary;
import com.sixpay.security.application.port.in.CreateSecurityUserCommand;
import com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase;
import com.sixpay.security.application.port.in.UpdateSecurityUserCommand;
import com.sixpay.security.authentication.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

    @PostMapping
    public ResponseEntity<SecurityUserDetail> createUser(
            @Valid @RequestBody CreateSecurityUserRequest request
    ) {
        UUID userId = UUID.randomUUID();

        SecurityUserDetail created = useCase.createUser(
                new CreateSecurityUserCommand(
                        userId,
                        request.username(),
                        request.email(),
                        request.roles(),
                        request.permissions(),
                        request.localAuthenticationEnabled(),
                        request.initialPassword(),
                        actorSubject()
                )
        );

        var location =
                ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{userId}")
                        .buildAndExpand(created.id())
                        .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<SecurityUserSummary> listUsers() {
        return useCase.listUsers();
    }

    @GetMapping("/{userId}")
    public SecurityUserDetail getUser(
            @PathVariable UUID userId
    ) {
        return useCase.getUser(userId);
    }

    @PutMapping("/{userId}")
    public SecurityUserDetail updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateSecurityUserRequest request
    ) {
        return useCase.updateUser(
                new UpdateSecurityUserCommand(
                        userId,
                        request.username(),
                        request.email(),
                        request.roles(),
                        request.permissions(),
                        actorSubject()
                )
        );
    }

    @PostMapping("/{userId}/enable")
    public SecurityUserDetail enableUser(
            @PathVariable UUID userId
    ) {
        return useCase.enableUser(
                userId,
                actorSubject()
        );
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
    public SecurityUserDetail disableUser(
            @PathVariable UUID userId
    ) {
        return useCase.disableUser(
                userId,
                actorSubject()
        );
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId
    ) {
        useCase.deleteUser(
                userId,
                actorSubject()
        );
        return ResponseEntity.noContent().build();
    }

    private String actorSubject() {
        return currentUserProvider
                .requireCurrentUser()
                .subject();
    }
}
