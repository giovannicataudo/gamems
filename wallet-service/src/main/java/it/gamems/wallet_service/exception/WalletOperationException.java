package it.gamems.wallet_service.exception;

/**
 * ========================================================
 * ECCEZIONE CUSTOM: WalletOperationException
 * ========================================================
 * Estendiamo RuntimeException per non obbligare i metodi 
 * a dichiarare "throws" (Unchecked Exception).
 * Verrà lanciata dal Service quando una regola di dominio viene violata.
 */
public class WalletOperationException extends RuntimeException {
    
    public WalletOperationException(String message) {
        super(message);
    }
}