package com.adminsite.core.workflows;

import com.adobe.granite.asset.api.AssetException;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;

import static com.adminsite.core.workflows.MoveApprovedAssetWorkflowProcess.WORKFLOW_PROCESS_NAME;
import static com.adminsite.core.workflows.UserUtil.ADMINSITE_USER_SERVICE;
import static com.day.cq.commons.jcr.JcrConstants.JCR_CONTENT;

@Slf4j
@Component(service = WorkflowProcess.class, property = {"process.label=" + WORKFLOW_PROCESS_NAME})
public class MoveApprovedAssetWorkflowProcess implements WorkflowProcess {

    public static final String WORKFLOW_PROCESS_NAME = "Move Approved Asset";
    public static final String PATH = "PATH";
    public static final String SLASH = "/";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        try (ResourceResolver resolver =
                     resourceResolverFactory.getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, ADMINSITE_USER_SERVICE))) {
            AssetManager assetManager = resolver.adaptTo(AssetManager.class);
            Session session = resolver.adaptTo(Session.class);
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            String destinationPath = WorkflowHistoryUtil.getLatestMetadataValue(workItem, workflowSession, "pathToMove");
            String fileName = payloadPath.substring(payloadPath.lastIndexOf(SLASH) + 1);
            String destinationPathWithFile = destinationPath + SLASH + fileName;
            if (StringUtils.isBlank(payloadPath) || !assetManager.assetExists(payloadPath)) {
                throw new WorkflowException("Payload Path somehow empty");
            }
            if (StringUtils.isBlank(destinationPath)) {
                throw new WorkflowException("Destination Path is empty");
            }
            if (StringUtils.isNoneBlank(destinationPath) && payloadPath.equals(destinationPathWithFile)) {
                workItem.getWorkflowData().getMetaDataMap().put(PATH, payloadPath);
            } else if (assetManager.assetExists(destinationPathWithFile)) {
                throw new WorkflowException("Payload with this name \"" + fileName + "\" already exist");
            } else if (StringUtils.isNoneBlank(destinationPath) && StringUtils.isNoneBlank(payloadPath)) {
                addWorkflowProcessedProperty(resolver, payloadPath);
                assetManager.moveAsset(payloadPath, destinationPathWithFile);
                session.save();
                session.logout();
                workItem.getWorkflowData().getMetaDataMap().put(PATH, destinationPathWithFile);
            }
        } catch (LoginException | AssetException | RepositoryException exception) {
            log.error("Error during processing of " + WORKFLOW_PROCESS_NAME + ": {}", exception.getMessage());
        }
    }

    private void addWorkflowProcessedProperty(ResourceResolver resolver, String assetPath) {
        Resource assetResource = resolver.getResource(assetPath);
        if (assetResource != null) {
            Resource contentResource = assetResource.getChild(JCR_CONTENT);
            if (contentResource != null) {
                ModifiableValueMap properties = contentResource.adaptTo(ModifiableValueMap.class);
                if (properties != null) {
                    properties.put("workflowProcessed", true);
                    log.info("Added 'workflowProcessed' property to jcr:content for resource: {}", assetPath);
                } else {
                    log.warn("Could not adapt jcr:content to ModifiableValueMap for path: {}", assetPath);
                }
            } else {
                log.warn("jcr:content node not found for resource: {}", assetPath);
            }
        } else {
            log.warn("Asset resource is null for path: {}", assetPath);
        }
    }
}