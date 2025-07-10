package com.adminsite.core.services;

import org.apache.commons.mail.EmailException;

import java.util.Map;

public interface EmailService {

    void sendEmail(String templatePath, String subject, String recipientEmail, Map<String, String> parameters) throws EmailException;

}