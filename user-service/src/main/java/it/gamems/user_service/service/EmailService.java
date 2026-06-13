package it.gamems.user_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String verificationLink = frontendUrl + "/verify-email?token=" + token;
        
        String htmlContent = "<div style='font-family: Arial, sans-serif;'>"
                + "<h2>Benvenuto in GameMS!</h2>"
                + "<p>Grazie per esserti registrato. Per completare la tua iscrizione e configurare l'accesso sicuro (MFA), clicca sul link sottostante per verificare il tuo indirizzo email:</p>"
                + "<a href='" + verificationLink + "' style='display:inline-block; padding:10px 20px; background-color:#4CAF50; color:white; text-decoration:none; border-radius:5px;'>Verifica la tua Email</a>"
                + "<p>Oppure copia e incolla questo link nel browser:</p>"
                + "<p><a href='" + verificationLink + "'>" + verificationLink + "</a></p>"
                + "<p>Se non hai richiesto tu la registrazione, puoi ignorare questa email.</p>"
                + "</div>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@gamems.local");
            helper.setTo(toEmail);
            helper.setSubject("Verifica il tuo Account GameMS");
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Errore nell'invio della mail a " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Impossibile inviare la mail di verifica");
        }
    }
}
