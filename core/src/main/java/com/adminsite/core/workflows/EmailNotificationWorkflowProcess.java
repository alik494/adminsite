package com.adminsite.core.workflows;

import com.adminsite.core.services.EmailService;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.adminsite.core.workflows.EmailNotificationWorkflowProcess.WORKFLOW_PROCESS_NAME;
import static com.adminsite.core.workflows.UserUtil.ADMINSITE_USER_SERVICE;

@Slf4j
@Component(service = WorkflowProcess.class, property = {"process.label=" + WORKFLOW_PROCESS_NAME})
public class EmailNotificationWorkflowProcess implements WorkflowProcess {

    public static final String WORKFLOW_PROCESS_NAME = "Send Email Notification";
    public static final String PATH = "PATH";
    private static final String APPROVED = "Approved";
    private static final String REJECTED = "Rejected";
    public static final String SLASH = "/";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private EmailService emailService;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {

        String rejectionReason = WorkflowHistoryUtil.getLatestMetadataValue(workItem, workflowSession, "rejectReason");
        String assetPath;
        if (StringUtils.isNotBlank(rejectionReason)) {
            assetPath = workItem.getWorkflowData().getPayload().toString();
        } else {
            assetPath = workItem.getWorkflowData().getMetaDataMap().get(PATH, String.class);
        }
        if (StringUtils.isBlank(assetPath)) {
            throw new WorkflowException("Asset path is blank, cannot send email notification.");
        }
        String uploaderEmail = null;
        try (ResourceResolver resolver =
                     resourceResolverFactory.getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, ADMINSITE_USER_SERVICE))) {
            uploaderEmail = UserUtil.getUploaderEmail(assetPath, resolver);
        } catch (LoginException | RepositoryException e) {
            log.error("Error obtaining ResourceResolver for adminsite-user-service.", e);
            throw new WorkflowException("Uploader email could not be determined for asset ", e);
        }
        if (StringUtils.isBlank(uploaderEmail)) {
            throw new WorkflowException("Uploader email could not be determined for asset at path:" + assetPath);
        }
        String approvalOption = StringUtils.isNotBlank(rejectionReason) ? REJECTED : APPROVED;
        String subject = "Asset " + approvalOption;
        String emailTemplatePath;
        Map<String, String> emailData = new HashMap<>();
        emailData.put("assetName", assetPath.substring(assetPath.lastIndexOf(SLASH) + 1));
        emailData.put("status", approvalOption);
        if (StringUtils.isNotBlank(rejectionReason)) {
            emailData.put("rejectionReason", rejectionReason);
            emailTemplatePath = "/conf/global/settings/workflow/notification/email/tasks/dropboxWFCompleted/reject.txt";
        } else {
            emailData.put("assetPath", assetPath);
            emailTemplatePath = "/conf/global/settings/workflow/notification/email/tasks/dropboxWFCompleted/approve.txt";
        }
        emailService.sendEmail(emailTemplatePath, subject, uploaderEmail, emailData);
    }
}