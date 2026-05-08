package it.gamems.wallet_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Tabella di appoggio per garantire l'idempotenza dei messaggi RabbitMQ.
 * Memorizza gli ID delle vincite già accreditate ai portafogli.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    // L'ID dell'evento (Corrisponderà al matchId del Game Service)
    @Id
    @Column(name = "event_id", updatable = false, nullable = false)
    private Long eventId;

    // Utile in futuro per uno scheduler che cancella gli eventi vecchi
    @Column(name = "processed_at", updatable = false, nullable = false)
    private LocalDateTime processedAt;

    // Costruttore vuoto per JPA
    public ProcessedEvent() {}

    public ProcessedEvent(Long eventId) {
        this.eventId = eventId;
        this.processedAt = LocalDateTime.now();
    }

    public Long getEventId() { return eventId; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}