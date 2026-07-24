package com.limitedgoods.limitedgoods.notification.infrastructure.mail;

import com.limitedgoods.limitedgoods.notification.exception.EmailInfrastructureException;
import com.limitedgoods.limitedgoods.notification.exception.NonRetryableEmailException;
import com.limitedgoods.limitedgoods.notification.exception.RetryableEmailException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

@Component
@ConditionalOnProperty(
        name = "app.mail.enabled",
        havingValue = "true"
)
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender, Environment environment) {
        this.mailSender = mailSender;
        this.from = environment.getRequiredProperty("app.mail.from");
    }

    @Override
    public void send(String recipientEmail,  String subject, String body) {
        validateRecipient(recipientEmail);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

        } catch (MailAuthenticationException exception) {
            throw new EmailInfrastructureException("SMTP authentication failed", exception);

        } catch (MailParseException exception) {
            throw new NonRetryableEmailException("Email message is invalid", exception);

        } catch (MailException exception) {
            throw new RetryableEmailException("SMTP delivery temporarily failed", exception);
        }
    }


    private void validateRecipient(String recipientEmail) {
        try {
            new InternetAddress(recipientEmail, true );
        } catch (AddressException exception) {
            throw new NonRetryableEmailException("Recipient email is invalid", exception);
        }
    }
}
