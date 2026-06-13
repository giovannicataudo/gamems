package it.gamems.user_service.entity;

import it.gamems.user_service.enums.Role;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * ========================================================
 * ENTITY: User (Identità e Sicurezza)
 * ========================================================
 * Rappresenta l'utente nel sistema. Implementa UserDetails
 * per usare il framework di sicurezza Spring Security.
 * Serve per far capire a Spring che questa è la classe
 * che deve usare per autorizzare gli utenti
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Password memorizzata esclusivamente come HASH (BCrypt).
     * Verrà hashata nel service
     */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean enabled = true;

    // Campi per la prevenzione del brute force
    @Column(name = "failed_login_attempts", columnDefinition = "integer default 0")
    private Integer failedLoginAttempts = 0;

    @Column(name = "lockout_end")
    private LocalDateTime lockoutEnd;

    // --- Campi per MFA e Verifica Email ---
    @Column(name = "is_email_verified", nullable = false, columnDefinition = "boolean default false")
    private boolean isEmailVerified = false;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "mfa_enabled", nullable = false, columnDefinition = "boolean default false")
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    // --- Metodi per scrivere in automatico le datetime ---

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.enabled = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Implementazione UserDetails ---

    // Dice a Spring quale ruolo ha questo utente
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Converte l'enum Role in una GrantedAuthority comprensibile a Spring
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    // Usiamo l'email come username 
    @Override
    public String getUsername() {
        return email;
    }

    // Restituisce la password hashata presente nel db
    // In modo da confrontarla con quella inserita dell'utente
    @Override
    public String getPassword() {
        return password;
    }

    // Booleani per bloccare l'account per varie ragioni

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }

    // --- Getter e Setter Standard ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean getEnable() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts == null ? 0 : failedLoginAttempts;}
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    
    public LocalDateTime getLockoutEnd() { return lockoutEnd; }
    public void setLockoutEnd(LocalDateTime lockoutEnd) { this.lockoutEnd = lockoutEnd; }

    public boolean isEmailVerified() { return isEmailVerified; }
    public void setEmailVerified(boolean emailVerified) { isEmailVerified = emailVerified; }

    public String getEmailVerificationToken() { return emailVerificationToken; }
    public void setEmailVerificationToken(String emailVerificationToken) { this.emailVerificationToken = emailVerificationToken; }

    public boolean isMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }

    public String getMfaSecret() { return mfaSecret; }
    public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }
}