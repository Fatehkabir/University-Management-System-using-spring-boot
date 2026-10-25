package edu.kuet.ums.controller;

import edu.kuet.ums.dto.Requests.ChangePasswordRequest;
import edu.kuet.ums.dto.Requests.LoginRequest;
import edu.kuet.ums.dto.Requests.RegisterRequest;
import edu.kuet.ums.dto.Responses.AuthResponse;
import edu.kuet.ums.dto.Responses.RegisterResponse;
import edu.kuet.ums.dto.Responses.UserSummary;
import edu.kuet.ums.service.AuthService;
import edu.kuet.ums.service.SignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SignupService signupService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return signupService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserSummary me(Authentication auth) {
        return authService.me(auth.getName());
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(auth.getName(), request);
        return ResponseEntity.noContent().build();
    }
}

