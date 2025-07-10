package com.adminsite.core.services.impl;

import com.adminsite.core.services.EmailService;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Session;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.adminsite.core.workflows.UserUtil.ADMINSITE_USER_SERVICE;

@Slf4j
@Component(service = EmailService.class)
public class EmailServiceImpl implements EmailService {

    @Reference
    private MessageGatewayService messageGatewayService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private final Map<String, MailTemplate> mailTemplateCache = new ConcurrentHashMap<>();

    public void sendEmail(String templatePath, String subject, String recipientEmail, Map<String, String> parameters) throws EmailException {
        try (ResourceResolver resolver =
                     resourceResolverFactory.getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, ADMINSITE_USER_SERVICE))) {
            MailTemplate mailTemplate = getCachedMailTemplate(templatePath, resolver);
            HtmlEmail email = mailTemplate.getEmail(StrLookup.mapLookup(parameters), HtmlEmail.class);
            email.setSubject(subject);
            email.addTo(recipientEmail);
            MessageGateway<HtmlEmail> messageGateway = messageGatewayService.getGateway(HtmlEmail.class);
            messageGateway.send(email);
            log.info("Email sent successfully to {}", recipientEmail);
        } catch (LoginException | IOException | MessagingException | EmailException e) {
            log.error("Failed to send email due to exception: {}", e.getMessage(), e);
            throw new EmailException("Failed to send email to " + recipientEmail, e);
        }
    }

    private MailTemplate getCachedMailTemplate(String templatePath, ResourceResolver resolver) {
        MailTemplate cachedTemplate = mailTemplateCache.get(templatePath);
        if (cachedTemplate != null) {
            return cachedTemplate;
        }
        Session session = resolver.adaptTo(Session.class);
        MailTemplate mailTemplate = MailTemplate.create(templatePath, session);
        if (mailTemplate == null) {
            throw new IllegalArgumentException("Unable to create MailTemplate for path: " + templatePath);
        }
        mailTemplateCache.put(templatePath, mailTemplate);
        return mailTemplate;
    }
}