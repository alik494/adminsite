package com.adminsite.core.workflows;

import com.adobe.granite.asset.api.AssetException;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Collections;

import static com.adminsite.core.workflows.DropboxWFCheckWorkflowProcess.WORKFLOW_PROCESS_NAME;
import static com.adminsite.core.workflows.UserUtil.ADMINSITE_USER_SERVICE;
import static com.day.cq.commons.jcr.JcrConstants.JCR_CONTENT;

@Slf4j
@Component(service = WorkflowProcess.class, property = {"process.label=" + WORKFLOW_PROCESS_NAME})
public class DropboxWFCheckWorkflowProcess implements WorkflowProcess {

    public static final String WORKFLOW_PROCESS_NAME = "Check Dropbox WF process";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        try (ResourceResolver resolver =
                     resourceResolverFactory.getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, ADMINSITE_USER_SERVICE))) {
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            if (isWorkflowAlreadyProcessed(resolver, payloadPath)) {
                log.info("Workflow already processed for asset: {}", payloadPath);
                Workflow workflow = workflowSession.getWorkflow(workItem.getWorkflow().getId());
                workflowSession.terminateWorkflow(workflow);
            }
        } catch (LoginException | AssetException exception) {
            log.error("Error during processing of " + WORKFLOW_PROCESS_NAME + ": {}", exception.getMessage());
        }
    }

    private boolean isWorkflowAlreadyProcessed(ResourceResolver resolver, String assetPath) {
        Resource assetResource = resolver.getResource(assetPath);
        if (assetResource != null) {
            Resource contentResource = assetResource.getChild(JCR_CONTENT);
            if (contentResource != null) {
                ValueMap properties = contentResource.getValueMap();
                boolean isProcessed = properties.get("workflowProcessed", false);
                log.info("'workflowProcessed' property for resource {} is: {}", assetPath, isProcessed);
                return isProcessed;
            } else {
                log.warn("jcr:content node not found for resource: {}", assetPath);
            }
        } else {
            log.warn("Asset resource is null for path: {}", assetPath);
        }
        return false;
    }

}