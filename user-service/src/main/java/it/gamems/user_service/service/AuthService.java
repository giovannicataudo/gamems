package it.gamems.user_service.service;

import it.gamems.user_service.dto.*;
import it.gamems.user_service.entity.User;
import it.gamems.user_service.enums.Role;
import it.gamems.user_service.exception.UserAlreadyExistsException;
import it.gamems.user_service.repository.UserRepository;
import it.gamems.user_service.security.JwtService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final TotpService totpService;

    public AuthService(UserRepository userRepository, 
                       PasswordEncoder passwordEncoder, 
                       JwtService jwtService, 
                       AuthenticationManager authenticationManager,
                       EmailService emailService,
                       TotpService totpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.totpService = totpService;
    }

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email già registrata nel sistema.");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        
        user.setEmailVerified(false);
        user.setMfaEnabled(false);
        String token = UUID.randomUUID().toString();
        user.setEmailVerificationToken(token);
        
        user = userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), token);

        return new AuthResponseDto(
                "",
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    @Transactional
    public MfaSetupResponseDto verifyEmailAndSetupMfa(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadCredentialsException("Token di verifica non valido o scaduto"));

        if (user.isEmailVerified()) {
            throw new BadCredentialsException("Email già verificata");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        
        String mfaSecret = totpService.generateSecret();
        user.setMfaSecret(mfaSecret);
        user.setMfaEnabled(true);
        userRepository.save(user);

        String qrCodeUri = totpService.getQrCodeImageUri(mfaSecret, user.getEmail());
        
        return new MfaSetupResponseDto(qrCodeUri, "Email verificata con successo! Configura l'app Authenticator scansionando il QR Code.");
    }

    public Object login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenziali non valide"));

        if (!user.isEnabled()) {
            throw new DisabledException("Sei stato bannato. Contatta l'assistenza per maggiori informazioni.");
        }

        if (!user.isEmailVerified()) {
            throw new DisabledException("Devi prima verificare la tua email. Controlla la posta in arrivo.");
        }

        if (user.getLockoutEnd() != null) {
            if (LocalDateTime.now().isBefore(user.getLockoutEnd())) {
                long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getLockoutEnd());
                throw new DisabledException("Account temporaneamente bloccato per sicurezza. Riprova tra " + minutesLeft + " minuti.");
            } else {
                user.setLockoutEnd(null);
                user.setFailedLoginAttempts(0);
            }
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException ex) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockoutEnd(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
                userRepository.save(user);
                throw new DisabledException("Troppi tentativi falliti. Account bloccato per " + LOCKOUT_DURATION_MINUTES + " minuti.");
            }
            userRepository.save(user);
            throw new BadCredentialsException("Credenziali non valide. Tentativi rimasti: " + (MAX_FAILED_ATTEMPTS - attempts));
        }

        if (user.getFailedLoginAttempts() > 0 || user.getLockoutEnd() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockoutEnd(null);
            userRepository.save(user);
        }

        if (user.isMfaEnabled()) {
            String tempJwt = jwtService.generateToken(user);
            return new MfaRequiredResponseDto(tempJwt, "MFA_REQUIRED");
        }

        String jwtToken = jwtService.generateToken(user);
        return new AuthResponseDto(jwtToken, user.getId(), user.getEmail(), user.getRole().name());
    }

    public AuthResponseDto verifyMfaLogin(MfaLoginRequestDto request) {
        String email = jwtService.extractEmail(request.tempToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Token non valido"));

        if (!totpService.verifyCode(user.getMfaSecret(), request.code())) {
            throw new BadCredentialsException("Codice MFA non valido");
        }

        String finalJwt = jwtService.generateToken(user);
        return new AuthResponseDto(finalJwt, user.getId(), user.getEmail(), user.getRole().name());
    }
}