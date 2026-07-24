package com.limitedgoods.limitedgoods.notification.sendor;

public interface EmailSender {
    void send(String recipientEmail, String subject, String body);
}
