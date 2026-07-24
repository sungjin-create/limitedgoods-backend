package com.limitedgoods.limitedgoods.notification.infrastructure.mail;

public interface EmailSender {
    void send(String recipientEmail, String subject, String body);
}
