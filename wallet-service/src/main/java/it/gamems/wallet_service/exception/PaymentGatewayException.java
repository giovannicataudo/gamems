package it.gamems.wallet_service.exception;

/**
 * Eccezione specifica per i fallimenti del Gateway di Pagamento esterno.
 * Viene mappata tipicamente come 503 Service Unavailable o 502 Bad Gateway.
 */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) {
        super(message);
    }
}