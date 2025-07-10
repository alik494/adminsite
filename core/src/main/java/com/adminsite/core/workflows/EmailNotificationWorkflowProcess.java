package com.adminsite.core.workflows;

import com.adminsite.core.services.CustomExternalizer;
import com.adminsite.core.services.EmailService;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.adminsite.core.workflows.EmailNotificationWorkflowProcess.WORKFLOW_PROCESS_NAME;
import static com.adminsite.core.workflows.UserUtil.ADMINSITE_USER_SERVICE;

@Slf4j
@Component(service = WorkflowProcess.class, property = {"process.label=" + WORKFLOW_PROCESS_NAME})
@Designate(ocd = EmailNotificationWorkflowProcess.EmailNotificationServiceConfig.class)
public class EmailNotificationWorkflowProcess implements WorkflowProcess {

    public static final String WORKFLOW_PROCESS_NAME = "Send Email Notification";
    public static final String PROPERTY_ASSET_NAME = "assetName";
    public static final String PROPERTY_ASSET_PATH = "assetPath";
    public static final String PROPERTY_ASSET_DETAILS = "/assetdetails.html";
    public static final String PROPERTY_ASSET_PATH_EXTERNAL = "assetPathExternal";
    public static final String PROPERTY_STATUS = "status";
    public static final String PROPERTY_REJECT_REASON = "rejectReason";
    public static final String PATH = "PATH";
    private static final String APPROVED = "Approved";
    private static final String REJECTED = "Rejected";
    public static final String SLASH = "/";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private EmailService emailService;

    @Reference
    private CustomExternalizer externalizer;

    private String approvalEmailTemplatePath;
    private String rejectionEmailTemplatePath;

    @ObjectClassDefinition(name = "Email Notification Service Configuration")
    public @interface EmailNotificationServiceConfig {

        @AttributeDefinition(name = "Approval Email Template Path",
                description = "The path to the email template used when an asset is approved.")
        String approvalEmailTemplatePath() default "/conf/global/settings/workflow/notification/email/tasks/dropboxWFCompleted/approve.txt";

        @AttributeDefinition(name = "Rejection Email Template Path",
                description = "The path to the email template used when an asset is rejected.")
        String rejectionEmailTemplatePath() default "/conf/global/settings/workflow/notification/email/tasks/dropboxWFCompleted/reject.txt";

    }

    @Activate
    @Modified
    protected void activate(EmailNotificationServiceConfig config) {
        this.approvalEmailTemplatePath = config.approvalEmailTemplatePath();
        this.rejectionEmailTemplatePath = config.rejectionEmailTemplatePath();
    }

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        String rejectionReason = WorkflowHistoryUtil.getLatestMetadataValue(workItem, workflowSession, PROPERTY_REJECT_REASON);
        String assetPath = getAssetPath(workItem, rejectionReason);
        String uploaderEmail = getUploaderEmail(assetPath);
        Map<String, String> emailData = buildEmailData(rejectionReason, assetPath);

        String subject = "Asset " + emailData.get(PROPERTY_STATUS);
        String emailTemplatePath = determineEmailTemplatePath(emailData);

        try {
            emailService.sendEmail(emailTemplatePath, subject, uploaderEmail, emailData);
            log.info("Email notification sent to {} for asset at {}", uploaderEmail, assetPath);
        } catch (EmailException e) {
            log.error("Failed to send email notification to {} for asset {}: {}", uploaderEmail, assetPath, e.getMessage(), e);
            throw new WorkflowException("Error occurred during email notification workflow", e);
        }
    }

    private String getAssetPath(WorkItem workItem, String rejectionReason) throws WorkflowException {
        String assetPath;
        if (StringUtils.isNotBlank(rejectionReason)) {
            assetPath = workItem.getWorkflowData().getPayload().toString();
        } else {
            assetPath = workItem.getWorkflowData().getMetaDataMap().get(PATH, String.class);
        }
        if (StringUtils.isBlank(assetPath)) {
            throw new WorkflowException("Asset path is blank, cannot send email notification.");
        }
        return assetPath;
    }

    private String getUploaderEmail(String assetPath) throws WorkflowException {
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, ADMINSITE_USER_SERVICE))) {
            String uploaderEmail = UserUtil.getUploaderEmail(assetPath, resolver);
            if (StringUtils.isBlank(uploaderEmail)) {
                throw new WorkflowException("Uploader email could not be determined for asset at path: " + assetPath);
            }
            return uploaderEmail;
        } catch (LoginException | RepositoryException e) {
            log.error("Error obtaining ResourceResolver for subservice user.", e);
            throw new WorkflowException("Uploader email could not be determined", e);
        }
    }

    private Map<String, String> buildEmailData(String rejectionReason, String assetPath) {
        Map<String, String> emailData = new HashMap<>();

        String status = StringUtils.isNotBlank(rejectionReason) ? REJECTED : APPROVED;
        emailData.put(PROPERTY_ASSET_NAME, assetPath.substring(assetPath.lastIndexOf(SLASH) + 1));
        emailData.put(PROPERTY_STATUS, status);

        if (StringUtils.isNotBlank(rejectionReason)) {
            emailData.put(PROPERTY_REJECT_REASON, rejectionReason);
        } else {
            emailData.put(PROPERTY_ASSET_PATH, assetPath);
            emailData.put(PROPERTY_ASSET_PATH_EXTERNAL, externalizer.externalize(PROPERTY_ASSET_DETAILS + assetPath));
        }
        return emailData;
    }

    private String determineEmailTemplatePath(Map<String, String> emailData) {
        return APPROVED.equals(emailData.get(PROPERTY_STATUS))
                ? approvalEmailTemplatePath
                : rejectionEmailTemplatePath;
    }
}