package it.gamems.user_service.event;

/**
 * ========================================================
 * EVENT DTO: SecurityEventDto
 * ========================================================
 * Mappa il messaggio JSON inviato sul FanoutExchange 
 * per notificare l'API Gateway di un ban/unban.
 */
public record SecurityEventDto(
        Long userId,
        String action // Conterrà "BAN" o "UNBAN"
) {
}