package com.sixpay.security.local.api;

import com.sixpay.security.local.LocalPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.TreeSet;

@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(
        prefix = "sixpay.security",
        name = "authentication-mode",
        havingValue = "LOCAL"
)
public class LocalAuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public LocalAuthenticationController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<
            LocalAuthResponses.CurrentUserResponse
            > login(
            @Valid @RequestBody
            LocalAuthRequests.LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username(),
                        request.password()
                )
        );

        var context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        return ResponseEntity.ok(toCurrentUser(authentication));
    }

    @GetMapping("/me")
    public ResponseEntity<
            LocalAuthResponses.CurrentUserResponse
            > currentUser(
            Authentication authentication
    ) {
        return ResponseEntity.ok(toCurrentUser(authentication));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request
    ) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        securityContextHolderStrategy.clearContext();
        return ResponseEntity.noContent().build();
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> authenticationFailure(
            AuthenticationException exception
    ) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password"
        );
        problem.setTitle("Authentication failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    private LocalAuthResponses.CurrentUserResponse toCurrentUser(
            Authentication authentication
    ) {
        var principal = (LocalPrincipal) authentication.getPrincipal();
        var roles = new TreeSet<String>();
        principal.roles().forEach(role -> roles.add(role.name()));

        return new LocalAuthResponses.CurrentUserResponse(
                principal.subject(),
                principal.username(),
                Set.copyOf(roles)
        );
    }
}
