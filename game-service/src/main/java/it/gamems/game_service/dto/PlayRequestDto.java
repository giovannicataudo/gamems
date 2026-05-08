package it.gamems.game_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * ========================================================
 * DTO: PlayRequestDto (Dati in entrata)
 * ========================================================
 * Mappa il payload JSON inviato dall'utente quando clicca su "Gioca".
 * * * VALIDAZIONE RESTRITTIVA:
 * Le regole di business vengono applicate direttamente all'ingresso.
 * Se un utente malintenzionato prova a inviare una stringa diversa 
 * da TESTA o CROCE, o un importo negativo, Spring blocca la richiesta 
 * automaticamente restituendo un 400 Bad Request.
 */
public record PlayRequestDto(

        @NotNull(message = "L'importo della scommessa non può essere nullo")
        @DecimalMin(value = "1.00", message = "La scommessa minima è di 1.00")
        BigDecimal betAmount,

        @NotBlank(message = "La scelta non può essere vuota")
        // La regex obbliga l'input a corrispondere esattamente a una delle due opzioni
        @Pattern(regexp = "^(TESTA|CROCE)$", message = "Scelta non valida. Sono ammessi solo i valori 'TESTA' o 'CROCE'")
        String choice
) {
}