package de.capswap.controller;

import de.capswap.dto.AuthDtos;
import de.capswap.entity.Company;
import de.capswap.entity.PasswordResetToken;
import de.capswap.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<de.capswap.dto.CompanyResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        Company registered = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(de.capswap.dto.CompanyResponse.from(registered));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.authenticate(request.getEmail(), request.getPassword())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "timestamp", Instant.now().toString(),
                                "message", "E-Mail-Adresse oder Passwort ist falsch.")));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<PasswordResetToken> requestPasswordReset(@Valid @RequestBody AuthDtos.PasswordResetRequest request) {
        PasswordResetToken token = authService.createPasswordResetToken(request.getEmail());
        return ResponseEntity.ok(token);
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody AuthDtos.PasswordResetConfirm request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
