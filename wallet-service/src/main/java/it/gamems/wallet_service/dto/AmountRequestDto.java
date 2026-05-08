package it.gamems.wallet_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * ========================================================
 * DTO: AmountRequestDto (Dati in entrata)
 * ========================================================
 * Questo Record mappa il JSON (payload) che il frontend invia 
 * quando l'utente clicca su "Ricarica Conto" o "Preleva".
 * * * VALIDAZIONE:
 * Sfruttiamo 'spring-boot-starter-validation'. Le annotazioni 
 * bloccheranno le richieste errate ancor prima che arrivino al Service,
 * risparmiando cicli di CPU ed elaborazioni inutili.
 */
public record AmountRequestDto(
        
        // Impedisce l'invio di JSON senza il campo "amount"
        @NotNull(message = "L'importo non può essere nullo")
        
        // Regola di business: la ricarica o prelievo minimo è di 1 Euro.
        // Previene anche l'inserimento di numeri negativi (es. tentativi di truffa/hack).
        @DecimalMin(value = "1.00", message = "L'importo minimo deve essere 1.00")
        BigDecimal amount
) {
}
