package com.adminsite.core.services;

import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.HtmlEmail;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Session;
import java.util.Collections;
import java.util.Map;

import static com.adminsite.core.workflows.UserUtil.ADMINSITE_USER_SERVICE;

@Slf4j
@Component(service = EmailService.class)
public class EmailService {

    @Reference
    private MessageGatewayService messageGatewayService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    public void sendEmail(String templatePath, String subject, String recipientEmail, Map<String, String> parameters) {
        try (ResourceResolver resolver =
                     resourceResolverFactory.getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, ADMINSITE_USER_SERVICE))) {
            Session session = resolver.adaptTo(Session.class);
            MailTemplate mailTemplate = MailTemplate.create(templatePath, session);
            HtmlEmail email = mailTemplate.getEmail(StrLookup.mapLookup(parameters), HtmlEmail.class);
            email.setSubject(subject);
            email.addTo(recipientEmail);
            MessageGateway<HtmlEmail> messageGateway = messageGatewayService.getGateway(HtmlEmail.class);
            messageGateway.send(email);
            log.info("Email sent successfully to {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send email due to exception: {}", e.getMessage(), e);
        }
    }
}