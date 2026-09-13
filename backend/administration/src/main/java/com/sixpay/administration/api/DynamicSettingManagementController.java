package com.sixpay.administration.api;

import com.sixpay.administration.api.dto.*;
import com.sixpay.administration.application.port.input.DynamicSettingManagementUseCase;
import com.sixpay.security.authentication.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/internal/api/v1/administration/dynamic-settings")
@PreAuthorize("hasRole('ADMIN')")
public class DynamicSettingManagementController {

    private final DynamicSettingManagementUseCase useCase;
    private final CurrentUserProvider currentUserProvider;

    public DynamicSettingManagementController(
            DynamicSettingManagementUseCase useCase,
            CurrentUserProvider currentUserProvider
    ) {
        this.useCase = Objects.requireNonNull(useCase);
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider);
    }

    @GetMapping
    public List<DynamicSettingDefinitionResponse> definitions() {
        return useCase.definitions().stream().map(DynamicSettingDefinitionResponse::from).toList();
    }

    @GetMapping("/{key:.+}")
    public DynamicSettingValueResponse get(@PathVariable String key) {
        return DynamicSettingValueResponse.from(useCase.get(key));
    }

    @PutMapping("/{key:.+}")
    public DynamicSettingValueResponse update(
            @PathVariable String key,
            @Valid @RequestBody DynamicSettingUpdateRequest request
    ) {
        return DynamicSettingValueResponse.from(
                useCase.update(key, request.value(), request.reason(), actorSubject())
        );
    }

    @GetMapping("/{key:.+}/history")
    public List<DynamicSettingHistoryResponse> history(@PathVariable String key) {
        return useCase.history(key).stream().map(DynamicSettingHistoryResponse::from).toList();
    }

    @PostMapping("/{key:.+}/rollback")
    public DynamicSettingValueResponse rollback(
            @PathVariable String key,
            @Valid @RequestBody DynamicSettingRollbackRequest request
    ) {
        return DynamicSettingValueResponse.from(
                useCase.rollback(key, request.targetVersion(), request.reason(), actorSubject())
        );
    }

    private String actorSubject() {
        return currentUserProvider.requireCurrentUser().subject();
    }
}
