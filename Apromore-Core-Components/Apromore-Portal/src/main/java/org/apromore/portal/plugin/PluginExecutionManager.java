package org.apromore.portal.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class is to manage all running plugins (PluginExecution) in the portal.
 * 
 * @author Bruce Nguyen
 *
 */
public class PluginExecutionManager {
    private Map<String,PluginExecution> pluginExecutions = new HashMap<>(); // plugin instance ID => PortalPlugin
    
    public PluginExecutionManager() {
        
    }
    
    public PluginExecution getPluginExecution(String executionId) {
        return pluginExecutions.get(executionId);
    }
    
    public String registerPluginExecution(PluginExecution pluginExecution) {
        String executionId = UUID.randomUUID().toString();
        pluginExecutions.put(executionId, pluginExecution);
        return executionId;
    }
}
