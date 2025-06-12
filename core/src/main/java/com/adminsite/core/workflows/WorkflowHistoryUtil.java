package com.adminsite.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.HistoryItem;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@UtilityClass
public class WorkflowHistoryUtil {

    public static String getLatestMetadataValue(WorkItem workItem, WorkflowSession workflowSession, String key) {

        List<HistoryItem> historyItems = null;
        try {
            historyItems = workflowSession.getHistory(workItem.getWorkflow());
            if (historyItems != null && !historyItems.isEmpty()) {
                HistoryItem latestHistoryItem = historyItems.get(historyItems.size() - 1);
                if (latestHistoryItem.getWorkItem() != null) {
                    MetaDataMap metadataMap = latestHistoryItem.getWorkItem().getMetaDataMap();
                    if (metadataMap != null) {
                        return metadataMap.get(key, String.class);
                    }
                }
            }
        } catch (WorkflowException e) {
            log.error("Error", e);
        }
        return null;
    }
}