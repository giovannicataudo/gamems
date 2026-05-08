package it.gamems.user_service.dto;

/**
 * ========================================================
 * DTO: AuthResponseDto (Output)
 * ========================================================
 * Restituito al client dopo un login o una registrazione di successo.
 * Contiene il token JWT da usare come "passaporto" per le chiamate future.
 * * Bearer è il tipo di Authorization che verrà usato nelle richieste Http
 *  vuol dire che per essere autorizzato basta essere "portatore" del token
 */
public record AuthResponseDto(
        
        String token,
        String type,
        Long userId,
        String email,
        String role
) {
    // Costruttore compatto per impostare "Bearer" di default.
    public AuthResponseDto(String token, Long userId, String email, String role) {
        this(token, "Bearer", userId, email, role);
    }
}