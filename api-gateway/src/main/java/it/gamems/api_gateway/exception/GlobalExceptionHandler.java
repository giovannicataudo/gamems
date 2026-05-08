package it.gamems.api_gateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.ConnectException;
import java.time.LocalDateTime;

/**
 * ========================================================
 * INTERCEPTOR: GlobalExceptionHandler (API Gateway)
 * ========================================================
 * Gestisce gli errori a livello di infrastruttura di rete.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * GESTORE 1: Microservizio a valle non raggiungibile (Down / Crash)
     * Se il Gateway prova a inoltrare la richiesta ma il servizio rifiuta la connessione.
     */
    @ExceptionHandler({ConnectException.class, ResourceAccessException.class})
    public ResponseEntity<ErrorResponseDto> handleConnectionError(Exception ex) {
        log.error("Connessione rifiutata verso un microservizio a valle: {}", ex.getMessage());
        
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE, 
                "Service Unavailable", 
                "Il servizio richiesto è temporaneamente offline. Riprova tra poco."
        );
    }

    /**
     * GESTORE 2: Rotta inesistente (404 Not Found)
     * Se il frontend chiama un'API che non è mappata in application.yml.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(NoResourceFoundException ex) {
        return buildResponse(
                HttpStatus.NOT_FOUND, 
                "Not Found", 
                "La risorsa richiesta non esiste o l'URL è errato."
        );
    }

    /**
     * GESTORE 3: Fallback Generico
     * Cattura qualsiasi errore imprevisto del Gateway stesso.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        log.error("Errore critico interno all'API Gateway", ex);
        
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "Gateway Error", 
                "Errore interno del server di routing."
        );
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String error, String message) {
        ErrorResponseDto response = new ErrorResponseDto(LocalDateTime.now(), status.value(), error, message);
        return new ResponseEntity<>(response, status);
    }
}