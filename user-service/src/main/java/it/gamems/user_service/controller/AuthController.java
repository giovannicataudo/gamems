package it.gamems.user_service.controller;

import it.gamems.user_service.dto.AuthResponseDto;
import it.gamems.user_service.dto.LoginRequestDto;
import it.gamems.user_service.dto.RegisterRequestDto;
import it.gamems.user_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ========================================================
 * CONTROLLER: AuthController
 * ========================================================
 * Espone gli endpoint pubblici per la registrazione e il login.
 * Non richiede autenticazione (configurato in SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * ENDPOINT: Registrazione Nuovo Utente
     * @param request Payload JSON con email e password (validate in ingresso).
     * @return 201 CREATED con il Token JWT.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ENDPOINT: Login Utente Esistente
     * @param request Payload JSON con email e password (validate in ingresso).
     * @return 200 OK con il Token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}