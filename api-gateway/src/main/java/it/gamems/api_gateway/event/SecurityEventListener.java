package it.gamems.api_gateway.event;

import it.gamems.api_gateway.service.BlacklistService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventListener {
    private final BlacklistService blacklistService;

    public SecurityEventListener(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @RabbitListener(queues = "#{autoDeleteQueue.name}")
    public void handleSecurityEvent(SecurityEventDto event) {
        if ("BAN".equals(event.action())) {
            blacklistService.add(event.userId());
        } else if ("UNBAN".equals(event.action())) {
            blacklistService.remove(event.userId());
        }
    }
}