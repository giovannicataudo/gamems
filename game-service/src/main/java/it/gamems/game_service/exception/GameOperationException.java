package it.gamems.game_service.exception;

/**
 * Eccezione di Business
 * Viene usata per gli errori dell'utente (es. Wallet rifiuta per saldo insufficiente)
 */
public class GameOperationException extends RuntimeException {
    public GameOperationException(String message) {
        super(message);
    }
}