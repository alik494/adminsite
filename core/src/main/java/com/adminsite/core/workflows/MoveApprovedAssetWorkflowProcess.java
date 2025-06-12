package com.adminsite.core.workflows;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;

import static com.adminsite.core.workflows.MoveApprovedAssetWorkflowProcess.WORKFLOW_PROCESS_NAME;
import static com.adminsite.core.workflows.UserUtil.ADMINSITE_USER_SERVICE;

@Slf4j
@Component(service = WorkflowProcess.class, property = {"process.label=" + WORKFLOW_PROCESS_NAME})
public class MoveApprovedAssetWorkflowProcess implements WorkflowProcess {

    public static final String WORKFLOW_PROCESS_NAME = "Move Approved Asset";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) {
        try (ResourceResolver resolver =
                     resourceResolverFactory.getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, ADMINSITE_USER_SERVICE))) {
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            String destinationPath = WorkflowHistoryUtil.getLatestMetadataValue(workItem, workflowSession, "pathToMove");
            if (payloadPath != null || destinationPath != null) {
                Session session = resolver.adaptTo(Session.class);
                if (session != null) {
                    session.move(payloadPath, destinationPath);
                    session.save();
                    metaDataMap.put("PATH",destinationPath);
                }
            }
        } catch (RepositoryException | LoginException exception) {
            log.error("Error during processing of " + WORKFLOW_PROCESS_NAME + ": {}", exception.getMessage());
        }
    }
}