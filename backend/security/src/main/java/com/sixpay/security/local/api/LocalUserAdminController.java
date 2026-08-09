package com.sixpay.security.local.api;

import com.sixpay.security.local.LocalUserAdministrationService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/users")
@ConditionalOnProperty(
        prefix = "sixpay.security",
        name = "authentication-mode",
        havingValue = "LOCAL"
)
public class LocalUserAdminController {

    private final LocalUserAdministrationService service;

    public LocalUserAdminController(
            LocalUserAdministrationService service
    ) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public LocalAuthResponses.CurrentUserResponse create(
            @Valid @RequestBody
            LocalUserAdminRequests.CreateLocalUserRequest request
    ) {
        var user = service.create(
                request.username(),
                request.password(),
                request.subject(),
                request.roles()
        );

        return new LocalAuthResponses.CurrentUserResponse(
                user.getSubject(),
                user.getUsername(),
                user.getRoles().stream()
                        .map(Enum::name)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
    }
}
