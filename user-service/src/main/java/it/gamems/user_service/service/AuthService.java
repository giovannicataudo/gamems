package it.gamems.user_service.service;

import it.gamems.user_service.dto.AuthResponseDto;
import it.gamems.user_service.dto.LoginRequestDto;
import it.gamems.user_service.dto.RegisterRequestDto;
import it.gamems.user_service.entity.User;
import it.gamems.user_service.enums.Role;
import it.gamems.user_service.exception.UserAlreadyExistsException;
import it.gamems.user_service.repository.UserRepository;
import it.gamems.user_service.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ========================================================
 * SERVICE: AuthService
 * ========================================================
 * Orchestratore delle operazioni di identità.
 * Gestisce la logica di registrazione di nuovi utenti 
 * e la generazione di token per il login.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, 
                       PasswordEncoder passwordEncoder, 
                       JwtService jwtService, 
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * REGISTRAZIONE: Crea un nuovo utente.
     * 1. Verifica se l'email esiste già.
     * 2. Cripta la password.
     * 3. Salva l'utente con ruolo USER di default.
     * 4. Ritorna il token per permettere l'autenticazione immediata.
     */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        // Controllo duplicati
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email già registrata nel sistema.");
        }

        // Creazione entità
        User user = new User();
        user.setEmail(request.email());
        // Hashing della password prima del salvataggio
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        // Salviamo il nuovo utente nel db
        user = userRepository.save(user);

        // Generazione Token
        String jwtToken = jwtService.generateToken(user);
        // Risposta inviata all'utente appena registrato
        return new AuthResponseDto(
                jwtToken,
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    /**
     * LOGIN: Valida le credenziali.
     * 1. L'AuthenticationManager interroga il CustomUserDetailsService.
     * 2. Se le credenziali sono errate, viene lanciata un'eccezione automatica.
     * 3. Se corrette, viene generato un nuovo token.
     */
    public AuthResponseDto login(LoginRequestDto request) {
        // Questa chiamata verifica email e password (con BCrypt)
        // Viene chiamato il "nostro" authenticationManager 
        // in SecurityConfig presente nell'application context
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // Se arriviamo qui, l'autenticazione ha avuto successo
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(); // Non può fallire dato che l'autenticazione è passata

        String jwtToken = jwtService.generateToken(user);

        return new AuthResponseDto(
                jwtToken,
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}