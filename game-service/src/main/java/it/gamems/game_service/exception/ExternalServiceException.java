package it.gamems.game_service.exception;

/**
 * Eccezioni colpa "nostra" (es. errore di rete, non si raggiunge wallet o è spento)
 */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }
}