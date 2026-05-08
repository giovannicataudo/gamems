package it.gamems.game_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * ========================================================
 * INTERCEPTOR GLOBALE: GlobalExceptionHandler
 * ========================================================
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Errore Utente / Business Logic (es. Saldo insufficiente) -> 400 Bad Request
    @ExceptionHandler(GameOperationException.class)
    public ResponseEntity<ErrorResponseDto> handleGameOperationException(GameOperationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Business Rule Violation", ex.getMessage());
    }

    // 2. Errore di Rete / Dipendenza Offline (es. Wallet non risponde) -> 503 Service Unavailable
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponseDto> handleExternalServiceException(ExternalServiceException ex) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "External Service Down", ex.getMessage());
    }

    // 3. Payload JSON non valido -> 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Error", errorMessage);
    }

    /**
     * GESTORE: Errore di sintassi JSON o Body mancante -> 400 Bad Request
     * Scatta quando il client invia un JSON malformato (es. virgole mancanti, tipi errati).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleJsonSyntaxError(HttpMessageNotReadableException ex) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Malformed JSON Request",
                "Impossibile leggere la richiesta. Controlla la sintassi del JSON o assicurati di aver inviato il body corretto."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * GESTORE: Metodo HTTP non supportato -> 405 Method Not Allowed
     * Scatta quando il client chiama una rotta esistente ma con il verbo sbagliato (es. GET invece di POST).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String supportedMethods = ex.getSupportedHttpMethods() != null ? 
                                  ex.getSupportedHttpMethods().toString() : "Sconosciuto";
                                  
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Method Not Allowed",
                "Metodo HTTP non supportato per questa rotta. Metodi ammessi: " + supportedMethods
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // 4. Fallback Generico -> 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Si è verificato un errore imprevisto.");
    }

    // Metodo di utilità per evitare codice duplicato
    private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String error, String message) {
        ErrorResponseDto response = new ErrorResponseDto(LocalDateTime.now(), status.value(), error, message);
        return new ResponseEntity<>(response, status);
    }
}