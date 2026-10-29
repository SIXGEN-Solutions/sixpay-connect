package com.sixpay.administration.api;

import com.sixpay.administration.api.dto.DirectoryUserResponse;
import com.sixpay.administration.api.dto.ProvisionDirectoryUserRequest;
import com.sixpay.security.application.model.DirectoryUserProfile;
import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.port.input.DirectoryUserLookupQuery;
import com.sixpay.security.application.port.input.DirectoryUserLookupUseCase;
import com.sixpay.security.application.port.input.ProvisionLdapSecurityUserCommand;
import com.sixpay.security.application.port.input.ProvisionLdapSecurityUserUseCase;
import com.sixpay.security.authentication.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Objects;

@RestController
@RequestMapping("/internal/api/v1/administration/directory-users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class DirectoryUserAdministrationController {

    private final DirectoryUserLookupUseCase lookupUseCase;
    private final ProvisionLdapSecurityUserUseCase provisioningUseCase;
    private final CurrentUserProvider currentUserProvider;

    public DirectoryUserAdministrationController(
            DirectoryUserLookupUseCase lookupUseCase,
            ProvisionLdapSecurityUserUseCase provisioningUseCase,
            CurrentUserProvider currentUserProvider
    ) {
        this.lookupUseCase = Objects.requireNonNull(lookupUseCase);
        this.provisioningUseCase = Objects.requireNonNull(provisioningUseCase);
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider);
    }

    @GetMapping("/{username}")
    public DirectoryUserResponse getDirectoryUser(
            @PathVariable @NotBlank @Size(max = 150) String username
    ) {
        return toResponse(
                lookupUseCase.lookup(
                        new DirectoryUserLookupQuery(username)
                )
        );
    }

    @PostMapping("/{username}/provisioning")
    public ResponseEntity<SecurityUserDetail> provisionDirectoryUser(
            @PathVariable @NotBlank @Size(max = 150) String username,
            @Valid @RequestBody ProvisionDirectoryUserRequest request
    ) {
        SecurityUserDetail created =
                provisioningUseCase.provision(
                        new ProvisionLdapSecurityUserCommand(
                                username,
                                request.roles(),
                                request.permissions(),
                                actorSubject()
                        )
                );

        var location =
                ServletUriComponentsBuilder
                        .fromCurrentContextPath()
                        .path("/internal/api/v1/administration/users/{userId}")
                        .buildAndExpand(created.id())
                        .toUri();

        return ResponseEntity.created(location).body(created);
    }

    private String actorSubject() {
        return currentUserProvider.requireCurrentUser().subject();
    }

    private static DirectoryUserResponse toResponse(
            DirectoryUserProfile profile
    ) {
        return new DirectoryUserResponse(
                profile.username(),
                profile.displayName(),
                profile.email(),
                profile.accountStatus().name(),
                profile.stableSubject()
        );
    }
}
