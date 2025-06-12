package com.adminsite.core.workflows;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.mailer.MessageGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.HtmlEmail;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;

import static com.adminsite.core.workflows.EmailNotificationWorkflowProcess.WORKFLOW_PROCESS_NAME;
import static com.adminsite.core.workflows.UserUtil.ADMINSITE_USER_SERVICE;

@Slf4j
@Component(service = WorkflowProcess.class, property = {"process.label=" + WORKFLOW_PROCESS_NAME})
public class EmailNotificationWorkflowProcess implements WorkflowProcess {

    public static final String WORKFLOW_PROCESS_NAME = "Send Email Notification";
    private static final String APPROVED = "Approved";
    private static final String REJECTED = "Rejected";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private MessageGatewayService messageGatewayService;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) {
        String assetPath = workItem.getWorkflowData().getPayload().toString();
        if (StringUtils.isBlank(assetPath)) {
            log.error("Asset path is blank, cannot send email notification.");
            return;
        }
        String uploaderEmail = getUploaderEmail(assetPath);
        if (StringUtils.isBlank(uploaderEmail)) {
            log.error("Uploader email could not be determined for asset at path: {}", assetPath);
            return;
        }
        String rejectionReason = WorkflowHistoryUtil.getLatestMetadataValue(workItem, workflowSession, "rejectReason");
        String approvalOption = StringUtils.isNoneBlank(rejectionReason) ? REJECTED : APPROVED;
        String finalAssetPath = metaDataMap.get("PATH", String.class);
        sendEmail(uploaderEmail, approvalOption, rejectionReason, finalAssetPath);
    }

    private String getUploaderEmail(String assetPath) {
        try (ResourceResolver resolver =
                     resourceResolverFactory.getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, ADMINSITE_USER_SERVICE))) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                log.error("Unable to adapt ResourceResolver to Session.");
                return null;
            }
            String createdBy = getCreatedBy(assetPath, session);
            if (StringUtils.isBlank(createdBy)) {
                log.error("Could not determine who created the asset at path: {}", assetPath);
                return null;
            }
            return UserUtil.getUserEmail(createdBy, session);
        } catch (LoginException e) {
            log.error("Error obtaining ResourceResolver for adminsite-user-service.", e);
        }
        return null;
    }

    private String getCreatedBy(String assetPath, Session session) {
        try {
            if (session.nodeExists(assetPath)) {
                Node node = session.getNode(assetPath);
                if (node.hasProperty("jcr:createdBy")) {
                    return node.getProperty("jcr:createdBy").getString();
                }
            } else {
                log.warn("Node does not exist at path: {}", assetPath);
            }
        } catch (RepositoryException e) {
            log.error("Error retrieving 'jcr:createdBy' for asset at path: {}", assetPath, e);
        }
        return null;
    }

    private void sendEmail(String email, String status, String reason, String assetPath) {
        try {
            HtmlEmail htmlEmail = new HtmlEmail();
            htmlEmail.setSubject("Asset " + status);
            htmlEmail.addTo(email);
            String message = status.equals(APPROVED)
                    ? "Your asset has been approved. It is now located at: " + assetPath
                    : "Your asset has been rejected. Reason: " + reason;
            htmlEmail.setMsg(message);
            messageGatewayService.getGateway(HtmlEmail.class).send(htmlEmail);
            log.info("Email notification sent to {} with status {}.", email, status);
        } catch (Exception e) {
            log.error("Unable to send email to {}. Error: {}", email, e.getMessage());
        }
    }
}