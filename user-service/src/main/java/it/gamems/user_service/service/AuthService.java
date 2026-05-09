package it.gamems.user_service.service;

import it.gamems.user_service.dto.AuthResponseDto;
import it.gamems.user_service.dto.LoginRequestDto;
import it.gamems.user_service.dto.RegisterRequestDto;
import it.gamems.user_service.entity.User;
import it.gamems.user_service.enums.Role;
import it.gamems.user_service.exception.UserAlreadyExistsException;
import it.gamems.user_service.repository.UserRepository;
import it.gamems.user_service.security.JwtService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
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

    // Costanti di configurazione per la sicurezza
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

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
     */
    public AuthResponseDto login(LoginRequestDto request) {
        // 1. Cerchiamo l'utente PRIMA di autenticarlo per vedere se è attualmente bannato
        // Usiamo un'eccezione generica per non far capire a un hacker se l'email esiste o no
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenziali non valide"));

        // 2. CONTROLLO LOCKOUT: È bloccato?
        if (user.getLockoutEnd() != null) {
            if (LocalDateTime.now().isBefore(user.getLockoutEnd())) {
                // È ancora in punizione
                long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getLockoutEnd());
                throw new DisabledException("Account temporaneamente bloccato per sicurezza. Riprova tra " + minutesLeft + " minuti.");
            } else {
                // Il tempo è scaduto, sblocchiamo l'account in memoria (salveremo sul DB dopo)
                user.setLockoutEnd(null);
                user.setFailedLoginAttempts(0);
            }
        }

        // 3. TENTATIVO DI AUTENTICAZIONE
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException ex) {
            // L'AUTENTICAZIONE È FALLITA! (Password errata)
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                // Scatta il blocco
                user.setLockoutEnd(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
                userRepository.save(user);
                throw new BadCredentialsException("Troppi tentativi falliti. Account bloccato per " + LOCKOUT_DURATION_MINUTES + " minuti.");
            }

            // Salviamo il nuovo contatore e diamo errore
            userRepository.save(user);
            throw new BadCredentialsException("Credenziali non valide. Tentativi rimasti: " + (MAX_FAILED_ATTEMPTS - attempts));
        }

        // 4. L'AUTENTICAZIONE HA AVUTO SUCCESSO!
        // Se l'utente aveva sbagliato in precedenza ma ora ha indovinato, azzeriamo la sua fedina penale
        if (user.getFailedLoginAttempts() > 0 || user.getLockoutEnd() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockoutEnd(null);
            userRepository.save(user);
        }

        // 5. GENERAZIONE TOKEN (Il tuo codice originale)
        String jwtToken = jwtService.generateToken(user);

        return new AuthResponseDto(
                jwtToken,
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}