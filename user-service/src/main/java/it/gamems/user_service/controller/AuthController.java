package it.gamems.user_service.controller;

import it.gamems.user_service.dto.*;
import it.gamems.user_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<MfaSetupResponseDto> verifyEmail(@RequestParam String token) {
        MfaSetupResponseDto response = authService.verifyEmailAndSetupMfa(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request) {
        Object response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/mfa")
    public ResponseEntity<AuthResponseDto> verifyMfa(@Valid @RequestBody MfaLoginRequestDto request) {
        AuthResponseDto response = authService.verifyMfaLogin(request);
        return ResponseEntity.ok(response);
    }
}